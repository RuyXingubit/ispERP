package com.xingubit.isperp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    
    private boolean success;
    private String message;
    private String token;
    private String username;
    private String role;
}