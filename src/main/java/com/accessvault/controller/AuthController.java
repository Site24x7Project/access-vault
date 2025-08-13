package com.accessvault.controller;

import com.accessvault.dto.JwtResponse;
import com.accessvault.dto.LoginRequest;
import com.accessvault.dto.SignupRequest;
import com.accessvault.model.User;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.security.JwtUtil;
import com.accessvault.service.AuditLogService;
import com.accessvault.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;                     // <-- added
import org.slf4j.LoggerFactory;             // <-- added
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER"); // <-- added

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody SignupRequest request) {
        try {
            userService.registerUser(request);

            // registration logging with target (DB + file via AuditLogService)
            auditLogService.logAction(
                AuditActionType.REGISTER,
                request.getUsername(),
                "N/A",
                "User registration completed"
            );

            return ResponseEntity.ok("User registered successfully");
        } catch (RuntimeException e) {
            // <-- added: file-only warning for duplicate/failure path
            AUDIT_LOGGER.warn("REGISTER failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.authenticateUser(request);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

            // login logging with target (DB + file via AuditLogService)
            auditLogService.logAction(
                AuditActionType.LOGIN,
                user.getUsername(),
                user.getRole().name(),
                "User authentication successful"
            );

            return ResponseEntity.ok(new JwtResponse(token, user.getUsername(), user.getRole().name()));
        } else {
            auditLogService.logAction(
                AuditActionType.UNAUTHORIZED_ACCESS,
                request.getUsername(),
                "UNKNOWN",
                "Failed login attempt"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
