package com.xingubit.isperp.service;

import com.xingubit.isperp.dto.LoginRequest;
import com.xingubit.isperp.dto.LoginResponse;
import com.xingubit.isperp.entity.User;
import com.xingubit.isperp.repository.UserRepository;
import com.xingubit.isperp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public LoginResponse authenticate(LoginRequest request) {
        log.info("Autenticando usuário: {}", request.getUsername());
        
        // Buscar por email (que é o username no nosso caso)
        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        log.info("Usuário encontrado: {}, senha no banco: {}", user.getEmail(), user.getPassword());
        log.info("Senha fornecida: {}", request.getPassword());
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("Senha confere: {}", passwordMatches);
        
        if (!passwordMatches) {
            throw new RuntimeException("Senha inválida");
        }
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().toString());
        
        return LoginResponse.builder()
                .success(true)
                .message("Login realizado com sucesso")
                .token(token)
                .username(user.getEmail())
                .role(user.getRole().toString())
                .build();
    }
}