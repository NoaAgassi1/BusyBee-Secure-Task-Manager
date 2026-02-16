package com.securefromscratch.busybee.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.securefromscratch.busybee.storage.TaskNotFoundException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionsHandler.class);

    private static final String BAD_REQUEST = "Bad request";
    private static final String FORBIDDEN = "Forbidden";
    private static final String NOT_FOUND = "Not found";
    private static final String SERVER_ERROR = "Server error";

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<String> handleDenied(Exception ex, HttpServletRequest req) {
        LOGGER.warn("event=request_rejected code=NOT_AUTHORIZED endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(FORBIDDEN);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        LOGGER.debug("event=request_rejected code=NOT_FOUND endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NOT_FOUND);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> resourceNotFound(IOException ex, HttpServletRequest req) {
        LOGGER.warn("event=request_rejected code=NOT_FOUND endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        LOGGER.warn("event=request_rejected code=INPUT_INVALID endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleJsonErrors(HttpMessageNotReadableException ex, HttpServletRequest req) {
        LOGGER.warn("event=request_rejected code=INPUT_INVALID endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), "invalid")
        );

        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField())
                .distinct()
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        LOGGER.warn("event=request_rejected code=INPUT_INVALID endpoint={} fields={}", req.getRequestURI(), fields);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        LOGGER.warn("event=request_rejected code=UPLOAD_REJECT endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipart(MultipartException ex, HttpServletRequest req) {
        LOGGER.warn("event=request_rejected code=UPLOAD_REJECT endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception ex, HttpServletRequest req) {
        LOGGER.error("event=server_error code=SERVER_ERROR endpoint={}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(SERVER_ERROR);
    }
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<String> handleTaskNotFound(TaskNotFoundException ex, HttpServletRequest req) {
        LOGGER.warn("event=resource_missing code=TASK_NOT_FOUND endpoint={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NOT_FOUND);
    }



}
