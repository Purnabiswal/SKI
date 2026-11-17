package com.app.ski.model;

public class SkiFile {
    private final String name;
    private String content;
    public SkiFile(String name, String content) { this.name = name; this.content = content == null ? "" : content; }
    public String getName() { return name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
}
