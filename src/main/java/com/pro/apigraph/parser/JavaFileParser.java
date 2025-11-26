package com.pro.apigraph.parser;

import com.pro.apigraph.model.Dependency;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JavaFileParser {

    public static List<Dependency> parse(Path file, String sourceService) {
        List<Dependency> deps = new ArrayList<>();

        try {
            String code = Files.readString(file, StandardCharsets.UTF_8);
            CompilationUnit cu = StaticJavaParser.parse(code);

            // 1. Detect @FeignClient
            detectFeignClients(cu, sourceService, deps);

            // 2. Detect RestTemplate calls
            detectRestTemplateCalls(cu, sourceService, deps);

            // 3. Detect WebClient calls
            detectWebClientCalls(cu, sourceService, deps);

            // 4. Detect Controller endpoints (creates reverse dependencies)
            detectControllerEndpoints(cu, sourceService, deps);

        } catch (Exception e) {
            System.err.println("Error parsing " + file + ": " + e.getMessage());
        }

        return deps;
    }

    private static void detectFeignClients(CompilationUnit cu, String sourceService, List<Dependency> deps) {
        cu.findAll(AnnotationExpr.class).stream()
                .filter(a -> a.getNameAsString().equals("FeignClient"))
                .forEach(a -> {
                    if (a.isNormalAnnotationExpr()) {
                        a.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                            String key = pair.getNameAsString();
                            String value = pair.getValue().toString().replace("\"", "");
                            if (key.equals("name") || key.equals("value")) {
                                deps.add(new Dependency(sourceService, value, "/", "Feign"));
                                System.out.println("  [Feign] " + sourceService + " -> " + value);
                            } else if (key.equals("url")) {
                                Dependency d = parseUrl(sourceService, value, "Feign");
                                deps.add(d);
                                System.out.println("  [Feign URL] " + sourceService + " -> " + d.getTarget());
                            }
                        });
                    } else if (a.isSingleMemberAnnotationExpr()) {
                        String value = a.asSingleMemberAnnotationExpr().getMemberValue().toString().replace("\"", "");
                        deps.add(new Dependency(sourceService, value, "/", "Feign"));
                        System.out.println("  [Feign] " + sourceService + " -> " + value);
                    }
                });
    }

    private static void detectRestTemplateCalls(CompilationUnit cu, String sourceService, List<Dependency> deps) {
        cu.findAll(MethodCallExpr.class).stream()
                .filter(m -> isRestTemplateMethod(m.getNameAsString()))
                .forEach(m -> {
                    // First argument is usually the URL
                    if (m.getArguments().size() > 0) {
                        Expression firstArg = m.getArguments().get(0);
                        extractStringLiteral(firstArg).ifPresent(url -> {
                            if (isValidUrlOrPath(url)) {
                                Dependency d = parseUrl(sourceService, url, "RestTemplate");
                                deps.add(d);
                                System.out.println(
                                        "  [RestTemplate] " + sourceService + " -> " + d.getTarget() + d.getLabel());
                            }
                        });
                    }
                });
    }

    private static boolean isRestTemplateMethod(String methodName) {
        return methodName.equals("getForObject") ||
                methodName.equals("getForEntity") ||
                methodName.equals("postForObject") ||
                methodName.equals("postForEntity") ||
                methodName.equals("put") ||
                methodName.equals("delete") ||
                methodName.equals("exchange") ||
                methodName.equals("execute");
    }

    private static void detectWebClientCalls(CompilationUnit cu, String sourceService, List<Dependency> deps) {
        cu.findAll(MethodCallExpr.class).stream()
                .filter(m -> m.getNameAsString().equals("uri"))
                .forEach(m -> {
                    if (m.getArguments().size() > 0) {
                        Expression firstArg = m.getArguments().get(0);
                        extractStringLiteral(firstArg).ifPresent(url -> {
                            if (isValidUrlOrPath(url)) {
                                Dependency d = parseUrl(sourceService, url, "WebClient");
                                deps.add(d);
                                System.out.println(
                                        "  [WebClient] " + sourceService + " -> " + d.getTarget() + d.getLabel());
                            }
                        });
                    }
                });
    }

    private static void detectControllerEndpoints(CompilationUnit cu, String sourceService, List<Dependency> deps) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            // Check if class is a controller
            boolean isController = cls.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals("RestController") ||
                            a.getNameAsString().equals("Controller"));

            if (!isController)
                return;

            // Get base path from @RequestMapping on class
            String basePath = "";
            for (AnnotationExpr ann : cls.getAnnotations()) {
                if (ann.getNameAsString().equals("RequestMapping")) {
                    basePath = extractPathFromMapping(ann);
                }
            }

            // Process each method
            String finalBasePath = basePath;
            cls.findAll(MethodDeclaration.class).forEach(method -> {
                for (AnnotationExpr ann : method.getAnnotations()) {
                    String annName = ann.getNameAsString();
                    if (isMappingAnnotation(annName)) {
                        String methodPath = extractPathFromMapping(ann);
                        String fullPath = combinePaths(finalBasePath, methodPath);
                        String httpMethod = getHttpMethod(annName);

                        // Create a "virtual" dependency showing this service exposes an endpoint
                        // Format: endpoint -> sourceService (reverse of normal dependency)
                        deps.add(new Dependency("EXTERNAL", sourceService, fullPath, httpMethod + "-Endpoint"));
                        System.out.println("  [Endpoint] " + sourceService + " exposes " + httpMethod + " " + fullPath);
                    }
                }
            });
        });
    }

    private static boolean isMappingAnnotation(String name) {
        return name.equals("GetMapping") ||
                name.equals("PostMapping") ||
                name.equals("PutMapping") ||
                name.equals("DeleteMapping") ||
                name.equals("PatchMapping") ||
                name.equals("RequestMapping");
    }

    private static String getHttpMethod(String annotationName) {
        if (annotationName.equals("GetMapping"))
            return "GET";
        if (annotationName.equals("PostMapping"))
            return "POST";
        if (annotationName.equals("PutMapping"))
            return "PUT";
        if (annotationName.equals("DeleteMapping"))
            return "DELETE";
        if (annotationName.equals("PatchMapping"))
            return "PATCH";
        return "REQUEST";
    }

    private static String extractPathFromMapping(AnnotationExpr ann) {
        if (ann.isSingleMemberAnnotationExpr()) {
            String value = ann.asSingleMemberAnnotationExpr().getMemberValue().toString();
            return cleanPath(value);
        } else if (ann.isNormalAnnotationExpr()) {
            return ann.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("value") || p.getNameAsString().equals("path"))
                    .findFirst()
                    .map(p -> cleanPath(p.getValue().toString()))
                    .orElse("");
        }
        return "";
    }

    private static String cleanPath(String path) {
        // Remove quotes and array brackets
        path = path.replace("\"", "").replace("{", "").replace("}", "");
        if (path.startsWith("[") && path.endsWith("]")) {
            path = path.substring(1, path.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    private static String combinePaths(String base, String method) {
        if (base.isEmpty())
            return method;
        if (method.isEmpty())
            return base;
        if (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);
        if (!method.startsWith("/"))
            method = "/" + method;
        return base + method;
    }

    private static Optional<String> extractStringLiteral(Expression e) {
        if (e.isStringLiteralExpr()) {
            return Optional.of(e.asStringLiteralExpr().getValue());
        }
        return Optional.empty();
    }

    private static Dependency parseUrl(String source, String url, String method) {
        String cleaned = url.replace("http://", "").replace("https://", "");

        // Filter out placeholders and format specifiers
        if (cleaned.contains("%") || cleaned.contains("${") || cleaned.contains("{")) {
            return new Dependency(source, "config-dependent", "/", method);
        }

        String[] parts = cleaned.split("/", 2);
        String target = parts[0];

        // Remove port if present
        if (target.contains(":")) {
            target = target.split(":")[0];
        }

        // Additional noise filtering
        if (target.length() < 2 || !isValidHostname(target)) {
            return new Dependency(source, "unknown", "/", method);
        }

        String endpoint = parts.length > 1 ? "/" + parts[1] : "/";
        return new Dependency(source, target, endpoint, method);
    }

    private static boolean isValidHostname(String s) {
        return s.matches("[a-zA-Z0-9_\\-.]+");
    }

    private static boolean isValidUrlOrPath(String s) {
        if (s.startsWith("http") || s.startsWith("/"))
            return true;
        // Filter out common noise
        if (s.contains(" ") || s.contains("\n") || s.contains("\t"))
            return false;
        if (s.equalsIgnoreCase("application/json"))
            return false;
        if (s.equalsIgnoreCase("Content-Type"))
            return false;
        // Allow potential hostnames if they look valid
        return isValidHostname(s) && s.contains(".");
    }
}