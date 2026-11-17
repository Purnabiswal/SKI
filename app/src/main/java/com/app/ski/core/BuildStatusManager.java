package com.app.ski.core;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class BuildStatusManager {

    public interface Listener {

        void onBuildStatusChanged(
                BuildStatus status
        );
    }

    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final List<Listener> listeners =
            new ArrayList<>();

    private BuildStatus currentStatus =
            new BuildStatus();

    public synchronized void addListener(
            Listener listener
    ) {

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }

        listener.onBuildStatusChanged(
                currentStatus
        );
    }

    public synchronized void removeListener(
            Listener listener
    ) {

        listeners.remove(listener);
    }

    public synchronized BuildStatus
    getCurrentStatus() {

        return currentStatus;
    }

    public void start(
            String projectName,
            String target
    ) {

        BuildStatus status =
                new BuildStatus();

        status.setState(
                BuildStatus.State.BUILDING
        );

        status.setProjectName(
                projectName
        );

        status.setTarget(
                target
        );

        status.setProgress(0);

        status.setStage(
                "Preparing build..."
        );

        setStatus(status);
    }

    public void update(
            int progress,
            String stage
    ) {

        synchronized (this) {

            currentStatus.setProgress(
                    progress
            );

            currentStatus.setStage(
                    stage
            );

            currentStatus.setState(
                    BuildStatus.State.BUILDING
            );
        }

        notifyListeners();
    }

    public void success(
            String stage
    ) {

        synchronized (this) {

            currentStatus.setProgress(100);

            currentStatus.setStage(stage);

            currentStatus.setState(
                    BuildStatus.State.SUCCESS
            );
        }

        notifyListeners();
    }

    public void failed(
            String error
    ) {

        synchronized (this) {

            currentStatus.setState(
                    BuildStatus.State.FAILED
            );

            currentStatus.setError(error);

            currentStatus.setStage(
                    "Build failed"
            );
        }

        notifyListeners();
    }

    public void cancel() {

        synchronized (this) {

            currentStatus.setState(
                    BuildStatus.State.CANCELLED
            );

            currentStatus.setStage(
                    "Build cancelled"
            );
        }

        notifyListeners();
    }

    private synchronized void setStatus(
            BuildStatus status
    ) {

        currentStatus = status;

        notifyListeners();
    }

    private void notifyListeners() {

        final BuildStatus snapshot;

        synchronized (this) {
            snapshot = currentStatus;
        }

        mainHandler.post(() -> {

            List<Listener> copy;

            synchronized (
                    BuildStatusManager.this
            ) {

                copy =
                        new ArrayList<>(
                                listeners
                        );
            }

            for (Listener listener : copy) {

                listener.onBuildStatusChanged(
                        snapshot
                );
            }
        });
    }
}