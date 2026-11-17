package com.app.ski.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.ski.MainActivity;
import com.app.ski.R;
import com.app.ski.core.LocalServer;
import com.app.ski.core.ProjectManager;
import com.app.ski.core.SkiCompiler;
import com.app.ski.model.SkiFile;
import com.app.ski.model.SkiProject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CodeFragment extends Fragment {

    private Spinner spinner;
    private EditText editor;
    private WebView preview;

    private TextView codeTab;
    private TextView previewTab;
    private TextView title;

    private FloatingActionButton run;

    private ProjectManager manager;
    private SkiProject project;
    private SkiFile currentFile;

    private LocalServer server;
    private int serverPort = -1;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_code,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            Bundle savedInstanceState
    ) {

        manager =
                ((MainActivity) requireActivity())
                        .getProjectManager();

        spinner =
                view.findViewById(
                        R.id.fileSpinner
                );

        editor =
                view.findViewById(
                        R.id.codeEditor
                );

        preview =
                view.findViewById(
                        R.id.preview
                );

        codeTab =
                view.findViewById(
                        R.id.codeTab
                );

        previewTab =
                view.findViewById(
                        R.id.previewTab
                );

        title =
                view.findViewById(
                        R.id.projectTitle
                );

        run =
                view.findViewById(
                        R.id.runFab
                );

        view.findViewById(
                R.id.menuButton
        ).setOnClickListener(
                this::menu
        );

        codeTab.setOnClickListener(
                v -> showCode()
        );

        previewTab.setOnClickListener(
                v -> showPreview()
        );

        run.setOnClickListener(
                v -> toggleRun()
        );

        preview.setWebViewClient(
                new WebViewClient()
        );

        preview.getSettings()
                .setJavaScriptEnabled(true);

        refreshProject();
    }

    public void refreshProject() {

        if (getView() == null
                || manager == null) {
            return;
        }

        /*
         * Code always shows the pinned project.
         */
        project =
                manager.getPinnedProject();

        title.setText(
                project.getName()
        );

        buildSpinner();
    }

    private void buildSpinner() {

        if (project == null) {
            return;
        }

        saveCurrent();

        List<String> names =
                new ArrayList<>();

        for (SkiFile file :
                project.getFiles()) {

            names.add(
                    file.getName()
            );
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                );

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position >= 0
                                && position < names.size()) {

                            switchFile(
                                    names.get(position)
                            );
                        }
                    }
                }
        );

        if (names.isEmpty()) {
            currentFile = null;
            editor.setText("");
            return;
        }

        String wanted =
                currentFile == null
                        ? "frontend.ski"
                        : currentFile.getName();

        int position =
                names.indexOf(wanted);

        if (position < 0) {
            position = 0;
        }

        spinner.setSelection(
                position
        );

        /*
         * Explicitly load the selected file.
         */
        switchFile(
                names.get(position)
        );
    }

    private void switchFile(
            String name
    ) {

        /*
         * Save the file we are leaving.
         */
        saveCurrent();

        if (project == null) {
            return;
        }

        SkiFile file =
                project.getFile(name);

        if (file == null) {
            return;
        }

        currentFile = file;

        /*
         * IMPORTANT:
         *
         * The editor receives ONLY this file's
         * stored content.
         */
        editor.setText(
                file.getContent()
        );

        editor.setSelection(
                editor.length()
        );
    }

    private void saveCurrent() {

        if (currentFile != null
                && editor != null) {

            currentFile.setContent(
                    editor.getText()
                            .toString()
            );
        }
    }

    private void showCode() {

        saveCurrent();

        editor.setVisibility(
                View.VISIBLE
        );

        preview.setVisibility(
                View.GONE
        );

        codeTab.setTextColor(
                0xFFFFFFFF
        );

        previewTab.setTextColor(
                0xFF697389
        );
    }

    private void showPreview() {

        saveCurrent();

        editor.setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);

        codeTab.setTextColor(0xFF697389);
        previewTab.setTextColor(0xFFFFFFFF);

        if (project == null) {
            return;
        }

        /*
         * Preview must run through LocalServer.
         *
         * This is important because backend.ski is handled
         * by LocalServer at /api/*.
         */
        if (server == null || !server.isRunning()) {

            manager.save();

            server = new LocalServer(project);

            try {

                server.start();

                serverPort = server.getPort();

                run.setImageResource(
                        android.R.drawable.ic_media_pause
                );

            } catch (Exception e) {

                server = null;
                serverPort = -1;

                run.setImageResource(
                        android.R.drawable.ic_media_play
                );

                Toast.makeText(
                        requireContext(),
                        "Failed to start SKI server: "
                                + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                return;
            }
        }

        /*
         * Compile frontend.
         */
        String html;

        try {

            html = SkiCompiler.compileFrontend(project);

        } catch (Exception e) {

            Toast.makeText(
                    requireContext(),
                    "Frontend compile error: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * IMPORTANT:
         *
         * Use the actual LocalServer URL as the
         * WebView base URL.
         *
         * This makes:
         *
         * fetch("/api/...")
         *
         * resolve to:
         *
         * http://127.0.0.1:<port>/api/...
         */
        String baseUrl = server.getUrl() + "/";

        preview.loadDataWithBaseURL(
                baseUrl,
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    /*
     * RUN / STOP
     */
    private void toggleRun() {

        saveCurrent();

        if (project == null) {
            Toast.makeText(
                    requireContext(),
                    "No project selected",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * STOP
         */
        if (server != null
                && server.isRunning()) {

            server.stop();

            serverPort = -1;

            run.setImageResource(
                    android.R.drawable.ic_media_play
            );

            Toast.makeText(
                    requireContext(),
                    "SKI server stopped",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * SAVE PROJECT
         */
        manager.save();

        /*
         * START NEW SERVER
         *
         * LocalServer now requires the project.
         */
        server =
                new LocalServer(
                        project
                );

        try {

            server.start();

            serverPort =
                    server.getPort();

            run.setImageResource(
                    android.R.drawable.ic_media_pause
            );

            Toast.makeText(
                    requireContext(),
                    "SKI server running: "
                            + server.getUrl(),
                    Toast.LENGTH_LONG
            ).show();

            /*
             * Show compiled preview.
             */
            showPreview();

            /*
             * Open Chrome / external browser.
             */
            openBrowser();

        } catch (Exception e) {

            server = null;
            serverPort = -1;

            run.setImageResource(
                    android.R.drawable.ic_media_play
            );

            Toast.makeText(
                    requireContext(),
                    "Failed to start server: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openBrowser() {

        if (server == null
                || !server.isRunning()) {

            return;
        }

        String url =
                server.getUrl();

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    requireContext(),
                    "No browser available",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /*
     * 3-DOT MENU
     */
    private void menu(View anchor) {

        PopupMenu popup =
                new PopupMenu(
                        requireContext(),
                        anchor
                );

        popup.getMenuInflater()
                .inflate(
                        R.menu.code_menu,
                        popup.getMenu()
                );

        popup.setOnMenuItemClickListener(
                item -> {

                    int id =
                            item.getItemId();

                    if (id == R.id.menu_clear) {

                        editor.setText("");

                        saveCurrent();

                        return true;
                    }

                    if (id == R.id.menu_paste) {

                        paste();

                        return true;
                    }

                    if (id == R.id.menu_add_file) {

                        addFile();

                        return true;
                    }

                    if (id == R.id.menu_save) {

                        saveCurrent();

                        manager.save();

                        Toast.makeText(
                                requireContext(),
                                "Project saved",
                                Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }

                    if (id == R.id.menu_switch_project) {

                        switchProject();

                        return true;
                    }

                    if (id == R.id.menu_external) {

                        if (server != null
                                && server.isRunning()) {

                            openBrowser();

                        } else {

                            toggleRun();
                        }

                        return true;
                    }

                    return false;
                }
        );

        popup.show();
    }

    private void paste() {

        ClipboardManager clipboard =
                (ClipboardManager)
                        requireContext()
                                .getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                );

        if (clipboard == null
                || !clipboard.hasPrimaryClip()) {

            return;
        }

        ClipData data =
                clipboard.getPrimaryClip();

        if (data == null
                || data.getItemCount() == 0) {

            return;
        }

        CharSequence text =
                data.getItemAt(0)
                        .coerceToText(
                                requireContext()
                        );

        editor.setText(
                text
        );

        saveCurrent();
    }

    private void addFile() {

        EditText input =
                new EditText(
                        requireContext()
                );

        input.setSingleLine(true);

        input.setHint(
                ".env / config.json / data.json"
        );

        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle(
                        "Add file"
                )
                .setView(input)
                .setPositiveButton(
                        "Add",
                        (dialog, which) -> {

                            String name =
                                    input.getText()
                                            .toString()
                                            .trim();

                            if (name.isEmpty()) {
                                return;
                            }

                            if (project.getFile(name)
                                    != null) {

                                Toast.makeText(
                                        requireContext(),
                                        "File already exists",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            saveCurrent();

                            project.addFile(
                                    name,
                                    ""
                            );

                            manager.save();

                            currentFile = null;

                            buildSpinner();

                            switchFile(name);
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void switchProject() {

        List<SkiProject> projects =
                manager.getProjects();

        if (projects.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "No projects available",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String[] names =
                new String[projects.size()];

        for (int i = 0;
             i < projects.size();
             i++) {

            SkiProject p =
                    projects.get(i);

            names[i] =
                    (p.isPinned()
                            ? "📌 "
                            : "")
                            + p.getName();
        }

        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle(
                        "Switch project"
                )
                .setItems(
                        names,
                        (dialog, position) -> {

                            saveCurrent();

                            SkiProject selected =
                                    projects.get(
                                            position
                                    );

                            /*
                             * Make selected project
                             * the pinned project.
                             */
                            manager.pinProject(
                                    selected
                            );

                            project =
                                    selected;

                            currentFile = null;

                            refreshProject();
                        }
                )
                .show();
    }

    @Override
    public void onDestroyView() {

        /*
         * Save current editor contents.
         */
        saveCurrent();

        /*
         * Don't kill the server merely because
         * the fragment view is recreated.
         */
        super.onDestroyView();
    }
}