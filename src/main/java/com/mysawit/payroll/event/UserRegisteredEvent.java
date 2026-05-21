package com.mysawit.payroll.event;

public class UserRegisteredEvent {
    private String userId;
    private String email;
    private String role;
    private String username;

    public UserRegisteredEvent() {}

    public UserRegisteredEvent(String userId, String email, String role, String username) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.username = username;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
