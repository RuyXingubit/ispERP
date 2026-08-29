package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.repository.UserRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Admin Teste")
                .email("admin@provedor.com.br")
                .password("senhaSegura123")
                .role(User.UserRole.ADMIN)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve criar usuário com senha criptografada")
    void shouldCreateUserWithEncodedPassword() {
        when(userRepository.existsByEmail("admin@provedor.com.br")).thenReturn(false);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(validUser);

        assertNotNull(created);
        assertEquals("$2a$12$hashedPassword", created.getPassword());
        verify(passwordEncoder, times(1)).encode("senhaSegura123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve rejeitar criação de usuário com email duplicado")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("admin@provedor.com.br")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(validUser));
        assertEquals("Email já cadastrado", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar usuário por UUID")
    void shouldFindUserById() {
        UUID userId = validUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));

        Optional<User> found = userService.getUserById(userId);

        assertTrue(found.isPresent());
        assertEquals("Admin Teste", found.get().getName());
        assertEquals(userId, found.get().getId());
    }
}
