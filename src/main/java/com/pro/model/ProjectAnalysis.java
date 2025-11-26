package com.pro.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the complete analysis result of a Spring Boot project
 */
public class ProjectAnalysis {
    private String projectName;
    private String projectPath;
    private String mainClass;
    private List<ApiEndpoint> apiEndpoints;
    private List<ComponentInfo> controllers;
    private List<ComponentInfo> services;
    private List<ComponentInfo> repositories;
    private List<ComponentInfo> models;
    private List<ComponentInfo> configurations;
    private Map<String, List<String>> dependencyGraph; // Component -> List of dependencies
    private Map<String, String> packageStructure;
    private List<ModuleInfo> modules;
    private long analysisTimestamp;

    private List<ComponentInfo> externalDependencies;

    // Constructors
    public ProjectAnalysis() {
    }

    public ProjectAnalysis(String projectName, String projectPath) {
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.analysisTimestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public List<ApiEndpoint> getApiEndpoints() {
        return apiEndpoints;
    }

    public void setApiEndpoints(List<ApiEndpoint> apiEndpoints) {
        this.apiEndpoints = apiEndpoints;
    }

    public List<ComponentInfo> getControllers() {
        return controllers;
    }

    public void setControllers(List<ComponentInfo> controllers) {
        this.controllers = controllers;
    }

    public List<ComponentInfo> getServices() {
        return services;
    }

    public void setServices(List<ComponentInfo> services) {
        this.services = services;
    }

    public List<ComponentInfo> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<ComponentInfo> repositories) {
        this.repositories = repositories;
    }

    public List<ComponentInfo> getModels() {
        return models;
    }

    public void setModels(List<ComponentInfo> models) {
        this.models = models;
    }

    public List<ComponentInfo> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<ComponentInfo> configurations) {
        this.configurations = configurations;
    }

    public List<ComponentInfo> getExternalDependencies() {
        return externalDependencies;
    }

    public void setExternalDependencies(List<ComponentInfo> externalDependencies) {
        this.externalDependencies = externalDependencies;
    }

    public Map<String, List<String>> getDependencyGraph() {
        return dependencyGraph;
    }

    public void setDependencyGraph(Map<String, List<String>> dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    public Map<String, String> getPackageStructure() {
        return packageStructure;
    }

    public void setPackageStructure(Map<String, String> packageStructure) {
        this.packageStructure = packageStructure;
    }

    public List<ModuleInfo> getModules() {
        return modules;
    }

    public void setModules(List<ModuleInfo> modules) {
        this.modules = modules;
    }

    public long getAnalysisTimestamp() {
        return analysisTimestamp;
    }

    public void setAnalysisTimestamp(long analysisTimestamp) {
        this.analysisTimestamp = analysisTimestamp;
    }

    // Utility methods
    public int getTotalEndpoints() {
        return apiEndpoints != null ? apiEndpoints.size() : 0;
    }

    public int getTotalComponents() {
        int total = 0;
        if (controllers != null)
            total += controllers.size();
        if (services != null)
            total += services.size();
        if (repositories != null)
            total += repositories.size();
        if (models != null)
            total += models.size();
        if (configurations != null)
            total += configurations.size();
        if (externalDependencies != null)
            total += externalDependencies.size();
        return total;
    }

    @Override
    public String toString() {
        return "ProjectAnalysis{" +
                "projectName='" + projectName + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", totalEndpoints=" + getTotalEndpoints() +
                ", totalComponents=" + getTotalComponents() +
                ", analysisTimestamp=" + analysisTimestamp +
                '}';
    }
}