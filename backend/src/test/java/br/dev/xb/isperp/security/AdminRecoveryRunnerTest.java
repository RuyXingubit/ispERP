package br.dev.xb.isperp.security;

import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AdminRecoveryRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminRecoveryRunner recoveryRunner;

    @Test
    @DisplayName("Não deve executar recuperação quando variável de senha estiver vazia")
    void shouldDoNothingWhenPasswordIsEmpty() {
        ReflectionTestUtils.setField(recoveryRunner, "recoveryPassword", "");
        ReflectionTestUtils.setField(recoveryRunner, "recoveryEmail", "admin@provedor.com.br");

        recoveryRunner.run(new DefaultApplicationArguments());

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Deve redefinir senha do administrador existente quando solicitado via ENV")
    void shouldResetExistingAdminPasswordWhenProvidedViaEnv() {
        User existingAdmin = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Carlos Diretor")
                .email("carlos@provedor.com.br")
                .password("oldHashedPassword")
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        when(passwordEncoder.encode("NovaSenhaSegura2026!")).thenReturn("newBcryptHash");
        when(userRepository.findByEmail("carlos@provedor.com.br")).thenReturn(Optional.of(existingAdmin));

        ReflectionTestUtils.setField(recoveryRunner, "recoveryPassword", "NovaSenhaSegura2026!");
        ReflectionTestUtils.setField(recoveryRunner, "recoveryEmail", "carlos@provedor.com.br");

        recoveryRunner.run(new DefaultApplicationArguments());

        verify(passwordEncoder).encode("NovaSenhaSegura2026!");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("newBcryptHash");
        assertThat(saved.getEmail()).isEqualTo("carlos@provedor.com.br");
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    @DisplayName("Deve criar novo administrador supremo caso nenhum exista e recovery for acionado")
    void shouldCreateNewAdminWhenNoUserFoundAndRecoveryRequested() {
        when(passwordEncoder.encode("MasterRecoveryPass999")).thenReturn("masterHashedPassword");
        when(userRepository.findByEmail("super@provedor.com.br")).thenReturn(Optional.empty());
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of());

        ReflectionTestUtils.setField(recoveryRunner, "recoveryPassword", "MasterRecoveryPass999");
        ReflectionTestUtils.setField(recoveryRunner, "recoveryEmail", "super@provedor.com.br");

        recoveryRunner.run(new DefaultApplicationArguments());

        verify(passwordEncoder).encode("MasterRecoveryPass999");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User created = captor.getValue();
        assertThat(created.getEmail()).isEqualTo("super@provedor.com.br");
        assertThat(created.getPassword()).isEqualTo("masterHashedPassword");
        assertThat(created.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(created.getActive()).isTrue();
    }
}
