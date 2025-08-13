package com.accessvault.service;

import com.accessvault.model.AuditLog;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional  // Add this class-level annotation
public class AuditLogService {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER"); // <-- added
    private final AuditLogRepository auditLogRepository;

    // Change method name from log() to logAction()
    public void logAction(AuditActionType action, String username, String role, String target) {
        try {
            AuditLog log = AuditLog.builder()
                    .username(username)
                    .role(role)
                    .action(action)
                    .target(target != null ? target : "N/A")
                    .build();
            auditLogRepository.saveAndFlush(log);  // Changed from save() to saveAndFlush()

            // <-- added: mirror DB audit to JSON/file log
            AUDIT_LOGGER.info("AUDIT action={} user={} role={} target={}",
                    action, username, role, log.getTarget());

        } catch (Exception e) {
            System.err.println("Failed to save audit log: " + e.getMessage());
            // optional: also emit to file log
            AUDIT_LOGGER.error("AUDIT persist failed: {}", e.getMessage());
        }
    }

    // Optional: Keep this overload if needed
    public void logAction(AuditActionType action, String target) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";
        String role = auth != null ?
            auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("UNKNOWN") :
            "SYSTEM";
        logAction(action, username, role, target);
    }

    // Add these methods while keeping the existing logAction() methods
    public void log(AuditActionType action, String username, String role, String target) {
        logAction(action, username, role, target); // Reuse existing implementation
    }

    public void log(AuditActionType action, String target) {
        logAction(action, target); // Reuse existing implementation
    }
}
