package com.app.ski.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.app.ski.MainActivity;
import com.app.ski.R;
import com.app.ski.core.BuildManager;
import com.app.ski.model.BuildRecord;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class DesktopBuildsFragment extends Fragment {

    private BuildManager buildManager;

    private LinearLayout buildContainer;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_desktop_builds,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            Bundle savedInstanceState
    ) {

        buildManager =
                ((MainActivity) requireActivity())
                        .getBuildManager();

        buildContainer =
                view.findViewById(
                        R.id.desktopBuildContainer
                );

        emptyText =
                view.findViewById(
                        R.id.emptyDesktopText
                );

        refreshBuilds();
    }

    public void refreshBuilds() {

        if (buildContainer == null ||
                buildManager == null) {
            return;
        }

        buildContainer.removeAllViews();

        List<BuildRecord> builds =
                buildManager.getDesktopBuilds();

        if (builds.isEmpty()) {

            emptyText.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        emptyText.setVisibility(
                View.GONE
        );

        for (BuildRecord build : builds) {

            buildContainer.addView(
                    createBuildCard(build)
            );
        }
    }

    private View createBuildCard(
            BuildRecord build
    ) {

        LinearLayout card =
                new LinearLayout(
                        requireContext()
                );

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                24,
                22,
                24,
                22
        );

        card.setBackgroundResource(
                R.drawable.bg_card
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(
                0,
                0,
                0,
                20
        );

        card.setLayoutParams(params);

        TextView title =
                new TextView(
                        requireContext()
                );

        title.setText(
                "🖥 " + build.getFileName()
        );

        title.setTextColor(
                0xFFFFFFFF
        );

        title.setTextSize(17);

        card.addView(title);

        TextView info =
                new TextView(
                        requireContext()
                );

        String date =
                DateFormat.getDateTimeInstance()
                        .format(
                                new Date(
                                        build.getCreatedAt()
                                )
                        );

        File file =
                new File(
                        build.getFilePath()
                );

        info.setText(
                build.getProjectName()
                        + "\nVersion "
                        + build.getVersion()
                        + "\n"
                        + date
                        + "\n"
                        + formatFileSize(
                        file.exists()
                                ? file.length()
                                : 0
                )
        );

        info.setTextColor(
                0xFFBBBBBB
        );

        info.setTextSize(13);

        info.setPadding(
                0,
                10,
                0,
                10
        );

        card.addView(info);

        LinearLayout buttons =
                new LinearLayout(
                        requireContext()
                );

        Button share =
                new Button(
                        requireContext()
                );

        share.setText("Share");

        share.setOnClickListener(
                v -> shareFile(build)
        );

        Button open =
                new Button(
                        requireContext()
                );

        open.setText("Open");

        open.setOnClickListener(
                v -> openFile(build)
        );

        Button menu =
                new Button(
                        requireContext()
                );

        menu.setText("⋮");

        menu.setOnClickListener(
                v -> showBuildMenu(build)
        );

        buttons.addView(
                open,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        buttons.addView(
                share,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        buttons.addView(
                menu,
                new LinearLayout.LayoutParams(
                        60,
                        -2
                )
        );

        card.addView(buttons);

        return card;
    }

    private void openFile(
            BuildRecord build
    ) {

        File file =
                new File(
                        build.getFilePath()
                );

        if (!file.exists()) {

            Toast.makeText(
                    requireContext(),
                    "Build file not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            Uri uri =
                    FileProvider.getUriForFile(
                            requireContext(),
                            requireContext()
                                    .getPackageName()
                                    + ".fileprovider",
                            file
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW
                    );

            intent.setDataAndType(
                    uri,
                    "application/octet-stream"
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(intent);

        } catch (Exception e) {

            shareFile(build);
        }
    }

    private void shareFile(
            BuildRecord build
    ) {

        File file =
                new File(
                        build.getFilePath()
                );

        if (!file.exists()) {

            Toast.makeText(
                    requireContext(),
                    "Build file not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Uri uri =
                FileProvider.getUriForFile(
                        requireContext(),
                        requireContext()
                                .getPackageName()
                                + ".fileprovider",
                        file
                );

        Intent share =
                new Intent(
                        Intent.ACTION_SEND
                );

        share.setType(
                "application/octet-stream"
        );

        share.putExtra(
                Intent.EXTRA_STREAM,
                uri
        );

        share.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        startActivity(
                Intent.createChooser(
                        share,
                        "Share Desktop Build"
                )
        );
    }

    private void showBuildMenu(
            BuildRecord build
    ) {

        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle(
                        build.getFileName()
                )
                .setItems(
                        new String[]{
                                "Open",
                                "Share",
                                "Delete"
                        },
                        (dialog, which) -> {

                            if (which == 0) {
                                openFile(build);
                            }

                            else if (which == 1) {
                                shareFile(build);
                            }

                            else {

                                new AlertDialog.Builder(
                                        requireContext()
                                )
                                        .setTitle(
                                                "Delete build?"
                                        )
                                        .setMessage(
                                                "Remove this desktop build?"
                                        )
                                        .setPositiveButton(
                                                "Delete",
                                                (d, w) -> {

                                                    buildManager
                                                            .deleteBuild(
                                                                    build
                                                            );

                                                    refreshBuilds();
                                                }
                                        )
                                        .setNegativeButton(
                                                "Cancel",
                                                null
                                        )
                                        .show();
                            }
                        }
                )
                .show();
    }

    private String formatFileSize(
            long bytes
    ) {

        if (bytes <= 0) {
            return "Unknown size";
        }

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        return String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }
}