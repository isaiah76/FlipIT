package com.flipit.models;


public class User {
    private int id;
    private String username;
    private String role;
    private boolean active;
    private java.sql.Timestamp createdAt;

    public User(int id, String username, String role, boolean active) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isModerator() {
        return "moderator".equals(role) || "admin".equals(role);
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getInitials() {
        if (username == null || username.isEmpty()) return "?";
        return username.substring(0, Math.min(1, username.length())).toUpperCase();
    }
}