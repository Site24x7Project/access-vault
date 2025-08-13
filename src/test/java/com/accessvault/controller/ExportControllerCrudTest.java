package com.accessvault.controller;

import com.accessvault.service.LogExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExportControllerCrudTest {

    @Autowired MockMvc mvc;

    @MockBean
    private com.accessvault.security.JwtFilter jwtFilter;

    @MockBean
    private com.accessvault.service.AuditLogService auditLogService; // advice dependency

    @MockBean
    LogExportService logExportService;

    @Test
    void exportNow_success_returns200() throws Exception {
        doNothing().when(logExportService).exportAuditLogs();

        mvc.perform(get("/api/logs/export-now"))
           .andExpect(status().isOk())
           .andExpect(content().string("Audit logs exported to CSV"));
    }

    @Test
    void exportNow_failure_returns500_withMessage() throws Exception {
        doThrow(new IOException("Disk full")).when(logExportService).exportAuditLogs();

        mvc.perform(get("/api/logs/export-now"))
           .andExpect(status().isInternalServerError())
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Export failed: Disk full")));
    }
}
