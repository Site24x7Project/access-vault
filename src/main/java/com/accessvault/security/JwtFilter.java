package com.accessvault.security;

import com.accessvault.model.enums.AuditActionType;
import com.accessvault.repository.UserRepository; // ✅ add
import com.accessvault.service.AuditLogService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER");

    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository; // ✅ add

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("userIp", request.getRemoteAddr());

            // ✅ BYPASS public + infra paths
            String uri = request.getRequestURI();
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                    || uri.startsWith("/api/auth/")
                    || uri.startsWith("/error")
                    || uri.startsWith("/v3/api-docs")
                    || uri.startsWith("/swagger-ui")
                    || uri.startsWith("/actuator")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            // ✅ No hard-403 on missing/malformed header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            try {
                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    MDC.put("username", username);

                    // ✅ real DB existence check
                    if (userRepository.existsByUsername(username)) {
                        var authToken = new UsernamePasswordAuthenticationToken(
                                username, null, List.of(new SimpleGrantedAuthority(role)));
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        if (!uri.startsWith("/api/vault")) {
                            AUDIT_LOGGER.info("Successful authentication for {}", username);
                            auditLogService.logAction(
                                    AuditActionType.LOGIN, username, role, "Authenticated: " + uri);
                        }
                    } else {
                        AUDIT_LOGGER.warn("Valid token for non-existent user: {}", username);
                        auditLogService.logAction(
                                AuditActionType.UNAUTHORIZED_ACCESS, username, "GHOST_USER",
                                "Valid token but user doesn't exist");
                        response.sendError(HttpStatus.FORBIDDEN.value(), "Invalid credentials");
                        return;
                    }
                } else {
                    AUDIT_LOGGER.warn("Invalid token detected");
                    auditLogService.logAction(
                            AuditActionType.UNAUTHORIZED_ACCESS, "SYSTEM", "INVALID_TOKEN",
                            "Token validation failed");
                    response.sendError(HttpStatus.FORBIDDEN.value(), "Invalid token");
                    return;
                }
            } catch (JwtException | IllegalArgumentException e) {
                AUDIT_LOGGER.error("Token parsing error: {}", e.getMessage());
                auditLogService.logAction(
                        AuditActionType.UNAUTHORIZED_ACCESS, "SYSTEM", "TOKEN_ERROR",
                        "Token error: " + e.getMessage());
                response.sendError(HttpStatus.FORBIDDEN.value(), "Token error");
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
