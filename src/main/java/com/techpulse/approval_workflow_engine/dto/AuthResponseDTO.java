package com.techpulse.approval_workflow_engine.dto;


import com.techpulse.approval_workflow_engine.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String tokenType;
    private Role role;

    public static AuthResponseDTO
    of(String token, Role role) {
        return new AuthResponseDTO(token, "Bearer", role);
    }
}