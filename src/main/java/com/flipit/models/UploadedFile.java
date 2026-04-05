package com.flipit.models;

import java.sql.Timestamp;

public class UploadedFile {
    private int id;
    private int userId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private Timestamp uploadedAt;

    public UploadedFile(int id, int userId, String fileName, String fileType, long fileSize, Timestamp uploadedAt) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }
}