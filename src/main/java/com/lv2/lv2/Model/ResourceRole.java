package com.lv2.lv2.Model;

public enum ResourceRole {
    DB_READER,
    DB_WRITER,
    FILE_UPLOADER,
    FILE_DOWNLOADER,
    REPORT_VIEWER;

    public boolean canAccess(ResourceRole required) {
        return this == required; // за RSR обично exact match
    }
}
