package com.accessvault.repository;

import com.accessvault.model.VaultSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VaultSecretRepository extends JpaRepository<VaultSecret, Long> {
    List<VaultSecret> findByOwnerUsername(String username);

    Optional<VaultSecret> findByIdAndOwnerUsername(Long id, String username);

    boolean existsByKeyNameAndOwnerUsername(String keyName, String username);
}
