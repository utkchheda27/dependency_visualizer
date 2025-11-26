package com.pro.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ApiDependencyParser {

    public static class Dependency {
        public String source;
        public String target;
        public String label;
        public String method;

        public Dependency(String source, String target, String label, String method) {
            this.source = source;
            this.target = target;
            this.label = label;
            this.method = method;
        }
    }

    public List<Dependency> parse(CompilationUnit cu, String sourceService) {
        List<Dependency> deps = new ArrayList<>();

        try {
            // 1. Detect @FeignClient
            detectFeignClients(cu, sourceService, deps);

            // 2. Detect RestTemplate calls
            detectRestTemplateCalls(cu, sourceService, deps);

            // 3. Detect WebClient calls
            detectWebClientCalls(cu, sourceService, deps);

        } catch (Exception e) {
            System.err.println("Error parsing dependencies for " + sourceService + ": " + e.getMessage());
        }

        return deps;
    }

    private void detectFeignClients(CompilationUnit cu, String sourceService, List<Dependency> deps) {
        cu.findAll(AnnotationExpr.class).stream()
                .filter(a -> a.getNameAsString().equals("FeignClient"))
                .forEach(a -> {
                    if (a.isNormalAnnotationExpr()) {
                        a.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                            String key = pair.getNameAsString();
                            String value = pair.getValue().toString().replace("\"", "");
                            if (key.equals("name") || key.equals("value")) {
                                deps.add(new Dependency(sourceService, value, "/", "Feign"));
                            } else if (key.equals("url")) {
                                Dependency d = parseUrl(sourceService, value, "Feign");
                                deps.add(d);
                            }
                        });
                    } else if (a.isSingleMemberAnnotationExpr()) {
                        String value = a.asSingleMemberAnnotationExpr().getMemberValue().toString().replace("\"", "");
                        deps.add(new Dependency(sourceService, value, "/", "Feign"));
                    }
                });
    }

    private void detectRestTemplateCalls(CompilationUnit cu, String sourceService, List<Dependency> deps) {
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
                            }
                        });
                    }
                });
    }

    private boolean isRestTemplateMethod(String methodName) {
        return methodName.equals("getForObject") ||
                methodName.equals("getForEntity") ||
                methodName.equals("postForObject") ||
                methodName.equals("postForEntity") ||
                methodName.equals("put") ||
                methodName.equals("delete") ||
                methodName.equals("exchange") ||
                methodName.equals("execute");
    }

    private void detectWebClientCalls(CompilationUnit cu, String sourceService, List<Dependency> deps) {
        cu.findAll(MethodCallExpr.class).stream()
                .filter(m -> m.getNameAsString().equals("uri"))
                .forEach(m -> {
                    if (m.getArguments().size() > 0) {
                        Expression firstArg = m.getArguments().get(0);
                        extractStringLiteral(firstArg).ifPresent(url -> {
                            if (isValidUrlOrPath(url)) {
                                Dependency d = parseUrl(sourceService, url, "WebClient");
                                deps.add(d);
                            }
                        });
                    }
                });
    }

    private Optional<String> extractStringLiteral(Expression e) {
        if (e.isStringLiteralExpr()) {
            return Optional.of(e.asStringLiteralExpr().getValue());
        }
        return Optional.empty();
    }

    private Dependency parseUrl(String source, String url, String method) {
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

    private boolean isValidHostname(String s) {
        return s.matches("[a-zA-Z0-9_\\-.]+");
    }

    private boolean isValidUrlOrPath(String s) {
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
