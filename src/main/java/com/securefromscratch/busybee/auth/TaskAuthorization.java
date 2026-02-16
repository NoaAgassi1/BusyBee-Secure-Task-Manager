package com.securefromscratch.busybee.auth;

import com.securefromscratch.busybee.controllers.TaskOut;
import com.securefromscratch.busybee.safety.ImageName;
import com.securefromscratch.busybee.storage.Task;
import com.securefromscratch.busybee.storage.TaskComment;
import com.securefromscratch.busybee.storage.TasksStorage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

// @Component
@Component("taskAuthorization")
public class TaskAuthorization {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L}\\p{N} ]*$");
    private final TasksStorage m_tasks;
    private final UsersStorage m_users;

    private static String normUser(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public TaskAuthorization(TasksStorage tasks, UsersStorage users) {
        this.m_tasks = tasks;
        this.m_users = users;
    }

    public boolean isAuthorizedToCreate(Authentication authentication) {
        String username = normUser(authentication.getName());
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        boolean isAdminOrCreator = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> "ROLE_ADMIN".equals(r) || "ROLE_CREATOR".equals(r));
        if (isAdminOrCreator) return true;

        boolean isTrial = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> "ROLE_TRIAL".equals(r));
        if (!isTrial) return false;
        return m_tasks.getAll().stream()
                .filter(t -> normUser(t.createdBy()).equals(username))
                .noneMatch(t -> !t.done());
    }

    public boolean userAllowedToViewTask(TaskOut t, Authentication authentication) {
        if (isAdmin(authentication)) return true;
        if (t == null) return false;
        String username = normUser(authentication.getName());

        if (t.createdBy() != null && !t.createdBy().isBlank() && normUser(t.createdBy()).equals(username)) {
            return true;
        }
        if (t.responsibilityOf() != null) {
            for (String responsible : t.responsibilityOf()) {
                if (normUser(responsible).equals(username)) return true;
            }
        }
        return false;
    }

    public boolean userAllowedToViewTask(Task t, Authentication authentication) {
        if (isAdmin(authentication)) return true;
        if (t == null) return false;
        String username = normUser(authentication.getName());
        if (normUser(t.createdBy()).equals(username)) return true;
        if (t.responsibilityOf() != null) {
            for (String responsible : t.responsibilityOf()) {
                if (normUser(responsible).equals(username)) return true;
            }
        }
        return false;
    }
    
    public boolean isTaskCreator(UUID taskid, Authentication authentication) {
        if (taskid == null) return false;
        if (isAdmin(authentication)) return true;

        String username = normUser(authentication.getName());
        return m_tasks.find(taskid)
                .map(task -> normUser(task.createdBy()).equals(username))
                .orElse(true); 
    }

    public boolean validateResponsibilityOf(List<String> responsibilityOf) {
        if (responsibilityOf == null || responsibilityOf.isEmpty()) return false;
        for (String raw : responsibilityOf) {
            if (raw == null) return false;
            String name = raw.trim();
            if (name.isEmpty()) return false;

            if (!USERNAME_PATTERN.matcher(name).matches()) return false;

            if (m_users.findByUsername(name).isEmpty()) return false;
        }
        return true;
    }

    public boolean imgIsInOwnedOrAssignedTask(ImageName imgName, Authentication authentication) {
         if (imgName == null) return false;
        String name = imgName.getName();
        if (name == null || name.isBlank()) return false;

        for (Task t : m_tasks.getAll()) {
            if (!userAllowedToViewTask(t, authentication)) continue;

            for (TaskComment c : t.comments()) {
                var opt = c.image();             
                if (opt != null && opt.isPresent() && opt.get().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isAllowedToComment(UUID taskid, Authentication authentication) {
        if (taskid == null) return false;
        return m_tasks.find(taskid)
                .map(task -> userAllowedToViewTask(task, authentication))
                .orElse(false);
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(auth.getAuthority())) return true;
        }
        return false;
    }

    public boolean attachmentIsInOwnedOrAssignedTask(ImageName fileName, Authentication authentication) {
    if (fileName == null) return false;

    String name = fileName.getName();
    if (name == null || name.isBlank()) return false;

    for (Task t : m_tasks.getAll()) {
        if (!userAllowedToViewTask(t, authentication)) continue;

        for (TaskComment c : t.comments()) {
            var opt = c.attachment(); 
            if (opt != null && opt.isPresent() && opt.get().equals(name)) {
                return true;
            }
        }
    }
    return false;
}

}
