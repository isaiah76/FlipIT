package com.flipit.models;

public class User {
    private int id;
    private String username;
    private String email;
    private String role;
    private boolean active;

    public User(int id, String username, String email, String role, boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
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
}
