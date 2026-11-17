package com.app.ski.model;
import java.util.ArrayList;
import java.util.List;

public class SkiProject {
    private String name;
    private boolean pinned;
    private final List<SkiFile> files = new ArrayList<>();
    public SkiProject(String name) { this.name = name; }
    public String getName() { return name; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public List<SkiFile> getFiles() { return files; }
    public SkiFile getFile(String name) {
        for (SkiFile f : files) if (f.getName().equals(name)) return f;
        return null;
    }
    public void addFile(String name, String content) {
        if (getFile(name) == null) files.add(new SkiFile(name, content));
    }
}
