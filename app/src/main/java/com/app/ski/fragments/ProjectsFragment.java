package com.app.ski.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.ski.MainActivity;
import com.app.ski.R;
import com.app.ski.core.BuildManager;
import com.app.ski.core.BuildStatusManager;
import com.app.ski.core.ProjectManager;
import com.app.ski.model.BuildRecord;
import com.app.ski.model.SkiProject;

import java.util.ArrayList;
import java.util.List;
import com.app.ski.core.BuildStatus;
import com.app.ski.core.ApkBuildEngine;

public class ProjectsFragment extends Fragment {

    private ProjectManager manager;
    private BuildManager buildManager;

    private LinearLayout projectContainer;

    private TextView projectsMenuButton;
    private TextView createProjectButton;

    private final List<SkiProject> shown =
            new ArrayList<>();

    private BuildStatusManager buildStatusManager;

    private LinearLayout buildStatusCard;
    private ProgressBar buildCircularProgress;
    private TextView buildProgressCenter;

    private TextView buildStatusTitle;
    private TextView buildProgressText;
    private TextView buildStageText;
    private final BuildStatusManager.Listener
            buildStatusListener =
            this::updateBuildStatus;


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_projects,
                container,
                false
        );
    }


    @Override
    public void onViewCreated(
            @NonNull View view,
            Bundle savedInstanceState
    ) {

        MainActivity activity =
                (MainActivity) requireActivity();

        manager =
                activity.getProjectManager();

        buildManager =
                activity.getBuildManager();


        projectContainer =
                view.findViewById(
                        R.id.projectContainer
                );


        projectsMenuButton =
                view.findViewById(
                        R.id.projectsMenuButton
                );


        createProjectButton =
                view.findViewById(
                        R.id.createProjectButton
                );

        buildStatusManager =
                ((MainActivity) requireActivity())
                        .getBuildStatusManager();

        buildStatusCard =
                view.findViewById(
                        R.id.buildStatusCard
                );

        buildCircularProgress =
                view.findViewById(
                        R.id.buildCircularProgress
                );

        buildProgressCenter =
                view.findViewById(
                        R.id.buildProgressCenter
                );



        buildStatusTitle =
                view.findViewById(
                        R.id.buildStatusTitle
                );

        buildProgressText =
                view.findViewById(
                        R.id.buildProgressText
                );

        buildStageText =
                view.findViewById(
                        R.id.buildStageText
                );


        buildStatusManager.addListener(
                buildStatusListener
        );


        /*
         * CREATE PROJECT FAB
         */
        createProjectButton.setOnClickListener(
                v -> showCreateProjectDialog()
        );


        /*
         * TOP RIGHT PROJECTS MENU
         */
        projectsMenuButton.setOnClickListener(
                v -> showProjectsMenu()
        );


        refreshProjects();
    }

    private void updateBuildStatus(BuildStatus status) {

        if (buildStatusCard == null) {
            return;
        }

        if (status.getState() == BuildStatus.State.IDLE) {

            buildStatusCard.setVisibility(View.GONE);
            return;
        }

        buildStatusCard.setVisibility(View.VISIBLE);


        // -----------------------------
        // Progress
        // -----------------------------

        int progress = status.getProgress();

        if (buildCircularProgress != null) {
            buildCircularProgress.setProgress(progress);
        }

        if (buildProgressText != null) {
            buildProgressText.setText(progress + "%");
        }

        if (buildProgressCenter != null) {
            buildProgressCenter.setText(progress + "%");
        }


        // -----------------------------
        // Current stage
        // -----------------------------

        if (buildStageText != null) {
            buildStageText.setText(
                    status.getStage()
            );
        }


        // -----------------------------
        // Title
        // -----------------------------

        if (status.getState()
                == BuildStatus.State.BUILDING) {

            buildStatusTitle.setText(
                    status.getProjectName()
                            + " • "
                            + status.getTarget()
            );
        }


        // -----------------------------
        // SUCCESS
        // -----------------------------

        else if (status.getState()
                == BuildStatus.State.SUCCESS) {

            buildStatusTitle.setText(
                    "✓ Build complete"
            );

            if (buildProgressText != null) {
                buildProgressText.setText("100%");
            }

            if (buildProgressCenter != null) {
                buildProgressCenter.setText("100%");
            }
        }


        // -----------------------------
        // FAILED
        // -----------------------------

        else if (status.getState()
                == BuildStatus.State.FAILED) {

            buildStatusTitle.setText(
                    "✕ Build failed"
            );

            buildStageText.setText(
                    status.getError()
            );
        }


        // -----------------------------
        // CANCELLED
        // -----------------------------

        else if (status.getState()
                == BuildStatus.State.CANCELLED) {

            buildStatusTitle.setText(
                    "Build cancelled"
            );
        }
    }

    public void refreshProjects() {

        if (projectContainer == null ||
                manager == null) {

            return;
        }


        projectContainer.removeAllViews();

        shown.clear();


        shown.addAll(
                manager.getProjects()
        );


        for (SkiProject project : shown) {

            projectContainer.addView(
                    createProjectCard(project)
            );
        }
    }


    /*
     * ============================================================
     * TOP PROJECTS MENU
     * ============================================================
     */

    private void showProjectsMenu() {

        List<String> items =
                new ArrayList<>();


        items.add("📱 Android APK Builds");
        items.add("🖥 Desktop Builds");


        String[] choices =
                items.toArray(
                        new String[0]
                );


        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle("Builds")
                .setItems(
                        choices,
                        (dialog, which) -> {

                            String choice =
                                    choices[which];


                            switch (choice) {

                                case "📱 Android APK Builds":

                                    ((MainActivity)
                                            requireActivity())
                                            .openApkBuilds();

                                    break;


                                case "🖥 Desktop Builds":

                                    ((MainActivity)
                                            requireActivity())
                                            .openDesktopBuilds();

                                    break;
                            }
                        }
                )
                .show();
    }


    /*
     * ============================================================
     * CREATE PROJECT
     * ============================================================
     */

    private void showCreateProjectDialog() {

        final EditText input =
                new EditText(requireContext());


        input.setHint("Project name");

        input.setSingleLine(true);


        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle("Create New Project")
                .setMessage("Choose a project name")
                .setView(input)

                .setPositiveButton(
                        "Create",
                        (dialog, which) -> {

                            String name =
                                    input.getText()
                                            .toString()
                                            .trim();


                            if (name.isEmpty()) {

                                Toast.makeText(
                                        requireContext(),
                                        "Enter a project name",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }


                            manager.createProject(name);

                            refreshProjects();
                        }
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }


    /*
     * ============================================================
     * PROJECT CARD
     * ============================================================
     */

    private View createProjectCard(
            SkiProject project
    ) {

        LinearLayout card =
                new LinearLayout(
                        requireContext()
                );


        card.setOrientation(
                LinearLayout.VERTICAL
        );


        card.setPadding(
                22,
                20,
                14,
                20
        );


        card.setBackgroundResource(
                R.drawable.bg_card
        );


        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );


        cardParams.setMargins(
                0,
                0,
                0,
                18
        );


        card.setLayoutParams(
                cardParams
        );


        /*
         * HEADER
         */

        LinearLayout header =
                new LinearLayout(
                        requireContext()
                );


        header.setOrientation(
                LinearLayout.HORIZONTAL
        );


        header.setGravity(
                android.view.Gravity.CENTER_VERTICAL
        );


        /*
         * PROJECT NAME
         */

        TextView name =
                new TextView(
                        requireContext()
                );


        name.setText(
                (project.isPinned()
                        ? "📌 "
                        : "")
                        + project.getName()
        );


        name.setTextColor(
                0xFFFFFFFF
        );


        name.setTextSize(18);


        name.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );


        header.addView(
                name,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );


        /*
         * PROJECT CARD 3-DOT MENU
         */

        TextView menu =
                new TextView(
                        requireContext()
                );


        menu.setText("⋮");

        menu.setTextColor(
                0xFFFFFFFF
        );

        menu.setTextSize(26);

        menu.setGravity(
                android.view.Gravity.CENTER
        );


        menu.setClickable(true);

        menu.setFocusable(true);


        menu.setOnClickListener(
                v -> showProjectMenu(project)
        );


        header.addView(
                menu,
                new LinearLayout.LayoutParams(
                        48,
                        48
                )
        );


        card.addView(header);


        /*
         * PROJECT TYPE
         */

        TextView type =
                new TextView(
                        requireContext()
                );


        String projectType =
                getProjectType(project);


        type.setText(
                projectType
        );


        type.setTextColor(
                0xFF999999
        );


        type.setTextSize(13);


        type.setPadding(
                0,
                4,
                0,
                14
        );


        card.addView(type);


        /*
         * OPEN PROJECT
         */

        Button open =
                new Button(
                        requireContext()
                );


        open.setText(
                "Open Project"
        );


        open.setOnClickListener(
                v -> {

                    manager.pinProject(
                            project
                    );


                    ((MainActivity)
                            requireActivity())
                            .openCode();
                }
        );


        card.addView(open);


        return card;
    }


    /*
     * ============================================================
     * PROJECT CARD MENU
     * ============================================================
     */

    private void showProjectMenu(
            SkiProject project
    ) {

        List<String> items =
                new ArrayList<>();


        items.add("Open Project");

        items.add("Build Android APK");


        if (supportsDesktop(project)) {

            items.add(
                    "Build Windows Desktop"
            );
        }


        items.add("Delete Project");


        String[] choices =
                items.toArray(
                        new String[0]
                );


        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle(
                        project.getName()
                )

                .setItems(
                        choices,
                        (dialog, which) -> {

                            String choice =
                                    choices[which];


                            switch (choice) {

                                case "Open Project":

                                    manager.pinProject(
                                            project
                                    );


                                    ((MainActivity)
                                            requireActivity())
                                            .openCode();

                                    break;


                                case "Build Android APK":

                                    buildApk(project);

                                    break;


                                case "Build Windows Desktop":

                                    buildDesktop(project);

                                    break;


                                case "Delete Project":

                                    confirmDelete(
                                            project
                                    );

                                    break;
                            }
                        }
                )

                .show();
    }


    /*
     * ============================================================
     * BUILD APK
     * ============================================================
     */

    private void buildApk(
            SkiProject project
    ) {

        if (project == null) {
            Toast.makeText(
                    requireContext(),
                    "No project selected.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        MainActivity activity =
                (MainActivity) requireActivity();

        /*
         * ------------------------------------------------------------
         * Calculate project fingerprint
         * ------------------------------------------------------------
         */

        String fingerprint =
                buildManager.calculateProjectFingerprint(
                        project
                );

        /*
         * ------------------------------------------------------------
         * Check duplicate build
         * ------------------------------------------------------------
         */

        BuildRecord duplicate =
                buildManager.findDuplicate(
                        project.getName(),
                        BuildRecord.TYPE_APK,
                        fingerprint
                );

        if (duplicate != null) {

            new AlertDialog.Builder(
                    requireContext()
            )
                    .setTitle(
                            "APK already exists"
                    )
                    .setMessage(
                            "This exact project version has already been built."
                    )
                    .setPositiveButton(
                            "Open Builds",
                            (dialog, which) ->
                                    activity.openApkBuilds()
                    )
                    .setNegativeButton(
                            "Cancel",
                            null
                    )
                    .show();

            return;
        }

        /*
         * ------------------------------------------------------------
         * Start build status
         * ------------------------------------------------------------
         */

        buildStatusManager.start(
                project.getName(),
                "ANDROID_WEBVIEW"
        );

        /*
         * ------------------------------------------------------------
         * Create REAL APK build engine
         * ------------------------------------------------------------
         */

        ApkBuildEngine engine =
                new ApkBuildEngine(
                        requireContext(),
                        buildManager,
                        buildStatusManager
                );

        /*
         * ------------------------------------------------------------
         * Start actual build
         * ------------------------------------------------------------
         */

        engine.build(
                project,
                new ApkBuildEngine.Listener() {

                    @Override
                    public void onProgress(
                            int progress,
                            String stage
                    ) {

                        // BuildStatusManager already handles UI updates.
                    }

                    @Override
                    public void onSuccess(
                            java.io.File apkFile,
                            BuildRecord build
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        requireActivity()
                                .runOnUiThread(() -> {

                                    new AlertDialog.Builder(
                                            requireContext()
                                    )
                                            .setTitle(
                                                    "APK Build Complete"
                                            )
                                            .setMessage(
                                                    "APK created successfully.\n\n"
                                                            + apkFile.getAbsolutePath()
                                            )
                                            .setPositiveButton(
                                                    "Open Builds",
                                                    (dialog, which) ->
                                                            activity.openApkBuilds()
                                            )
                                            .setNegativeButton(
                                                    "Close",
                                                    null
                                            )
                                            .show();
                                });
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        requireActivity()
                                .runOnUiThread(() -> {

                                    new AlertDialog.Builder(
                                            requireContext()
                                    )
                                            .setTitle(
                                                    "APK Build Failed"
                                            )
                                            .setMessage(
                                                    error == null ||
                                                            error.trim().isEmpty()
                                                            ? "Unknown build error."
                                                            : error
                                            )
                                            .setPositiveButton(
                                                    "OK",
                                                    null
                                            )
                                            .show();
                                });
                    }

                    @Override
                    public void onCancelled() {

                        if (!isAdded()) {
                            return;
                        }

                        requireActivity()
                                .runOnUiThread(() -> {

                                    Toast.makeText(
                                            requireContext(),
                                            "APK build cancelled.",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                    }
                }
        );

        Toast.makeText(
                requireContext(),
                "APK build started...",
                Toast.LENGTH_SHORT
        ).show();
    }


    /*
     * ============================================================
     * BUILD DESKTOP
     * ============================================================
     */

    private void buildDesktop(
            SkiProject project
    ) {

        String fingerprint =
                buildManager
                        .calculateProjectFingerprint(
                                project
                        );


        BuildRecord duplicate =
                buildManager.findDuplicate(
                        project.getName(),
                        BuildRecord.TYPE_DESKTOP,
                        fingerprint
                );


        if (duplicate != null) {

            new AlertDialog.Builder(
                    requireContext()
            )
                    .setTitle(
                            "Desktop build already exists"
                    )

                    .setMessage(
                            "This exact project version has already been built."
                    )

                    .setPositiveButton(
                            "Open Builds",
                            (d, w) ->
                                    ((MainActivity)
                                            requireActivity())
                                            .openDesktopBuilds()
                    )

                    .setNegativeButton(
                            "Cancel",
                            null
                    )

                    .show();

            return;
        }


        Toast.makeText(
                requireContext(),
                "Desktop build started...",
                Toast.LENGTH_SHORT
        ).show();


        /*
         * Actual desktop generation
         * remains connected to your
         * DesktopBuildEngine.
         */
    }


    /*
     * ============================================================
     * DELETE
     * ============================================================
     */

    private void confirmDelete(
            SkiProject project
    ) {

        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle("Delete project?")

                .setMessage(
                        "Delete " +
                                project.getName() +
                                "?"
                )

                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            Toast.makeText(
                                    requireContext(),
                                    "Delete functionality will be connected here.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }


    /*
     * ============================================================
     * PROJECT TYPE
     * ============================================================
     */

    private String getProjectType(
            SkiProject project
    ) {

        try {

            java.lang.reflect.Method method =
                    project.getClass()
                            .getMethod(
                                    "getType"
                            );


            Object result =
                    method.invoke(project);


            if (result != null) {

                return result.toString();
            }

        } catch (Exception ignored) {
        }


        return "WEB / CROSS-PLATFORM";
    }


    private boolean supportsDesktop(
            SkiProject project
    ) {

        String type =
                getProjectType(project);


        return !type.equalsIgnoreCase(
                "NATIVE_ANDROID"
        );
    }


    @Override
    public void onDestroyView() {

        if (buildStatusManager != null) {

            buildStatusManager.removeListener(
                    buildStatusListener
            );
        }

        super.onDestroyView();
    }

}