package com.pro.apigraph.parser;

import com.pro.apigraph.model.Dependency;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlConfigParser {

    // Pattern to match URLs in config files
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[a-zA-Z0-9_\\-./:]+(?:/[a-zA-Z0-9_\\-./?]*)?)");

    // Pattern to match property-style URLs (key: value)
    private static final Pattern PROPERTY_URL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._\\-]+\\s*[:=]\\s*(https?://[^\\s,;\"'\\]\\}]+)");

    public static List<Dependency> parse(Path file, String sourceService) {
        List<Dependency> deps = new ArrayList<>();

        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);

            // Extract all URLs from the config file
            Matcher matcher = PROPERTY_URL_PATTERN.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                if (isValidUrl(url)) {
                    Dependency d = parseUrl(sourceService, url, "Config");
                    if (!d.getTarget().equals("unknown") && !d.getTarget().equals("config-dependent")) {
                        deps.add(d);
                        System.out.println("  [Config URL] " + sourceService + " -> " + d.getTarget() + d.getLabel());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing config " + file + ": " + e.getMessage());
        }

        return deps;
    }

    private static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty())
            return false;

        // Must start with http:// or https://
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            return false;

        // Filter out example/placeholder URLs
        String lower = url.toLowerCase();
        if (lower.contains("example.com") ||
                lower.contains("localhost") ||
                lower.contains("127.0.0.1") ||
                lower.contains("0.0.0.0")) {
            return false;
        }

        return true;
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

        // Validate hostname
        if (target.length() < 2 || !isValidHostname(target)) {
            return new Dependency(source, "unknown", "/", method);
        }

        String endpoint = parts.length > 1 ? "/" + parts[1] : "/";
        return new Dependency(source, target, endpoint, method);
    }

    private static boolean isValidHostname(String s) {
        return s.matches("[a-zA-Z0-9_\\-.]+");
    }
}
