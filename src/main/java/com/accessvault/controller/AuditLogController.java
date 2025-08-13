package com.accessvault.controller;

import com.accessvault.model.AuditLog;
import com.accessvault.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<AuditLog> getAllLogs() {
        
        return auditLogRepository.findAll();
    }
}
