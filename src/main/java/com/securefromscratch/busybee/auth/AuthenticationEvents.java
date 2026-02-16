package com.securefromscratch.busybee.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationEvents.class);

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        var auth = event.getAuthentication();
        LOGGER.info("LOGIN success user={} roles={}", auth.getName(), auth.getAuthorities());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String user = (event.getAuthentication() != null) ? event.getAuthentication().getName() : "<null>";
        String reason = event.getException().getClass().getSimpleName();
        LOGGER.warn("LOGIN failed user={} reason={}", user, reason);
    }
}
