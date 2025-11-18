package com.pro.model;

import java.util.List;

/**
 * Represents a Spring component (Service, Repository, Component, etc.)
 */
public class ComponentInfo {
    private String className;
    private String packageName;
    private String componentType; // Service, Repository, Component, Controller
    private List<String> annotations;
    private List<String> methods;
    private List<String> dependencies; // Other components this depends on
    private List<String> usedBy; // Components that use this component
    private String filePath;

    // Constructors
    public ComponentInfo() {
    }

    public ComponentInfo(String className, String packageName, String componentType) {
        this.className = className;
        this.packageName = packageName;
        this.componentType = componentType;
    }

    // Getters and Setters
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(List<String> usedBy) {
        this.usedBy = usedBy;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFullyQualifiedName() {
        return packageName + "." + className;
    }

    @Override
    public String toString() {
        return "ComponentInfo{" +
                "className='" + className + '\'' +
                ", packageName='" + packageName + '\'' +
                ", componentType='" + componentType + '\'' +
                ", dependencies=" + (dependencies != null ? dependencies.size() : 0) +
                ", usedBy=" + (usedBy != null ? usedBy.size() : 0) +
                '}';
    }
}