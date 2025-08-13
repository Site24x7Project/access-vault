package com.accessvault.service;

import com.accessvault.model.AuditLog;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.show-sql=false",
        "jwt.secret=ThisIsATestSecretKeyForJwtAtLeast32Chars!",
        "app.export.path=${java.io.tmpdir}"
})
class LogExportServiceIntegrationTest {

    @Autowired LogExportService logExportService;
    @Autowired AuditLogRepository auditLogRepository;

    @Test
    void export_creates_csv_and_logs_audit() throws Exception {
        auditLogRepository.save(AuditLog.builder()
                .username("u").role("ADMIN").action(AuditActionType.LOGIN).target("t").build());

        // Before export, record tmp dir listing
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        long beforeCount = tmp.listFiles((dir, name) -> name.startsWith("audit_") && name.endsWith(".csv")).length;

        logExportService.exportAuditLogs();

        File[] after = tmp.listFiles((dir, name) -> name.startsWith("audit_") && name.endsWith(".csv"));
        assertNotNull(after);
        assertTrue(after.length >= beforeCount + 1);

        // Optionally check one newest file is non-empty
        Path newest = Files.list(tmp.toPath())
                .filter(p -> p.getFileName().toString().startsWith("audit_") && p.getFileName().toString().endsWith(".csv"))
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .orElseThrow();
        assertTrue(Files.size(newest) > 0);
    }
}
