package com.app.ski.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.ski.model.BuildRecord;
import com.app.ski.model.SkiProject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BuildManager {

    private static final String PREFS = "ski_builds";
    private static final String KEY_BUILDS = "build_records";

    private final Context context;
    private final SharedPreferences prefs;

    private final List<BuildRecord> builds =
            new ArrayList<>();

    public BuildManager(Context context) {

        this.context =
                context.getApplicationContext();

        prefs =
                this.context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        load();
    }

    private void load() {

        builds.clear();

        String raw =
                prefs.getString(
                        KEY_BUILDS,
                        "[]"
                );

        try {

            JSONArray array =
                    new JSONArray(raw);

            for (int i = 0;
                 i < array.length();
                 i++) {

                builds.add(
                        BuildRecord.fromJson(
                                array.getJSONObject(i)
                        )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        sortBuilds();
    }

    private synchronized void save() {

        JSONArray array =
                new JSONArray();

        for (BuildRecord build : builds) {
            array.put(build.toJson());
        }

        prefs.edit()
                .putString(
                        KEY_BUILDS,
                        array.toString()
                )
                .apply();
    }

    private void sortBuilds() {

        Collections.sort(
                builds,
                new Comparator<BuildRecord>() {

                    @Override
                    public int compare(
                            BuildRecord a,
                            BuildRecord b
                    ) {

                        return Long.compare(
                                b.getCreatedAt(),
                                a.getCreatedAt()
                        );
                    }
                }
        );
    }

    public synchronized List<BuildRecord>
    getAllBuilds() {

        return new ArrayList<>(
                builds
        );
    }

    public synchronized List<BuildRecord>
    getApkBuilds() {

        List<BuildRecord> result =
                new ArrayList<>();

        for (BuildRecord build : builds) {

            if (BuildRecord.TYPE_APK
                    .equals(build.getType())) {

                result.add(build);
            }
        }

        return result;
    }

    public synchronized List<BuildRecord>
    getDesktopBuilds() {

        List<BuildRecord> result =
                new ArrayList<>();

        for (BuildRecord build : builds) {

            if (BuildRecord.TYPE_DESKTOP
                    .equals(build.getType())) {

                result.add(build);
            }
        }

        return result;
    }

    public synchronized List<BuildRecord>
    getProjectBuilds(
            String projectId,
            String type
    ) {

        List<BuildRecord> result =
                new ArrayList<>();

        for (BuildRecord build : builds) {

            if (projectId.equals(
                    build.getProjectId()
            )
                    && type.equals(
                    build.getType()
            )) {

                result.add(build);
            }
        }

        return result;
    }

    public synchronized BuildRecord
    findDuplicate(
            String projectId,
            String type,
            String fingerprint
    ) {

        for (BuildRecord build : builds) {

            if (projectId.equals(
                    build.getProjectId()
            )
                    && type.equals(
                    build.getType()
            )
                    && fingerprint.equals(
                    build.getFingerprint()
            )) {

                File file =
                        new File(
                                build.getFilePath()
                        );

                if (file.exists()) {
                    return build;
                }
            }
        }

        return null;
    }

    public synchronized BuildRecord addBuild(
            String projectId,
            String projectName,
            String type,
            String version,
            File file,
            String fingerprint
    ) {

        String id =
                UUID.randomUUID().toString();

        BuildRecord build =
                new BuildRecord(
                        id,
                        projectId,
                        projectName,
                        type,
                        version,
                        file.getAbsolutePath(),
                        file.getName(),
                        fingerprint,
                        System.currentTimeMillis()
                );

        builds.add(build);

        sortBuilds();

        save();

        return build;
    }

    public synchronized void deleteBuild(
            BuildRecord build
    ) {

        File file =
                new File(
                        build.getFilePath()
                );

        if (file.exists()) {
            file.delete();
        }

        builds.remove(build);

        save();
    }

    public String calculateProjectFingerprint(
            SkiProject project
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            StringBuilder content =
                    new StringBuilder();

            content.append(
                    project.getName()
            );

            for (com.app.ski.model.SkiFile file
                    : project.getFiles()) {

                content.append(
                        "\n---FILE---\n"
                );

                content.append(
                        file.getName()
                );

                content.append("\n");

                content.append(
                        file.getContent()
                );
            }

            byte[] hash =
                    digest.digest(
                            content.toString()
                                    .getBytes("UTF-8")
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {

                hex.append(
                        String.format(
                                "%02x",
                                b & 0xff
                        )
                );
            }

            return hex.toString();

        } catch (Exception e) {

            return String.valueOf(
                    project.hashCode()
            );
        }
    }
}