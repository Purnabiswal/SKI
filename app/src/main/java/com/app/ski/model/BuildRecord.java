package com.app.ski.model;

import org.json.JSONObject;

public class BuildRecord {

    public static final String TYPE_APK = "APK";
    public static final String TYPE_DESKTOP = "DESKTOP";

    private String id;
    private String projectId;
    private String projectName;
    private String type;
    private String version;
    private String filePath;
    private String fileName;
    private String fingerprint;
    private long createdAt;

    public BuildRecord() {
    }

    public BuildRecord(
            String id,
            String projectId,
            String projectName,
            String type,
            String version,
            String filePath,
            String fileName,
            String fingerprint,
            long createdAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.type = type;
        this.version = version;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fingerprint = fingerprint;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();

        try {
            o.put("id", id);
            o.put("projectId", projectId);
            o.put("projectName", projectName);
            o.put("type", type);
            o.put("version", version);
            o.put("filePath", filePath);
            o.put("fileName", fileName);
            o.put("fingerprint", fingerprint);
            o.put("createdAt", createdAt);
        } catch (Exception ignored) {
        }

        return o;
    }

    public static BuildRecord fromJson(JSONObject o) {

        BuildRecord b = new BuildRecord();

        b.id = o.optString("id", "");
        b.projectId = o.optString("projectId", "");
        b.projectName = o.optString("projectName", "");
        b.type = o.optString("type", "");
        b.version = o.optString("version", "1.0");
        b.filePath = o.optString("filePath", "");
        b.fileName = o.optString("fileName", "");
        b.fingerprint = o.optString("fingerprint", "");
        b.createdAt = o.optLong("createdAt", 0);

        return b;
    }
}