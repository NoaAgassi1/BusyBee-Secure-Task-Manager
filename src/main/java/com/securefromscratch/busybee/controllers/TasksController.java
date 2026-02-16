package com.securefromscratch.busybee.controllers;

import com.securefromscratch.busybee.auth.TaskAuthorization;
import com.securefromscratch.busybee.safety.SafeDescription;
import com.securefromscratch.busybee.storage.Task;
import com.securefromscratch.busybee.storage.TaskNotFoundException;
import com.securefromscratch.busybee.storage.TasksStorage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "null")
public class TasksController {

private static final Logger LOGGER = LoggerFactory.getLogger(TasksController.class);

    @Autowired
    private TasksStorage m_tasks;
    @Autowired
    private TaskAuthorization taskAuthorization;

    @PreAuthorize("permitAll()")
    @PostFilter("@taskAuthorization.userAllowedToViewTask(filterObject, authentication)")
    @GetMapping("/tasks")
    public Collection<TaskOut> getTasks(@AuthenticationPrincipal UserDetails user1) {

        List<Task> allTasks = m_tasks.getAll();
        Transformer<Task, TaskOut> transformer = t -> TaskOut.fromTask((Task) t);
        return CollectionUtils.collect(allTasks, transformer);
    }

    public record MarkAsDoneRequest(UUID taskid) {}
    public record MarkAsDoneResponse(boolean success) {}

    public record CreateTaskResponse(UUID taskid) {}
    public record CreateTaskRequest(
            @NotBlank
            @Size(max = 80)
            @Pattern(regexp = "^[^<>\\n\\r]*$", message = "Name must not contain HTML tags or newlines")
            String name,

            SafeDescription desc, 
            java.time.LocalDate dueDate,
            java.time.LocalTime dueTime,

            @NotNull
            @Size(min = 1)
            List<@NotBlank String> responsibilityOf
    ) {
        
        @AssertTrue(message = "dueTime cannot be provided without dueDate")
        public boolean isDueTimeRequiresDueDate() {
            System.out.println("VALIDATION HIT: dueDate=" + dueDate + ", dueTime=" + dueTime);
            return dueTime == null || dueDate != null;
        }


        @AssertTrue(message = "Due date and time must be in the future")
        public boolean isDateInFuture() {
            if (dueDate == null) return true;
            if (dueTime != null) {
                return java.time.LocalDateTime.of(dueDate, dueTime)
                        .isAfter(java.time.LocalDateTime.now());
            }
            return !dueDate.isBefore(java.time.LocalDate.now());
        }

    }

    @PostMapping("/done")
    @PreAuthorize("@taskAuthorization.isTaskCreator(#request.taskid, authentication)")
    public ResponseEntity<MarkAsDoneResponse> markTaskDone(
            @Valid @RequestBody MarkAsDoneRequest request, 
            Authentication authentication) throws TaskNotFoundException, IOException { 
        
        String username = authentication.getName().trim().toLowerCase(java.util.Locale.ROOT);
        m_tasks.markDone(request.taskid());            
        LOGGER.info("Task marked as DONE. TaskID: {}, User: {}", request.taskid(), username);            
        return ResponseEntity.ok(new MarkAsDoneResponse(true));
    }

    @PostMapping("/create")
    @PreAuthorize("@taskAuthorization.isAuthorizedToCreate(authentication)")
    public ResponseEntity<CreateTaskResponse> create( @Valid @RequestBody CreateTaskRequest request, Authentication authentication) throws IOException {
        String username = authentication.getName().trim().toLowerCase(java.util.Locale.ROOT);
        String name = request.name().trim();
        String nameKey = name.toLowerCase(java.util.Locale.ROOT);
        boolean nameExists = m_tasks.getAll().stream().anyMatch(t -> t.name() != null
                        && t.name().trim().toLowerCase(java.util.Locale.ROOT).equals(nameKey));
        if (nameExists) {
            LOGGER.warn("Task creation failed: Name already exists. Name: '{}', User: {}", name, username);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        String desc = (request.desc() == null) ? "" : request.desc().getValue();
        if (!taskAuthorization.validateResponsibilityOf(request.responsibilityOf())) {
            LOGGER.warn("Task creation failed: Invalid participants provided. User: {}", username);
            return ResponseEntity.badRequest().build();
        }
        String[] participants = request.responsibilityOf().stream()
            .map(s -> s == null ? "" : s.trim().toLowerCase(java.util.Locale.ROOT))
            .toArray(String[]::new);
        UUID id;
        if (request.dueDate() != null && request.dueTime() != null) {
            id = m_tasks.add(name, desc, request.dueDate(), request.dueTime(), username, participants);
        } else if (request.dueDate() != null) {
            id = m_tasks.add(name, desc, request.dueDate(), username, participants);
        } else {
            id = m_tasks.add(name, desc, username, participants);
        }
        LOGGER.info("New task created successfully. TaskID: {}, Name: '{}', User: {}", id, name, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateTaskResponse(id));
    }

}
