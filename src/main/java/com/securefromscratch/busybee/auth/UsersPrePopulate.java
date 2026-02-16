package com.securefromscratch.busybee.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;


@Component
public class UsersPrePopulate {
    private static final SecureRandom s_random = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    @Bean
    CommandLineRunner populateInitialUsers(UsersStorage usersStorage, PasswordEncoder passwordEncoder) {
        return args -> {
            populateUser("Yariv", new String[] {"ADMIN"}, usersStorage, passwordEncoder);
            populateUser("Or",new String[] {"CREATOR"}, usersStorage, passwordEncoder);
            populateUser("Eyal",new String[] {"TRIAL"}, usersStorage, passwordEncoder);
        };
    }

    private void populateUser(String username,String[] roles, UsersStorage usersStorage, PasswordEncoder passwordEncoder) {
        String plainPassword = generatePwd(12);
        String encodedPassword = passwordEncoder.encode(plainPassword);
        UserAccount newAccount = usersStorage.createUser(username, encodedPassword, roles);

        System.out.println("********** User created: **********");
        System.out.println(newAccount.getUsername());
        System.out.println("********** Plain Password: **********");

        //TODO - Remove plain password printing 
        System.out.println(plainPassword);
    }

    private String generatePwd(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(PASSWORD_CHARS.charAt(s_random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }


}

