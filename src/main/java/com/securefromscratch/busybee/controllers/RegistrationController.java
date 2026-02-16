package com.securefromscratch.busybee.controllers;

import com.securefromscratch.busybee.auth.UserAlreadyExistsException;
import com.securefromscratch.busybee.auth.UsernamePasswordDetailsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@PreAuthorize("permitAll()")
public class RegistrationController {

    private final UsernamePasswordDetailsService detailsService;
    private final PasswordEncoder passwordEncoder;
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);


    public RegistrationController(UsernamePasswordDetailsService detailsService,
                                  PasswordEncoder passwordEncoder) {
        this.detailsService = detailsService;
        this.passwordEncoder = passwordEncoder;
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 1, max = 30) String username,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    public record RegisterResponse(String redirectTo) {}
    public record CsrfTokenOut(String token, String headerName) {}

    @GetMapping("/gencsrftoken")
    public ResponseEntity<CsrfTokenOut> genCsrf(CsrfToken token) {
        LOGGER.debug("CSRF token generated for registration endpoint. headerName={}",
        token.getHeaderName());
        return ResponseEntity.ok(new CsrfTokenOut(token.getToken(), token.getHeaderName()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        LOGGER.info("Registration attempt for username='{}'", req.username());
        String username = req.username().trim();
        String encoded = passwordEncoder.encode(req.password());
        String[] roles = new String[]{"TRIAL"};
        try {
            detailsService.createUser(username, encoded, roles);
            LOGGER.info("User '{}' registered successfully with role TRIAL", username);
            return ResponseEntity.ok(new RegisterResponse("/index.html"));
        } catch (UserAlreadyExistsException ex) {
            LOGGER.warn("Registration failed: username '{}' already exists", username);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Registration failed"));

        }
    }
}
