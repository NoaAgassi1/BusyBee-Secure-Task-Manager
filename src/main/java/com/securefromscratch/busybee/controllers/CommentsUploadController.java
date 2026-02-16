package com.securefromscratch.busybee.controllers;

import com.securefromscratch.busybee.safety.CommentText;
import com.securefromscratch.busybee.storage.FileStorage;
import com.securefromscratch.busybee.storage.Task;
import com.securefromscratch.busybee.storage.TaskNotFoundException;
import com.securefromscratch.busybee.storage.TasksStorage;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
@RestController
@PreAuthorize("denyAll()")
public class CommentsUploadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommentsUploadController.class);

    @Autowired
    private TasksStorage m_tasks;

    public record AddCommentFields(@NotNull UUID taskid, Optional<UUID> commentid, @NotNull CommentText text) {
        public AddCommentFields {
            commentid = commentid == null ? Optional.empty() : commentid;
        }
    }
    public record CreatedCommentId(UUID commentid) {}
    
    @PostMapping(value = "/comment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@taskAuthorization.isAllowedToComment(#commentFields.taskid, authentication)")
    public ResponseEntity<CreatedCommentId> addComment(
            @RequestPart("commentFields") AddCommentFields commentFields,
            @RequestPart(value = "file", required = false) Optional<MultipartFile> optFile,
            Authentication authentication
    ) throws IOException {
        
        String username = authentication.getName(); 
        
        LOGGER.info("Request to add comment. TaskID: {}, User: {}", commentFields.taskid(), username);
        
        Optional<Task> t = m_tasks.find(commentFields.taskid());
        if (t.isEmpty()) {
            LOGGER.warn("Task not found: {}", commentFields.taskid());
            throw new TaskNotFoundException(commentFields.taskid());
        }

        if (optFile.isEmpty() || optFile.get().isEmpty()) {
            LOGGER.info("Adding text-only comment. User: {}", username);
            UUID newComment = m_tasks.addComment(t.get(), commentFields.text().get(), username, commentFields.commentid());
            return ResponseEntity.ok(new CreatedCommentId(newComment));
        }

        LOGGER.info("Processing file upload for TaskID: {}", commentFields.taskid());

        FileStorage fileStorage = new FileStorage(Path.of("uploads").toAbsolutePath().normalize());
        
     
        String storedFilename = fileStorage.store(optFile.get());
        
        LOGGER.info("File uploaded successfully. User: {}, Size: {}, Type: {}, StorageID: {}", 
                username,                      
                optFile.get().getSize(),        
                optFile.get().getContentType(), 
                storedFilename);                


        FileStorage.FileType filetype = FileStorage.identifyTypeFromStoredName(storedFilename);

        Optional<String> imageFilename = (filetype == FileStorage.FileType.IMAGE)
                ? Optional.of(storedFilename) : Optional.empty();
        Optional<String> attachFilename = (filetype != FileStorage.FileType.IMAGE)
                ? Optional.of(storedFilename) : Optional.empty();


        UUID newComment = m_tasks.addComment(
                t.get(),
                commentFields.text().get(),
                imageFilename,
                attachFilename,
                username,
                commentFields.commentid()
        );
        return ResponseEntity.ok(new CreatedCommentId(newComment));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleValidationExceptions(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body("Input validation failed: " + ex.getMessage());
    }
}