package com.accessvault.controller;

import com.accessvault.dto.SecretRequest;
import com.accessvault.dto.SecretResponse;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.service.AuditLogService;
import com.accessvault.service.VaultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VaultController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(VaultControllerTest.TestMocks.class)
class VaultControllerTest {

    @TestConfiguration
    static class TestMocks {
        @Bean VaultService vaultService() { return mock(VaultService.class); }
        @Bean AuditLogService auditLogService() { return mock(AuditLogService.class); }
    }

    @Autowired MockMvc mvc;
    @Autowired VaultService vaultService;
    @Autowired AuditLogService auditLogService;

    @MockBean
    private com.accessvault.security.JwtFilter jwtFilter; // prevent real filter

    @Test
    @WithMockUser(username = "devtest3", authorities = "DEV")
    void addSecret_ok_audited() throws Exception {
        when(vaultService.addSecret(any(SecretRequest.class))).thenReturn("Secret added successfully");

        mvc.perform(post("/api/vault/add")
                .principal(() -> "devtest3")  // <— IMPORTANT
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"keyName":"stripe_api","secretValue":"sk_live_123456"}
                        """))
           .andExpect(status().isOk())
           .andExpect(content().string("Secret added successfully"));

        verify(auditLogService, times(1))
                .logAction(eq(AuditActionType.ADD_SECRET), eq("devtest3"), eq("DEV"), eq("stripe_api"));
    }

    @Test
    @WithMockUser(username = "devtest3", authorities = "DEV")
    void mySecrets_masked_and_audited() throws Exception {
        when(vaultService.getMySecretsMasked()).thenReturn(
                List.of(new SecretResponse(1L, "k", "****1234", "2025-01-01 00:00:00"))
        );

        mvc.perform(get("/api/vault/my-secrets")
                .principal(() -> "devtest3"))  // <— IMPORTANT
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].keyName").value("k"))
           .andExpect(jsonPath("$[0].maskedSecret").value("****1234"));

        verify(auditLogService, times(1))
                .logAction(eq(AuditActionType.VIEW_SECRETS), eq("devtest3"), eq("DEV"), eq("Viewed masked secrets"));
    }

    @Test
    @WithMockUser(username = "devtest3", authorities = "DEV")
    void deleteSecret_ok_audited() throws Exception {
        when(vaultService.deleteSecret(15L)).thenReturn("Secret deleted successfully");

        mvc.perform(delete("/api/vault/delete/15")
                .principal(() -> "devtest3"))  // <— IMPORTANT
           .andExpect(status().isOk())
           .andExpect(content().string("Secret deleted successfully"));

        verify(auditLogService, times(1))
                .logAction(eq(AuditActionType.DELETE_SECRET), eq("devtest3"), eq("DEV"), eq("Secret ID: 15"));
    }
}
