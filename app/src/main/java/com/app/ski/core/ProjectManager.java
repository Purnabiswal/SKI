package com.app.ski.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.ski.model.SkiFile;
import com.app.ski.model.SkiProject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProjectManager {

    private final SharedPreferences prefs;
    private final List<SkiProject> projects = new ArrayList<>();

    public ProjectManager(Context c) {
        prefs = c.getSharedPreferences("ski_projects", Context.MODE_PRIVATE);

        load();

        // Create the initial project if no projects exist.
        if (projects.isEmpty()) {

            SkiProject p = new SkiProject("project 1");
            p.setPinned(true);

            // -----------------------------
            // Default frontend.ski
            // -----------------------------
            String frontend =
                    "@file \"index.html\" {\n" +
                            "<!doctype html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n" +
                            "    <style>\n" +
                            "        body {\n" +
                            "            font-family: system-ui;\n" +
                            "            background: #0b0f19;\n" +
                            "            color: white;\n" +
                            "            padding: 40px;\n" +
                            "        }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <h1>SKI is running 🚀</h1>\n" +
                            "    <p>Edit frontend.ski and press Run.</p>\n" +
                            "</body>\n" +
                            "</html>\n" +
                            "}";

            p.addFile("frontend.ski", frontend);

            // -----------------------------
            // Default backend.ski
            // -----------------------------
            String backend =
                    "@route GET \"/api/hello\" {\n" +
                            "    return { \"message\": \"Hello from SKI backend\" };\n" +
                            "}";

            p.addFile("backend.ski", backend);

            projects.add(p);

            save();
        }
    }

    // --------------------------------------------------
    // Get all projects
    // --------------------------------------------------

    public List<SkiProject> getProjects() {
        return projects;
    }

    // --------------------------------------------------
    // Get currently pinned project
    // --------------------------------------------------

    public SkiProject getPinnedProject() {

        for (SkiProject p : projects) {
            if (p.isPinned()) {
                return p;
            }
        }

        // Safety fallback
        if (projects.isEmpty()) {
            SkiProject p = new SkiProject("project 1");
            p.setPinned(true);

            p.addFile("frontend.ski", "");
            p.addFile("backend.ski", "");

            projects.add(p);
            save();

            return p;
        }

        SkiProject p = projects.get(0);
        p.setPinned(true);

        save();

        return p;
    }

    // --------------------------------------------------
    // Pin a project
    // --------------------------------------------------

    public void pinProject(SkiProject selected) {

        for (SkiProject p : projects) {
            p.setPinned(p == selected);
        }

        save();
    }

    // --------------------------------------------------
    // Create a new project
    // --------------------------------------------------

    public SkiProject createProject(String name) {

        SkiProject p = new SkiProject(name);

        p.addFile("frontend.ski", "");
        p.addFile("backend.ski", "");

        projects.add(p);

        save();

        return p;
    }

    // --------------------------------------------------
    // Save all projects
    // --------------------------------------------------

    public void save() {

        try {

            JSONArray projectsArray = new JSONArray();

            for (SkiProject project : projects) {

                JSONObject projectObject = new JSONObject();

                projectObject.put(
                        "name",
                        project.getName()
                );

                projectObject.put(
                        "pinned",
                        project.isPinned()
                );

                JSONArray filesArray = new JSONArray();

                for (SkiFile file : project.getFiles()) {

                    JSONObject fileObject = new JSONObject();

                    fileObject.put(
                            "name",
                            file.getName()
                    );

                    fileObject.put(
                            "content",
                            file.getContent()
                    );

                    filesArray.put(fileObject);
                }

                projectObject.put(
                        "files",
                        filesArray
                );

                projectsArray.put(projectObject);
            }

            prefs.edit()
                    .putString(
                            "data",
                            projectsArray.toString()
                    )
                    .apply();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // --------------------------------------------------
    // Load projects
    // --------------------------------------------------

    private void load() {

        String raw = prefs.getString(
                "data",
                null
        );

        if (raw == null || raw.isEmpty()) {
            return;
        }

        try {

            JSONArray projectsArray = new JSONArray(raw);

            for (int i = 0; i < projectsArray.length(); i++) {

                JSONObject projectObject =
                        projectsArray.getJSONObject(i);

                String projectName =
                        projectObject.getString("name");

                SkiProject project =
                        new SkiProject(projectName);

                project.setPinned(
                        projectObject.optBoolean(
                                "pinned",
                                false
                        )
                );

                JSONArray filesArray =
                        projectObject.optJSONArray("files");

                if (filesArray != null) {

                    for (int j = 0; j < filesArray.length(); j++) {

                        JSONObject fileObject =
                                filesArray.getJSONObject(j);

                        String fileName =
                                fileObject.getString("name");

                        String content =
                                fileObject.optString(
                                        "content",
                                        ""
                                );

                        project.addFile(
                                fileName,
                                content
                        );
                    }
                }

                projects.add(project);
            }

        } catch (Exception e) {

            e.printStackTrace();

            projects.clear();
        }
    }
}