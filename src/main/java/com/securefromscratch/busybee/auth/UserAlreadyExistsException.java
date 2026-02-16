package com.securefromscratch.busybee.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) 
public class UserAlreadyExistsException extends RuntimeException {
    private final String username;

    public UserAlreadyExistsException(String username) {
        super("User already exists: " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
