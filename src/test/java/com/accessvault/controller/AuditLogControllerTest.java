package com.accessvault.controller;

import com.accessvault.model.AuditLog;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuditLogControllerTest.TestConfig.class)
class AuditLogControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean AuditLogRepository auditLogRepository() { return Mockito.mock(AuditLogRepository.class); }
        @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired AuditLogRepository auditLogRepository;

    @MockBean
    private com.accessvault.security.JwtFilter jwtFilter;

    @MockBean
    private com.accessvault.service.AuditLogService auditLogService; // advice dependency

    @Test
    @WithMockUser(authorities = "ADMIN")
    void admin_can_fetch_logs() throws Exception {
        AuditLog row = AuditLog.builder()
                .id(1L).username("u").role("ADMIN")
                .action(AuditActionType.LOGIN).target("ok")
                .timestamp(LocalDateTime.now()).build();
        when(auditLogRepository.findAll()).thenReturn(List.of(row));

        mvc.perform(get("/api/admin/audit-logs"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].username").value("u"))
           .andExpect(jsonPath("$[0].action").value("LOGIN"));
    }

    @Test
    @WithMockUser(authorities = "DEV")
    void non_admin_forbidden() throws Exception {
        mvc.perform(get("/api/admin/audit-logs"))
           .andExpect(status().isForbidden());
    }
}
