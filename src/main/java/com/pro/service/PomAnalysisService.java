package com.pro.service;

import com.pro.model.ModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PomAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(PomAnalysisService.class);

    /**
     * Scans the project directory for pom.xml files and extracts module information
     */
    public List<ModuleInfo> scanPomFiles(String projectPath) {
        List<ModuleInfo> modules = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(projectPath))) {
            paths.filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !path.toString().contains("target")) // Exclude target directories
                    .forEach(path -> {
                        ModuleInfo module = parsePomFile(path.toFile());
                        if (module != null) {
                            modules.add(module);
                        }
                    });
        } catch (IOException e) {
            logger.error("Error scanning POM files: {}", e.getMessage());
        }
        return modules;
    }

    private ModuleInfo parsePomFile(File pomFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);
            doc.getDocumentElement().normalize();

            Element projectElement = doc.getDocumentElement();
            String groupId = getTagValue("groupId", projectElement);
            String artifactId = getTagValue("artifactId", projectElement);
            String version = getTagValue("version", projectElement);
            String packaging = getTagValue("packaging", projectElement);

            // Handle parent if groupId/version missing
            if (groupId == null) {
                NodeList parentList = projectElement.getElementsByTagName("parent");
                if (parentList.getLength() > 0) {
                    Element parent = (Element) parentList.item(0);
                    groupId = getTagValue("groupId", parent);
                    if (version == null) {
                        version = getTagValue("version", parent);
                    }
                }
            }

            ModuleInfo module = new ModuleInfo();
            module.setGroupId(groupId);
            module.setArtifactId(artifactId);
            module.setVersion(version);
            module.setPackaging(packaging != null ? packaging : "jar");
            module.setPath(pomFile.getAbsolutePath());

            // Extract dependencies
            List<String> dependencies = new ArrayList<>();
            NodeList dependencyList = projectElement.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyList.getLength(); i++) {
                Node node = dependencyList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String depArtifactId = getTagValue("artifactId", element);
                    if (depArtifactId != null) {
                        dependencies.add(depArtifactId);
                    }
                }
            }
            module.setDependencies(dependencies);

            return module;
        } catch (Exception e) {
            logger.error("Error parsing POM file {}: {}", pomFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            // Ensure we get the direct child, not a nested one (simple heuristic)
            // For a robust solution, we'd need to check parent, but for POMs usually the
            // first one at top level or under parent is what we want.
            // However, getElementsByTagName searches recursively.
            // Let's try to find the direct child.
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getParentNode() == element) {
                    return node.getTextContent();
                }
            }
            // Fallback for parent tag which is direct child of project
            if (element.getTagName().equals("project") && (tag.equals("groupId") || tag.equals("version"))) {
                // If not found as direct child, it might be inherited, handled in caller.
                return null;
            }
            // For dependencies, we are passing the dependency element, so direct child
            // check is good.
            if (nodeList.item(0).getParentNode() == element) {
                return nodeList.item(0).getTextContent();
            }
        }
        return null;
    }
}
