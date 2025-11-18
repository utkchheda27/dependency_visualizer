package com.pro.model;

import java.util.List;
import java.util.Map;

/**
 * Represents an API endpoint in a Spring Boot application
 */
public class ApiEndpoint {
    private String path;
    private String httpMethod;
    private String controllerClass;
    private String methodName;
    private List<String> parameters;
    private String returnType;
    private List<String> annotations;
    private Map<String, String> requestMappingDetails;

    // Constructors
    public ApiEndpoint() {
    }

    public ApiEndpoint(String path, String httpMethod, String controllerClass, String methodName) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.controllerClass = controllerClass;
        this.methodName = methodName;
    }

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(String controllerClass) {
        this.controllerClass = controllerClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public Map<String, String> getRequestMappingDetails() {
        return requestMappingDetails;
    }

    public void setRequestMappingDetails(Map<String, String> requestMappingDetails) {
        this.requestMappingDetails = requestMappingDetails;
    }

    @Override
    public String toString() {
        return "ApiEndpoint{" +
                "path='" + path + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", controllerClass='" + controllerClass + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}