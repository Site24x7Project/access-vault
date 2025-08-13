 package com.accessvault.model;

import com.accessvault.model.enums.AuditActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String role;

    @Enumerated(EnumType.STRING)
    private AuditActionType action;

    private String target;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
