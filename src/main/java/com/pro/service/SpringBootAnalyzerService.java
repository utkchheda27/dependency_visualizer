package com.pro.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pro.model.ApiEndpoint;
import com.pro.model.ComponentInfo;
import com.pro.model.ProjectAnalysis;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
public class SpringBootAnalyzerService {

    private JavaParser javaParser;
    private DependencyAnalysisService dependencyAnalysisService;

    public SpringBootAnalyzerService(DependencyAnalysisService dependencyAnalysisService) {
        this.javaParser = new JavaParser();
        this.dependencyAnalysisService = dependencyAnalysisService;
    }

    /**
     * Analyzes a Spring Boot project and returns comprehensive analysis
     */
    public ProjectAnalysis analyzeProject(String projectPath) {
        ProjectAnalysis analysis = new ProjectAnalysis();
        analysis.setProjectPath(projectPath);
        analysis.setProjectName(extractProjectName(projectPath));

        try {
            // Find all Java files
            List<File> javaFiles = findJavaFiles(projectPath);

            // Initialize lists
            List<ApiEndpoint> endpoints = new ArrayList<>();
            List<ComponentInfo> controllers = new ArrayList<>();
            List<ComponentInfo> services = new ArrayList<>();
            List<ComponentInfo> repositories = new ArrayList<>();
            List<ComponentInfo> models = new ArrayList<>();
            List<ComponentInfo> configurations = new ArrayList<>();
            Map<String, List<String>> dependencyGraph = new HashMap<>();

            // Analyze each Java file
            for (File javaFile : javaFiles) {
                analyzeJavaFile(javaFile, analysis, endpoints, controllers, services, repositories, models,
                        configurations, dependencyGraph);
            }

            // Set results
            analysis.setApiEndpoints(endpoints);
            analysis.setControllers(controllers);
            analysis.setServices(services);
            analysis.setRepositories(repositories);
            analysis.setModels(models);
            analysis.setConfigurations(configurations);
            analysis.setDependencyGraph(dependencyGraph);
            analysis.setAnalysisTimestamp(System.currentTimeMillis());

            // Build package structure
            analysis.setPackageStructure(buildPackageStructure(javaFiles));

            // Enhance dependency analysis
            dependencyAnalysisService.enhanceDependencyAnalysis(analysis);

        } catch (IOException e) {
            throw new RuntimeException("Error analyzing project: " + e.getMessage(), e);
        }

        return analysis;
    }

    /**
     * Finds all Java files in the project
     */
    private List<File> findJavaFiles(String projectPath) throws IOException {
        List<File> javaFiles = new ArrayList<>();
        Path startPath = Paths.get(projectPath);

        try (Stream<Path> paths = Files.walk(startPath)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("target"))
                    .filter(path -> !path.toString().contains(".git"))
                    .forEach(path -> javaFiles.add(path.toFile()));
        }

        return javaFiles;
    }

    /**
     * Analyzes a single Java file
     */
    private void analyzeJavaFile(File javaFile, ProjectAnalysis analysis,
            List<ApiEndpoint> endpoints, List<ComponentInfo> controllers,
            List<ComponentInfo> services, List<ComponentInfo> repositories,
            List<ComponentInfo> models, List<ComponentInfo> configurations,
            Map<String, List<String>> dependencyGraph) {

        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);

            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();

                // Check if this is the main application class
                if (isMainApplicationClass(cu)) {
                    String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
                    String className = cu.getPrimaryTypeName().orElse("Unknown");
                    analysis.setMainClass(packageName + "." + className);
                }

                // Analyze classes in the file
                cu.accept(new ClassVisitor(javaFile, endpoints, controllers, services, repositories, models,
                        configurations, dependencyGraph), null);
            }
        } catch (Exception e) {
            System.err.println("Error parsing file: " + javaFile.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    /**
     * Checks if a compilation unit contains the main application class
     */
    private boolean isMainApplicationClass(CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .anyMatch(clazz -> clazz.getAnnotations().stream()
                        .anyMatch(annotation -> annotation.getNameAsString().equals("SpringBootApplication")));
    }

    /**
     * Visitor class to analyze Java classes
     */
    private static class ClassVisitor extends VoidVisitorAdapter<Void> {

        private final File javaFile;
        private final List<ApiEndpoint> endpoints;
        private final List<ComponentInfo> controllers;
        private final List<ComponentInfo> services;
        private final List<ComponentInfo> repositories;
        private final List<ComponentInfo> models;
        private final List<ComponentInfo> configurations;
        private final Map<String, List<String>> dependencyGraph;

        public ClassVisitor(File javaFile, List<ApiEndpoint> endpoints, List<ComponentInfo> controllers,
                List<ComponentInfo> services, List<ComponentInfo> repositories,
                List<ComponentInfo> models, List<ComponentInfo> configurations,
                Map<String, List<String>> dependencyGraph) {
            this.javaFile = javaFile;
            this.endpoints = endpoints;
            this.controllers = controllers;
            this.services = services;
            this.repositories = repositories;
            this.models = models;
            this.configurations = configurations;
            this.dependencyGraph = dependencyGraph;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            super.visit(n, arg);

            String className = n.getNameAsString();
            String packageName = n.findCompilationUnit()
                    .flatMap(cu -> cu.getPackageDeclaration())
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            ComponentInfo component = new ComponentInfo(className, packageName, determineComponentType(n));
            component.setFilePath(javaFile.getAbsolutePath());
            component.setAnnotations(extractAnnotations(n));
            component.setMethods(extractMethods(n));
            component.setDependencies(extractDependencies(n));

            // Categorize the component
            String componentType = component.getComponentType();
            switch (componentType) {
                case "Controller" -> {
                    controllers.add(component);
                    // Extract API endpoints from controller
                    extractApiEndpoints(n, endpoints, packageName, className);
                }
                case "Service" -> services.add(component);
                case "Repository" -> repositories.add(component);
                case "Configuration" -> configurations.add(component);
                default -> models.add(component);
            }

            // Add to dependency graph
            dependencyGraph.put(component.getFullyQualifiedName(), component.getDependencies());
        }
    }

    /**
     * Determines the type of a Spring component based on annotations
     */
    private static String determineComponentType(ClassOrInterfaceDeclaration clazz) {
        List<String> annotations = extractAnnotations(clazz);

        if (annotations.contains("RestController") || annotations.contains("Controller")) {
            return "Controller";
        } else if (annotations.contains("Service")) {
            return "Service";
        } else if (annotations.contains("Repository")) {
            return "Repository";
        } else if (annotations.contains("Configuration")) {
            return "Configuration";
        } else if (annotations.contains("Entity") || annotations.contains("Document")
                || annotations.contains("Table")) {
            return "Entity";
        } else if (annotations.contains("Component")) {
            return "Component";
        }

        return "Model";
    }

    /**
     * Extracts annotations from a class
     */
    private static List<String> extractAnnotations(ClassOrInterfaceDeclaration clazz) {
        List<String> annotations = new ArrayList<>();
        for (AnnotationExpr annotation : clazz.getAnnotations()) {
            annotations.add(annotation.getNameAsString());
        }
        return annotations;
    }

    /**
     * Extracts method names from a class
     */
    private static List<String> extractMethods(ClassOrInterfaceDeclaration clazz) {
        List<String> methods = new ArrayList<>();
        for (MethodDeclaration method : clazz.getMethods()) {
            methods.add(method.getNameAsString());
        }
        return methods;
    }

    /**
     * Extracts dependencies (fields with @Autowired or similar) from a class
     */
    private static List<String> extractDependencies(ClassOrInterfaceDeclaration clazz) {
        List<String> dependencies = new ArrayList<>();

        for (FieldDeclaration field : clazz.getFields()) {
            boolean isInjected = field.getAnnotations().stream()
                    .anyMatch(annotation -> annotation.getNameAsString().equals("Autowired") ||
                            annotation.getNameAsString().equals("Inject") ||
                            annotation.getNameAsString().equals("Resource"));

            if (isInjected) {
                String fieldType = field.getElementType().asString();
                dependencies.add(fieldType);
            }
        }

        return dependencies;
    }

    /**
     * Extracts API endpoints from a controller class
     */
    private static void extractApiEndpoints(ClassOrInterfaceDeclaration controller, List<ApiEndpoint> endpoints,
            String packageName, String className) {

        String baseMapping = getBaseMappingFromController(controller);

        for (MethodDeclaration method : controller.getMethods()) {
            List<AnnotationExpr> requestMappings = method.getAnnotations().stream()
                    .filter(annotation -> isRequestMappingAnnotation(annotation.getNameAsString()))
                    .toList();

            for (AnnotationExpr mapping : requestMappings) {
                ApiEndpoint endpoint = createEndpointFromMapping(mapping, method, packageName, className, baseMapping);
                if (endpoint != null) {
                    endpoints.add(endpoint);
                }
            }
        }
    }

    /**
     * Gets base mapping from controller level RequestMapping annotation
     */
    private static String getBaseMappingFromController(ClassOrInterfaceDeclaration controller) {
        return controller.getAnnotations().stream()
                .filter(annotation -> annotation.getNameAsString().equals("RequestMapping"))
                .findFirst()
                .map(SpringBootAnalyzerService::extractMappingValue)
                .orElse("");
    }

    /**
     * Checks if annotation is a request mapping annotation
     */
    private static boolean isRequestMappingAnnotation(String annotationName) {
        return annotationName.equals("RequestMapping") ||
                annotationName.equals("GetMapping") ||
                annotationName.equals("PostMapping") ||
                annotationName.equals("PutMapping") ||
                annotationName.equals("DeleteMapping") ||
                annotationName.equals("PatchMapping");
    }

    /**
     * Creates an ApiEndpoint from a request mapping annotation
     */
    private static ApiEndpoint createEndpointFromMapping(AnnotationExpr mapping, MethodDeclaration method,
            String packageName, String className, String baseMapping) {

        String annotationName = mapping.getNameAsString();
        String httpMethod = getHttpMethodFromAnnotation(annotationName);
        String path = extractMappingValue(mapping);

        if (path.isEmpty() && annotationName.equals("RequestMapping")) {
            // Try to extract from method attribute
            path = extractMethodAttribute(mapping);
        }

        // Combine base mapping with method mapping
        String fullPath = combinePaths(baseMapping, path);

        ApiEndpoint endpoint = new ApiEndpoint(fullPath, httpMethod, packageName + "." + className,
                method.getNameAsString());
        endpoint.setReturnType(method.getType().asString());
        endpoint.setParameters(extractParameterTypes(method));
        endpoint.setAnnotations(method.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList());

        return endpoint;
    }

    /**
     * Extracts the HTTP method from annotation name
     */
    private static String getHttpMethodFromAnnotation(String annotationName) {
        return switch (annotationName) {
            case "GetMapping" -> "GET";
            case "PostMapping" -> "POST";
            case "PutMapping" -> "PUT";
            case "DeleteMapping" -> "DELETE";
            case "PatchMapping" -> "PATCH";
            case "RequestMapping" -> "GET"; // Default to GET for RequestMapping
            default -> "UNKNOWN";
        };
    }

    /**
     * Extracts mapping value from annotation
     */
    private static String extractMappingValue(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr) {
            return ((SingleMemberAnnotationExpr) annotation).getMemberValue().toString().replace("\"", "");
        } else if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
            return normalAnnotation.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path"))
                    .findFirst()
                    .map(MemberValuePair::getValue)
                    .map(value -> value.toString().replace("\"", ""))
                    .orElse("");
        }
        return "";
    }

    /**
     * Extracts method attribute from RequestMapping annotation
     */
    private static String extractMethodAttribute(AnnotationExpr annotation) {
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
            return normalAnnotation.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("method"))
                    .findFirst()
                    .map(MemberValuePair::getValue)
                    .map(Object::toString)
                    .orElse("");
        }
        return "";
    }

    /**
     * Combines base path and method path
     */
    private static String combinePaths(String basePath, String methodPath) {
        if (basePath == null)
            basePath = "";
        if (methodPath == null)
            methodPath = "";

        basePath = basePath.trim();
        methodPath = methodPath.trim();

        if (basePath.isEmpty())
            return methodPath;
        if (methodPath.isEmpty())
            return basePath;

        if (!basePath.startsWith("/"))
            basePath = "/" + basePath;
        if (!methodPath.startsWith("/"))
            methodPath = "/" + methodPath;

        return basePath + methodPath;
    }

    /**
     * Extracts parameter types from a method
     */
    private static List<String> extractParameterTypes(MethodDeclaration method) {
        return method.getParameters().stream()
                .map(param -> param.getType().asString())
                .toList();
    }

    /**
     * Builds package structure map
     */
    private Map<String, String> buildPackageStructure(List<File> javaFiles) {
        Map<String, String> packageStructure = new HashMap<>();

        for (File javaFile : javaFiles) {
            try {
                ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);
                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    String packageName = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse("default");
                    String className = cu.getPrimaryTypeName().orElse("Unknown");
                    packageStructure.put(packageName + "." + className, packageName);
                }
            } catch (Exception e) {
                System.err.println("Error processing file for package structure: " + javaFile.getAbsolutePath());
            }
        }

        return packageStructure;
    }

    /**
     * Extracts project name from project path
     */
    private String extractProjectName(String projectPath) {
        return Paths.get(projectPath).getFileName().toString();
    }
}