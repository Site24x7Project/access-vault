package com.accessvault.service;

import com.accessvault.model.AuditLog;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.FileWriter;  // ← Add this line

@Service
@RequiredArgsConstructor
public class LogExportService {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER");
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    
    @Value("${app.export.path}") 
    private String exportPath;

    @Scheduled(cron = "0 0 0 * * ?")
    public void exportAuditLogs() throws IOException {
        String currentUser = getCurrentUsername();
        Path fullPath = prepareExportPath(currentUser);
        
        try {
            ensureExportDirectoryExists(fullPath);
            List<AuditLog> logs = auditLogRepository.findAll();
            exportLogsToFile(fullPath, logs, currentUser);
        } catch (Exception e) {
            AUDIT_LOGGER.error("Export process failed: {}", e.getMessage());
            throw e;
        }
    }

    private String getCurrentUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "SYSTEM";
    }

    private Path prepareExportPath(String username) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "audit_" + timestamp + "_" + username + ".csv";
        return Paths.get(exportPath, filename).toAbsolutePath();
    }

    private void ensureExportDirectoryExists(Path fullPath) throws IOException {
        try {
            Files.createDirectories(fullPath.getParent());
            AUDIT_LOGGER.debug("Export directory prepared at: {}", fullPath.getParent());
        } catch (IOException e) {
            AUDIT_LOGGER.error("Directory creation failed for path {}: {}", fullPath.getParent(), e.getMessage());
            throw e;
        }
    }

    private void exportLogsToFile(Path fullPath, List<AuditLog> logs, String currentUser) throws IOException {
        try (FileWriter writer = new FileWriter(fullPath.toFile())) {
            writer.write("id,username,role,action,timestamp,target\n");
            for (AuditLog log : logs) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s\n",
                    log.getId(), log.getUsername(), log.getRole(),
                    log.getAction(), log.getTimestamp(), log.getTarget()));
            }
            
            AUDIT_LOGGER.info("Successfully exported {} logs to {}", logs.size(), fullPath);
            auditLogService.logAction(
                AuditActionType.EXPORT_LOGS,
                currentUser,
                "ADMIN",
                "Exported audit logs"
            );
        } catch (IOException e) {
            AUDIT_LOGGER.error("File write operation failed: {}", e.getMessage());
            throw e;
        }
    }
}