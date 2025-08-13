package com.accessvault.security;

import com.accessvault.repository.UserRepository;
import com.accessvault.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

class JwtFilterTest {

    @Test
    void missing_header_allows_through() throws ServletException, IOException {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        UserRepository userRepository = mock(UserRepository.class);
        JwtFilter filter = new JwtFilter(jwtUtil, auditLogService, userRepository);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getRequestURI()).thenReturn("/api/vault/all"); // not bypass path
        when(req.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verify(res, never()).sendError(anyInt(), anyString());
    }

    @Test
    void invalid_token_returns_403_and_does_not_call_chain() throws ServletException, IOException {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        UserRepository userRepository = mock(UserRepository.class);
        JwtFilter filter = new JwtFilter(jwtUtil, auditLogService, userRepository);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getRequestURI()).thenReturn("/api/vault/all");
        when(req.getHeader("Authorization")).thenReturn("Bearer abc");
        when(jwtUtil.isTokenValid("abc")).thenReturn(false);

        filter.doFilterInternal(req, res, chain);

        verify(res, times(1)).sendError(eq(403), anyString());
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void valid_token_existing_user_sets_auth_and_calls_chain() throws ServletException, IOException {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        UserRepository userRepository = mock(UserRepository.class);
        JwtFilter filter = new JwtFilter(jwtUtil, auditLogService, userRepository);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getRequestURI()).thenReturn("/api/test/dev");
        when(req.getHeader("Authorization")).thenReturn("Bearer good");
        when(jwtUtil.isTokenValid("good")).thenReturn(true);
        when(jwtUtil.extractUsername("good")).thenReturn("devtest3");
        when(jwtUtil.extractRole("good")).thenReturn("DEV");
        when(userRepository.existsByUsername("devtest3")).thenReturn(true);

        filter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verify(res, never()).sendError(anyInt(), anyString());
    }
}
