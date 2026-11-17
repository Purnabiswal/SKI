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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ApkBuildsFragment extends Fragment {

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
                R.layout.fragment_apk_builds,
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
                        R.id.apkBuildContainer
                );

        emptyText =
                view.findViewById(
                        R.id.emptyApkText
                );

        refreshBuilds();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden) {
            refreshBuilds();
        }
    }

    public void refreshBuilds() {

        if (buildContainer == null ||
                buildManager == null) {
            return;
        }

        buildContainer.removeAllViews();

        List<BuildRecord> builds =
                buildManager.getApkBuilds();

        if (builds.isEmpty()) {

            emptyText.setVisibility(View.VISIBLE);

            return;
        }

        emptyText.setVisibility(View.GONE);

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
                new LinearLayout(requireContext());

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

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                20
        );

        card.setLayoutParams(cardParams);

        TextView title =
                new TextView(requireContext());

        title.setText(
                "📱 " + build.getFileName()
        );

        title.setTextColor(
                0xFFFFFFFF
        );

        title.setTextSize(17);

        title.setPadding(
                0,
                0,
                0,
                10
        );

        card.addView(title);

        TextView info =
                new TextView(requireContext());

        String date =
                DateFormat.getDateTimeInstance()
                        .format(
                                new Date(
                                        build.getCreatedAt()
                                )
                        );

        File file =
                new File(build.getFilePath());

        String size =
                formatFileSize(
                        file.exists()
                                ? file.length()
                                : 0
                );

        info.setText(
                build.getProjectName()
                        + "\nVersion "
                        + build.getVersion()
                        + "\n"
                        + date
                        + "\n"
                        + size
        );

        info.setTextColor(
                0xFFBBBBBB
        );

        info.setTextSize(13);

        card.addView(info);

        LinearLayout buttons =
                new LinearLayout(
                        requireContext()
                );

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        buttons.setPadding(
                0,
                18,
                0,
                0
        );

        Button install =
                new Button(requireContext());

        install.setText("Install");

        install.setOnClickListener(
                v -> installApk(build)
        );

        Button share =
                new Button(requireContext());

        share.setText("Share");

        share.setOnClickListener(
                v -> shareFile(build)
        );

        Button menu =
                new Button(requireContext());

        menu.setText("⋮");

        menu.setOnClickListener(
                v -> showBuildMenu(build)
        );

        buttons.addView(
                install,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        buttons.addView(
                share,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        buttons.addView(
                menu,
                new LinearLayout.LayoutParams(
                        60,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        card.addView(buttons);

        return card;
    }

    private void installApk(BuildRecord build) {

        File apk = new File(build.getFilePath());

        if (!apk.exists()) {

            Toast.makeText(
                    requireContext(),
                    "APK file no longer exists",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            // Android 8.0+
            if (android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.O) {

                android.content.pm.PackageManager pm =
                        requireContext().getPackageManager();

                boolean allowed =
                        pm.canRequestPackageInstalls();

                if (!allowed) {

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Allow APK installation")
                            .setMessage(
                                    "SKI needs permission to install APK files " +
                                            "generated by the SKI builder."
                            )
                            .setPositiveButton(
                                    "Open Settings",
                                    (dialog, which) -> {

                                        Intent settingsIntent =
                                                new Intent(
                                                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                        Uri.parse(
                                                                "package:" +
                                                                        requireContext()
                                                                                .getPackageName()
                                                        )
                                                );

                                        startActivity(settingsIntent);
                                    }
                            )
                            .setNegativeButton(
                                    "Cancel",
                                    null
                            )
                            .show();

                    return;
                }
            }

            Uri uri =
                    FileProvider.getUriForFile(
                            requireContext(),
                            requireContext()
                                    .getPackageName()
                                    + ".fileprovider",
                            apk
                    );

            Intent intent =
                    new Intent(Intent.ACTION_VIEW);

            intent.setDataAndType(
                    uri,
                    "application/vnd.android.package-archive"
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    requireContext(),
                    "Unable to open APK installer: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void shareFile(BuildRecord build) {

        File file =
                new File(build.getFilePath());

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
                "application/vnd.android.package-archive"
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
                        "Share APK"
                )
        );
    }

    private void showBuildMenu(
            BuildRecord build
    ) {

        String[] items = {
                "Install",
                "Share",
                "Delete"
        };

        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle(build.getFileName())
                .setItems(
                        items,
                        (dialog, which) -> {

                            if (which == 0) {
                                installApk(build);
                            }

                            else if (which == 1) {
                                shareFile(build);
                            }

                            else {

                                new AlertDialog.Builder(
                                        requireContext()
                                )
                                        .setTitle("Delete build?")
                                        .setMessage(
                                                "This will remove the generated APK."
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