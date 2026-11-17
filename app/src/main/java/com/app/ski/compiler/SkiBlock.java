package com.app.ski.compiler;

public class SkiBlock {

    public enum Type {
        FILE,
        ROUTE
    }

    private final Type type;
    private final String path;
    private final String method;
    private final String content;

    public SkiBlock(
            Type type,
            String path,
            String method,
            String content
    ) {
        this.type = type;
        this.path = path;
        this.method = method;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getContent() {
        return content;
    }
}