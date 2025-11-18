package com.pro.controller;

import com.pro.model.ProjectAnalysis;
import com.pro.service.DependencyAnalysisService;
import com.pro.service.SpringBootAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analyzer")
@CrossOrigin(origins = "*")
public class AnalyzerController {

    @Autowired
    private SpringBootAnalyzerService analyzerService;

    @Autowired
    private DependencyAnalysisService dependencyAnalysisService;

    /**
     * Analyzes a Spring Boot project at the given path
     */
    @PostMapping("/analyze")
    public ResponseEntity<ProjectAnalysis> analyzeProject(@RequestBody Map<String, String> request) {
        try {
            String projectPath = request.get("projectPath");

            if (projectPath == null || projectPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Project path is required");
            }

            ProjectAnalysis analysis = analyzerService.analyzeProject(projectPath);
            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze project: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes the current project (self-analysis)
     */
    @GetMapping("/analyze-self")
    public ResponseEntity<ProjectAnalysis> analyzeSelf() {
        try {
            // Get the current project path (where this application is running)
            String currentPath = System.getProperty("user.dir");
            ProjectAnalysis analysis = analyzerService.analyzeProject(currentPath);
            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze current project: " + e.getMessage(), e);
        }
    }

    /**
     * Gets basic project statistics
     */
    @PostMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProjectStats(@RequestBody Map<String, String> request) {
        try {
            String projectPath = request.get("projectPath");

            if (projectPath == null || projectPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Project path is required");
            }

            ProjectAnalysis analysis = analyzerService.analyzeProject(projectPath);

            Map<String, Object> stats = new HashMap<>();
            stats.put("projectName", analysis.getProjectName());
            stats.put("mainClass", analysis.getMainClass());
            stats.put("totalEndpoints", analysis.getTotalEndpoints());
            stats.put("totalComponents", analysis.getTotalComponents());
            stats.put("controllerCount", analysis.getControllers() != null ? analysis.getControllers().size() : 0);
            stats.put("serviceCount", analysis.getServices() != null ? analysis.getServices().size() : 0);
            stats.put("repositoryCount", analysis.getRepositories() != null ? analysis.getRepositories().size() : 0);
            stats.put("modelCount", analysis.getModels() != null ? analysis.getModels().size() : 0);
            stats.put("configurationCount",
                    analysis.getConfigurations() != null ? analysis.getConfigurations().size() : 0);
            stats.put("analysisTimestamp", analysis.getAnalysisTimestamp());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get project stats: " + e.getMessage(), e);
        }
    }

    /**
     * Gets dependency analysis metrics for a project
     */
    @PostMapping("/dependency-metrics")
    public ResponseEntity<Map<String, Object>> getDependencyMetrics(@RequestBody Map<String, String> request) {
        try {
            String projectPath = request.get("projectPath");

            if (projectPath == null || projectPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Project path is required");
            }

            ProjectAnalysis analysis = analyzerService.analyzeProject(projectPath);
            Map<String, Object> metrics = dependencyAnalysisService.calculateDependencyMetrics(analysis);

            // Add circular dependency detection
            List<List<String>> circularDependencies = dependencyAnalysisService.detectCircularDependencies(analysis);
            metrics.put("circularDependencies", circularDependencies);
            metrics.put("hasCircularDependencies", !circularDependencies.isEmpty());

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate dependency metrics: " + e.getMessage(), e);
        }
    }

    /**
     * Gets dependency metrics for the current project
     */
    @GetMapping("/dependency-metrics-self")
    public ResponseEntity<Map<String, Object>> getDependencyMetricsSelf() {
        try {
            String currentPath = System.getProperty("user.dir");
            ProjectAnalysis analysis = analyzerService.analyzeProject(currentPath);
            Map<String, Object> metrics = dependencyAnalysisService.calculateDependencyMetrics(analysis);

            // Add circular dependency detection
            List<List<String>> circularDependencies = dependencyAnalysisService.detectCircularDependencies(analysis);
            metrics.put("circularDependencies", circularDependencies);
            metrics.put("hasCircularDependencies", !circularDependencies.isEmpty());

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate dependency metrics: " + e.getMessage(), e);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Spring Boot Project Analyzer");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(status);
    }

    /**
     * Global exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal Server Error");
        error.put("message", e.getMessage());
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.internalServerError().body(error);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad Request");
        error.put("message", e.getMessage());
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.badRequest().body(error);
    }
}