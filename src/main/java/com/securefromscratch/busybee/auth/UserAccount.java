package com.securefromscratch.busybee.auth;

public class UserAccount {
    private String username;
    private String hashedPassword;
    private boolean enabled = true;
    private String[] roles;

    public UserAccount(String username, String hashedPassword, String[] roles) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.roles = roles;
    }

    public UserAccount(String username, String hashedPassword) {
        this(username, hashedPassword, new String[]{"USER"});
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String[] getRoles() {
        return roles;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
