package com.xingubit.isperp.controller;

import com.xingubit.isperp.dto.LoginRequest;
import com.xingubit.isperp.dto.LoginResponse;
import com.xingubit.isperp.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Tentativa de login para usuário: {}", request.getUsername());
        
        try {
            LoginResponse response = authService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Erro durante o login: {}", e.getMessage());
            LoginResponse errorResponse = LoginResponse.builder()
                    .success(false)
                    .message("Credenciais inválidas")
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}