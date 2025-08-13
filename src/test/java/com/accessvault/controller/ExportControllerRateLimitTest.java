package com.accessvault.controller;

import com.accessvault.security.RateLimiterFilter;
import com.accessvault.service.LogExportService;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExportController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(ExportControllerRateLimitTest.TestConfig.class)
class ExportControllerRateLimitTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean LogExportService logExportService() { return Mockito.mock(LogExportService.class); }
        @Bean RateLimiterFilter rateLimiterFilter() { return new RateLimiterFilter(); }
        @Bean
        SecurityFilterChain chain(HttpSecurity http, RateLimiterFilter rl) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .addFilterBefore(rl, UsernamePasswordAuthenticationFilter.class)
                    .build();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired LogExportService logExportService;

    @MockBean
    private com.accessvault.security.JwtFilter jwtFilter;

    @MockBean
    private com.accessvault.service.AuditLogService auditLogService; // advice dependency

    @Test
    @WithMockUser(authorities = "ADMIN")
    void third_call_within_minute_hits_429() throws Exception {
        doNothing().when(logExportService).exportAuditLogs();

        mvc.perform(get("/api/logs/export-now")).andExpect(status().isOk());
        mvc.perform(get("/api/logs/export-now")).andExpect(status().isOk());
        mvc.perform(get("/api/logs/export-now")).andExpect(status().isTooManyRequests());
    }
}
