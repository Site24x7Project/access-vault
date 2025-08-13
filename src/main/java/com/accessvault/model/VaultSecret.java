package com.accessvault.model;

import com.accessvault.security.SecretCryptoConverter; // <-- added
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vault_secrets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"key_name", "owner_username"})
})
public class VaultSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_name", nullable = false)
    private String keyName;

    @Column(name = "secret_value", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = SecretCryptoConverter.class) // <-- added (encrypt/decrypt at rest)
    private String secretValue;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "owner_username", nullable = false)
    private String ownerUsername;
}
