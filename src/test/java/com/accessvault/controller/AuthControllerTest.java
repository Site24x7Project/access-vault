package com.accessvault.controller;

import com.accessvault.dto.LoginRequest;
import com.accessvault.dto.SignupRequest;
import com.accessvault.model.Role;
import com.accessvault.model.User;
import com.accessvault.security.JwtUtil;
import com.accessvault.service.AuditLogService;
import com.accessvault.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mvc;

    @MockBean
    private com.accessvault.security.JwtFilter jwtFilter; // <— ADD THIS

    @MockBean UserService userService;
    @MockBean JwtUtil jwtUtil;
    @MockBean AuditLogService auditLogService;

    @Test
    void register_success_returns200_and_audits() throws Exception {
        doNothing().when(userService).registerUser(any(SignupRequest.class));
        doNothing().when(auditLogService).logAction(any(), any(), any(), any());

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"newuser","password":"Pass@123","role":"ADMIN"}
                        """))
           .andExpect(status().isOk())
           .andExpect(content().string("User registered successfully"));

        verify(userService, times(1)).registerUser(any(SignupRequest.class));
        verify(auditLogService, times(1)).logAction(any(), eq("newuser"), eq("N/A"), contains("registration"));
    }

    @Test
    void register_duplicate_returns400_and_no_db_audit_call() throws Exception {
        doThrow(new RuntimeException("Username already exists"))
                .when(userService).registerUser(any(SignupRequest.class));

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"dup","password":"x","role":"DEV"}
                        """))
           .andExpect(status().isBadRequest())
           .andExpect(content().string("Username already exists"));

        verify(auditLogService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void login_success_returnsJwt_and_audits() throws Exception {
        User user = User.builder().id(1L).username("u").password("enc").role(Role.ADMIN).build();
        when(userService.authenticateUser(any(LoginRequest.class))).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(eq("u"), eq("ADMIN"))).thenReturn("token-abc");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"u","password":"p"}
                        """))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.token", is("token-abc")))
           .andExpect(jsonPath("$.username", is("u")))
           .andExpect(jsonPath("$.role", is("ADMIN")));

        verify(auditLogService, times(1)).logAction(any(), eq("u"), eq("ADMIN"), contains("authentication"));
    }

    @Test
    void login_badPassword_returns401_and_auditsUnauthorized() throws Exception {
        when(userService.authenticateUser(any(LoginRequest.class))).thenReturn(Optional.empty());

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"u","password":"wrong"}
                        """))
           .andExpect(status().isUnauthorized())
           .andExpect(content().string("Invalid credentials"));

        verify(auditLogService, times(1))
                .logAction(any(), eq("u"), eq("UNKNOWN"), contains("Failed login"));
    }
}
