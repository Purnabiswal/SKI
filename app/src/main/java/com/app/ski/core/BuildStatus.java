package com.app.ski.core;

public class BuildStatus {

    public enum State {
        IDLE,
        BUILDING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    private State state = State.IDLE;

    private int progress = 0;

    private String stage = "Waiting";

    private String projectName = "";

    private String target = "";

    private String error = "";

    public State getState() {
        return state;
    }

    public int getProgress() {
        return progress;
    }

    public String getStage() {
        return stage;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getTarget() {
        return target;
    }

    public String getError() {
        return error;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setProgress(int progress) {

        this.progress =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progress
                        )
                );
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public void setProjectName(
            String projectName
    ) {
        this.projectName = projectName;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setError(String error) {
        this.error = error;
    }
}