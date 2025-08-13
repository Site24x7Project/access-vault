package com.accessvault.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SecretResponse {
    private Long id;
    private String keyName;
    private String maskedSecret;
    private String createdAt;
}
