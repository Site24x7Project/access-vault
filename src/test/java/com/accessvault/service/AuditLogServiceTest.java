package com.accessvault.service;

import com.accessvault.model.AuditLog;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    AuditLogRepository repo = mock(AuditLogRepository.class);
    AuditLogService service = new AuditLogService(repo);

    @Test
    void logAction_persists_and_mirrors_to_logger() {
        // We only verify repository interaction; logger side effects are side-effects
        when(repo.saveAndFlush(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.logAction(AuditActionType.LOGIN, "user1", "ADMIN", "Authenticated: /x");

        verify(repo, times(1)).saveAndFlush(any(AuditLog.class));
    }

    @Test
    void logAction_overload_reads_authContext_safely() {
        // Just ensure it delegations to the main method
        when(repo.saveAndFlush(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service.logAction(AuditActionType.VIEW_SECRETS, "target-x");
        verify(repo, times(1)).saveAndFlush(any(AuditLog.class));
    }
}
