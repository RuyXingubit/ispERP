package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.LoginRequest;
import br.dev.xb.isperp.dto.LoginResponse;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.util.JwtUtil;
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
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) {
            log.warn("Falha de autenticação para usuário: {}", request.getUsername());
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