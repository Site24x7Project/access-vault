package com.accessvault.exception;

import com.accessvault.model.enums.AuditActionType;
import com.accessvault.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final Logger ERROR_LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuditLogService auditLogService;
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurity(SecurityException ex) {
        ERROR_LOGGER.warn("Security violation detected: {}", ex.getMessage());
        auditLogService.log(
                AuditActionType.UNAUTHORIZED_ACCESS,
                "SYSTEM",
                "SECURITY_EXCEPTION",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";
        String role = auth != null ? auth.getAuthorities().toString() : "NO_ROLE";

        ERROR_LOGGER.warn("Access denied for user {} ({}): {}", username, role, ex.getMessage());
        auditLogService.log(
                AuditActionType.UNAUTHORIZED_ACCESS,
                username,
                role,
                "Access denied: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegal(IllegalArgumentException ex) {
        ERROR_LOGGER.warn("Validation error occurred: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}