package com.securefromscratch.busybee.auth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler =
                new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();

        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();

        http
            .requiresChannel(channel -> channel.anyRequest().requiresSecure())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/register.js", "/helpers.js", "/welcome.css",
                                "/gencsrftoken", "/register",
                                "/*.webp", "/*.png", "/*.jpg", "/*.ico")
                .permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/main/main.html", true)
                .permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(repo)
                .csrfTokenRequestHandler(requestHandler)
            )
            .addFilterAfter((request, response, chain) -> {
                org.springframework.security.web.csrf.CsrfToken csrfToken =
                        (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(
                                org.springframework.security.web.csrf.CsrfToken.class.getName());
                if (csrfToken != null) {
                    csrfToken.getToken();
                }
                chain.doFilter(request, response);
            }, org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class);

        return http.build();
    }
}