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
                .role(br.dev.xb.isperp.entity.UserRole.ADMIN)
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

    @Test
    @DisplayName("Deve atualizar usuário sem alterar senha e NÃO re-hashear a senha existente")
    void shouldUpdateUserWithoutRehashingExistingPassword() {
        UUID userId = validUser.getId();
        validUser.setPassword("$2a$10$existingBcryptHash");

        User updateDetails = User.builder()
                .name("Admin Atualizado")
                .email("admin@provedor.com.br")
                .role(br.dev.xb.isperp.entity.UserRole.ADMIN)
                .active(true)
                .password("$2a$10$existingBcryptHash")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(userId, updateDetails);

        assertEquals("Admin Atualizado", updated.getName());
        assertEquals("$2a$10$existingBcryptHash", updated.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository).save(validUser);
    }

    @Test
    @DisplayName("Deve atualizar usuário e criptografar nova senha quando informada")
    void shouldUpdateUserAndEncodeNewPasswordWhenProvided() {
        UUID userId = validUser.getId();
        validUser.setPassword("$2a$10$existingBcryptHash");

        User updateDetails = User.builder()
                .name("Admin Atualizado")
                .email("admin@provedor.com.br")
                .role(br.dev.xb.isperp.entity.UserRole.ADMIN)
                .active(true)
                .password("novaSenhaSecreta456")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.encode("novaSenhaSecreta456")).thenReturn("$2a$10$brandNewHashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(userId, updateDetails);

        assertEquals("$2a$10$brandNewHashedPassword", updated.getPassword());
        verify(passwordEncoder).encode("novaSenhaSecreta456");
    }

    @Test
    @DisplayName("Deve suspender e reativar colaborador (updateUserStatus)")
    void shouldUpdateUserStatus() {
        UUID userId = validUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User suspended = userService.updateUserStatus(userId, false);
        assertFalse(suspended.getActive());

        User reactivated = userService.updateUserStatus(userId, true);
        assertTrue(reactivated.getActive());

        verify(userRepository, times(2)).save(validUser);
    }

    @Test
    @DisplayName("Deve alterar papel de acesso RBAC do colaborador (updateUserRole)")
    void shouldUpdateUserRole() {
        UUID userId = validUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User roleChanged = userService.updateUserRole(userId, br.dev.xb.isperp.entity.UserRole.FINANCIAL);
        assertEquals(br.dev.xb.isperp.entity.UserRole.FINANCIAL, roleChanged.getRole());

        verify(userRepository).save(validUser);
    }

    @Test
    @DisplayName("Deve redefinir senha com sucesso (resetPassword)")
    void shouldResetPassword() {
        UUID userId = validUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.encode("reset@2026Nova")).thenReturn("$2a$10$hashedResetPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User reset = userService.resetPassword(userId, "reset@2026Nova");
        assertEquals("$2a$10$hashedResetPassword", reset.getPassword());

        verify(passwordEncoder).encode("reset@2026Nova");
        verify(userRepository).save(validUser);
    }

    @Test
    @DisplayName("Deve rejeitar redefinição de senha com menos de 6 caracteres")
    void shouldRejectResetPasswordWhenTooShort() {
        UUID userId = validUser.getId();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword(userId, "123")
        );

        assertEquals("A nova senha deve ter no mínimo 6 caracteres", ex.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve remover usuário existente (deleteUser)")
    void shouldDeleteUserWhenExists() {
        UUID userId = validUser.getId();
        when(userRepository.existsById(userId)).thenReturn(true);

        assertDoesNotThrow(() -> userService.deleteUser(userId));
        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar remover usuário inexistente")
    void shouldThrowWhenDeletingNonExistentUser() {
        UUID userId = validUser.getId();
        when(userRepository.existsById(userId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.deleteUser(userId));
        assertEquals("Usuário não encontrado", ex.getMessage());
        verify(userRepository, never()).deleteById(any());
    }
}
