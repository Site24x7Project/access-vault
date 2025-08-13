package com.accessvault.dto;

import com.accessvault.model.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String username;
    private String password;
    private Role role;
}
