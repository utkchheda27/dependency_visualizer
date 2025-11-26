package com.pro.apigraph.model;

public class Dependency {
    private String source;
    private String target;
    private String label;
    private String method;

    public Dependency() {
    }

    public Dependency(String source, String target, String label, String method) {
        this.source = source;
        this.target = target;
        this.label = label;
        this.method = method;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
