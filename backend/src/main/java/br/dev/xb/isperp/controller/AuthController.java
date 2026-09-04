package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.AuthApi;
import br.dev.xb.isperp.api.dto.LoginRequest;
import br.dev.xb.isperp.api.dto.LoginResponse;
import br.dev.xb.isperp.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class AuthController implements AuthApi {
    
    private final AuthService authService;
    
    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        log.info("Tentativa de login para usuário: {}", loginRequest.getUsername());
        
        try {
            LoginResponse response = authService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Erro durante o login: {}", e.getMessage());
            LoginResponse errorResponse = new LoginResponse()
                    .success(false)
                    .message("Credenciais inválidas");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}