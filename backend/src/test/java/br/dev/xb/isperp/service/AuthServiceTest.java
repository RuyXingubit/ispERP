package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.LoginRequest;
import br.dev.xb.isperp.api.dto.LoginResponse;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.util.JwtUtil;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Admin Ativo")
                .email("admin@provedor.com.br")
                .password("$2a$10$hashedPassword")
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        inactiveUser = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Ex-Colaborador Desligado")
                .email("ex@provedor.com.br")
                .password("$2a$10$hashedPassword")
                .role(UserRole.TECHNICIAN)
                .active(false)
                .build();
    }

    @Test
    @DisplayName("Regressão: Deve autenticar colaborador ativo com senha correta e gerar JWT")
    void shouldAuthenticateActiveUserSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@provedor.com.br");
        request.setPassword("SenhaSegura123");

        when(userRepository.findByEmail("admin@provedor.com.br")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SenhaSegura123", activeUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken("admin@provedor.com.br", "ADMIN")).thenReturn("jwt-token-valido");

        LoginResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("jwt-token-valido", response.getToken());
        assertEquals("admin@provedor.com.br", response.getUsername());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    @DisplayName("Regressão: Deve rejeitar login com senha incorreta")
    void shouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@provedor.com.br");
        request.setPassword("SenhaErrada");

        when(userRepository.findByEmail("admin@provedor.com.br")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SenhaErrada", activeUser.getPassword())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.authenticate(request));
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Segurança: Deve bloquear login de colaborador inativo (active = false) mesmo com senha válida")
    void shouldBlockInactiveUserLoginEvenWithValidPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("ex@provedor.com.br");
        request.setPassword("SenhaSegura123");

        when(userRepository.findByEmail("ex@provedor.com.br")).thenReturn(Optional.of(inactiveUser));
        lenient().when(passwordEncoder.matches("SenhaSegura123", inactiveUser.getPassword())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.authenticate(request));
        assertTrue(ex.getMessage().toLowerCase().contains("inativo") || ex.getMessage().toLowerCase().contains("desativado"));
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }
}
