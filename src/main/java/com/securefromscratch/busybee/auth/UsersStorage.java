package com.securefromscratch.busybee.auth;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UsersStorage {
    private final Map<String, UserAccount> m_users = new HashMap<>();

   
public Optional<UserAccount> findByUsername(String username) {
    if (username == null) return Optional.empty();
    String key = username.trim().toLowerCase(Locale.ROOT);
    return Optional.ofNullable(m_users.get(key));
}

    public UserAccount createUser(String username, String password, String[] roles) {
        if (username == null) throw new IllegalArgumentException("username is required");
        String trimmed = username.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("username is required");

        String key = trimmed.toLowerCase(Locale.ROOT);
        UserAccount newAccount = new UserAccount(trimmed, password, roles);

        UserAccount existing = m_users.putIfAbsent(key, newAccount);
        if (existing != null) {
            throw new UserAlreadyExistsException(trimmed);
        }
        return newAccount;
    }

}
