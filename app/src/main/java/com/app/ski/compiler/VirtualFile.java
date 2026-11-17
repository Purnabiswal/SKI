package com.app.ski.compiler;


public class VirtualFile {

    private final String path;
    private final String content;

    public VirtualFile(String path, String content) {
        this.path = path;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }
}