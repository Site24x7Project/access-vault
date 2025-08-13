package com.accessvault.controller;

import com.accessvault.dto.SecretRequest;
import com.accessvault.dto.SecretResponse;
import com.accessvault.model.enums.AuditActionType;
import com.accessvault.service.AuditLogService;
import com.accessvault.service.VaultService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;
    private final AuditLogService auditLogService;

@PostMapping("/add")
public ResponseEntity<String> addSecret(@RequestBody SecretRequest request, Principal principal) {
    try {
        // Add validation before service call
                if (request.getKeyName() == null || request.getKeyName().isBlank() || 
            request.getSecretValue() == null || request.getSecretValue().isBlank()) {
            return ResponseEntity.badRequest().body("Key name and secret value cannot be empty");
        }
        
        vaultService.addSecret(request); // Let service handle duplicate key check
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();
        
        auditLogService.logAction(
            AuditActionType.ADD_SECRET,
            principal.getName(),
            role,
            request.getKeyName()
        );
        return ResponseEntity.ok("Secret added successfully");
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage()); // No logging
    }
}
    @GetMapping("/my-secrets")  // Add this new endpoint
public ResponseEntity<List<SecretResponse>> getMySecretsMasked(Principal principal) {
    List<SecretResponse> secrets = vaultService.getMySecretsMasked(); // You'll need this service method
    
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String role = auth.getAuthorities().iterator().next().getAuthority();
    
    auditLogService.logAction(
        AuditActionType.VIEW_SECRETS,
        principal.getName(),
        role,
        "Viewed masked secrets"
    );
    return ResponseEntity.ok(secrets);
}
    @GetMapping("/all")
    public ResponseEntity<List<SecretResponse>> getSecrets(Principal principal) {
        List<SecretResponse> secrets = vaultService.getMySecrets();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();
        
        auditLogService.logAction(
                AuditActionType.VIEW_SECRETS,
                principal.getName(),
                role, // Now includes actual role
                null
        );
        return ResponseEntity.ok(secrets);
    }

@DeleteMapping("/delete/{id}")
public ResponseEntity<String> deleteSecret(@PathVariable Long id, Principal principal) {
    try {
        vaultService.deleteSecret(id);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();
        
        auditLogService.logAction(
                AuditActionType.DELETE_SECRET,
                principal.getName(),
                role,
                "Secret ID: " + id
        );
        return ResponseEntity.ok("Secret deleted successfully");
    } catch (SecurityException e) {
        // Add this block for failed deletions
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();
        
        auditLogService.logAction(
                AuditActionType.UNAUTHORIZED_ACCESS,  // Reuse existing enum
                principal.getName(),
                role,
                "Failed to delete non-existent Secret ID: " + id
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}
}