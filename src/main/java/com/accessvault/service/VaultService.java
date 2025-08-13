package com.accessvault.service;

import com.accessvault.dto.SecretRequest;
import com.accessvault.dto.SecretResponse;
import com.accessvault.model.VaultSecret;
import com.accessvault.repository.VaultSecretRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final VaultSecretRepository vaultSecretRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public String addSecret(SecretRequest request) {
        String username = getCurrentUsername();

        // Early validation for empty fields (prevents audit logging)
        if (request.getKeyName() == null || request.getKeyName().trim().isEmpty() ||
            request.getSecretValue() == null || request.getSecretValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Key name and secret value cannot be empty");
        }

        // Early duplicate check (prevents audit logging)
        if (vaultSecretRepository.existsByKeyNameAndOwnerUsername(request.getKeyName(), username)) {
            throw new IllegalArgumentException("Secret key already exists for this user");
        }

        VaultSecret secret = new VaultSecret();
        secret.setKeyName(request.getKeyName());
        secret.setSecretValue(request.getSecretValue()); // In production, encrypt this
        secret.setOwnerUsername(username);

        vaultSecretRepository.save(secret);
        return "Secret added successfully";
    }

    public List<SecretResponse> getMySecrets() {
        String username = getCurrentUsername();
        List<VaultSecret> secrets = vaultSecretRepository.findByOwnerUsername(username);

        return secrets.stream()
                .map(secret -> new SecretResponse(
                        secret.getId(),
                        secret.getKeyName(),
                        maskSecret(secret.getSecretValue()),
                        secret.getCreatedAt().format(formatter)
                ))
                .collect(Collectors.toList());
    }

    // NEW METHOD: Masked secrets with full security checks
    public List<SecretResponse> getMySecretsMasked() {
        return getMySecrets(); // Reuses existing logic including masking
    }

    public String deleteSecret(Long id) {
        String username = getCurrentUsername();
        Optional<VaultSecret> secretOpt = vaultSecretRepository.findByIdAndOwnerUsername(id, username);

        if (secretOpt.isEmpty()) {
            throw new SecurityException("You are not authorized to delete this secret");
        }

        vaultSecretRepository.deleteById(id);
        return "Secret deleted successfully";
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 4) return "****";
        return "****" + secret.substring(secret.length() - 4);
    }
}