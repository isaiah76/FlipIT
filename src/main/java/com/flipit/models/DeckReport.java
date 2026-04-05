package com.flipit.models;

import java.sql.Timestamp;

public class DeckReport {
    private int id;
    private int deckId;
    private int reporterId;
    private String reason;
    private String status;
    private Timestamp createdAt;

    private String deckTitle;
    private String reporterName;

    public DeckReport(int id, int deckId, int reporterId, String reason, String status, Timestamp createdAt) {
        this.id = id;
        this.deckId = deckId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getDeckId() {
        return deckId;
    }

    public int getReporterId() {
        return reporterId;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getDeckTitle() {
        return deckTitle;
    }

    public void setDeckTitle(String deckTitle) {
        this.deckTitle = deckTitle;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }
}