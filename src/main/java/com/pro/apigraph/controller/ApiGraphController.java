package com.pro.apigraph.controller;

import com.pro.apigraph.model.Dependency;
import com.pro.apigraph.model.RepoScanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiGraphController {

    @PostMapping("/scan")
    public ResponseEntity<?> scanRepository(@RequestBody Map<String, String> payload) {
        String pathStr = payload.get("path");
        if (pathStr == null || pathStr.isBlank()) {
            return ResponseEntity.badRequest().body("Path is required");
        }

        Path path = Paths.get(pathStr);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return ResponseEntity.badRequest().body("Invalid directory path");
        }

        try {
            RepoScanner scanner = new RepoScanner(path);
            List<Dependency> dependencies = scanner.scan();
            return ResponseEntity.ok(toCytoscape(dependencies));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error scanning repository: " + e.getMessage());
        }
    }

    private Map<String, Object> toCytoscape(List<Dependency> deps) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> nodeSet = new HashSet<>();

        for (Dependency d : deps) {
            nodeSet.add(d.getSource());
            nodeSet.add(d.getTarget());
        }

        for (String n : nodeSet) {
            nodes.add(Map.of("data", Map.of("id", n, "label", n)));
        }

        int edgeId = 1;
        for (Dependency d : deps) {
            edges.add(Map.of(
                    "data", Map.of(
                            "id", "e" + edgeId++,
                            "source", d.getSource(),
                            "target", d.getTarget(),
                            "label",
                            (d.getMethod() != null ? d.getMethod() + " " : "")
                                    + (d.getLabel() != null ? d.getLabel() : ""),
                            "method", d.getMethod() != null ? d.getMethod() : "")));
        }

        return Map.of("elements", Map.of("nodes", nodes, "edges", edges));
    }
}
