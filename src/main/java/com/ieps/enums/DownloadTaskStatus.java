package com.ieps.enums;

public enum DownloadTaskStatus {

    PENDING("pending"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed"),
    EXPIRED("expired");

    private final String value;

    DownloadTaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
