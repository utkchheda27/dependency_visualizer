package com.pro.apigraph.model;

import com.pro.apigraph.parser.JavaFileParser;
import com.pro.apigraph.parser.YamlConfigParser;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class RepoScanner {

    private final Path root;
    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", ".idea", "target", "build", "out", "node_modules", "test", "src/test");

    public RepoScanner(Path root) {
        this.root = root;
    }

    public List<Dependency> scan() throws IOException {
        List<Dependency> list = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (IGNORED_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isSupported(file)) {
                    processFile(file, list);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return dedupe(list);
    }

    private boolean isSupported(Path p) {
        String f = p.toString().toLowerCase();
        return f.endsWith(".java") || f.endsWith(".json") || f.endsWith(".yaml") || f.endsWith(".yml")
                || f.endsWith(".properties");
    }

    private void processFile(Path file, List<Dependency> list) {
        try {
            String serviceName = determineServiceName(file);
            if (serviceName == null)
                return;

            String fileName = file.getFileName().toString().toLowerCase();
            System.out.println("Processing: " + file + " (service: " + serviceName + ")");

            if (fileName.endsWith(".java")) {
                list.addAll(JavaFileParser.parse(file, serviceName));
            } else if (fileName.endsWith(".json") || fileName.endsWith(".yaml") || fileName.endsWith(".yml")
                    || fileName.endsWith(".properties")) {
                list.addAll(YamlConfigParser.parse(file, serviceName));
            }
        } catch (Exception e) {
            System.err.println("Error processing file " + file + ": " + e.getMessage());
        }
    }

    private String determineServiceName(Path file) {
        // Heuristic: Use the name of the directory containing 'pom.xml' or
        // 'build.gradle'
        // closest to the file, or just the parent directory name if not found.
        Path current = file.getParent();
        while (current != null && !current.equals(root)) {
            if (Files.exists(current.resolve("pom.xml")) || Files.exists(current.resolve("build.gradle"))) {
                return current.getFileName().toString();
            }
            current = current.getParent();
        }
        // Fallback to direct parent if inside root
        if (file.getParent() != null) {
            return file.getParent().getFileName().toString();
        }
        return "unknown";
    }

    private List<Dependency> dedupe(List<Dependency> list) {
        Set<String> set = new HashSet<>();
        List<Dependency> out = new ArrayList<>();

        for (Dependency d : list) {
            // Filter self-loops and empty targets
            if (d.getSource().equals(d.getTarget()) || d.getTarget().isEmpty())
                continue;

            // Filter common noisy targets
            if (isNoisyTarget(d.getTarget()))
                continue;

            String key = d.getSource() + "|" + d.getTarget() + "|" + d.getLabel();
            if (set.add(key))
                out.add(d);
        }
        return out;
    }

    private boolean isNoisyTarget(String target) {
        return target.equalsIgnoreCase("localhost") ||
                target.equalsIgnoreCase("127.0.0.1") ||
                target.startsWith("java.") ||
                target.startsWith("org.springframework") ||
                target.equals("config-dependent") ||
                target.equals("unknown");
    }
}
