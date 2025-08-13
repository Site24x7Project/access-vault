package com.accessvault.controller;

import com.accessvault.service.LogExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class ExportController {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER");
    private final LogExportService logExportService;

    @GetMapping("/export-now")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> exportNow() {
        AUDIT_LOGGER.info("Manual export triggered by admin");
        
        try {
            logExportService.exportAuditLogs();
            AUDIT_LOGGER.info("Manual export completed successfully");
            return ResponseEntity.ok("Audit logs exported to CSV");
        } catch (IOException e) {
            AUDIT_LOGGER.error("Manual export failed: {}", e.getMessage());
            return ResponseEntity.status(500).body("Export failed: " + e.getMessage());
        }
    }
}