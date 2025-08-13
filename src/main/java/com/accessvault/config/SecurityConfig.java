package com.accessvault.config;

import com.accessvault.model.enums.AuditActionType;
import com.accessvault.security.JwtFilter;
import com.accessvault.security.RateLimiterFilter;
import com.accessvault.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // ✅ ADD THIS
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // ✅ ADD THIS
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AuditLogService auditLogService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(new RateLimiterFilter(), UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/test/admin").hasAuthority("ADMIN")
                .requestMatchers("/api/test/dev").hasAuthority("DEV")
                .requestMatchers("/api/test/ops").hasAuthority("OPS")
                .requestMatchers("/api/logs/export-now").hasAuthority("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    auditLogService.logAction(
                        AuditActionType.UNAUTHORIZED_ACCESS,
                        auth != null ? auth.getName() : "SYSTEM",
                        auth != null ? auth.getAuthorities().toString() : "NO_ROLE",
                        "Role violation on: " + request.getRequestURI()
                    );
                    response.sendError(HttpStatus.FORBIDDEN.value(), "Access denied");
                })
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
