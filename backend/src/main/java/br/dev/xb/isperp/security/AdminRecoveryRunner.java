package br.dev.xb.isperp.security;

import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Mecanismo de Recuperação Emergencial (Break-Glass) de Administrador via Variável de Ambiente.
 * 
 * Se o administrador supremo perder a senha ou o acesso ao banco em produção,
 * basta declarar ADMIN_RECOVERY_PASSWORD no .env / container e reiniciar o backend.
 * O sistema sobrescreve a senha com hash BCrypt seguro e emite alerta de auditoria.
 * 
 * Em operação normal (quando a variável não está definida), o runner não executa nenhuma ação.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminRecoveryRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.recovery.email:}")
    private String recoveryEmail;

    @Value("${app.admin.recovery.password:}")
    private String recoveryPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (recoveryPassword == null || recoveryPassword.trim().isEmpty()) {
            log.debug("[AdminRecoveryRunner] Nenhuma solicitação de recuperação emergencial via ENV detectada.");
            return;
        }

        String targetEmail = (recoveryEmail != null && !recoveryEmail.trim().isEmpty()) 
                ? recoveryEmail.trim().toLowerCase() 
                : null;

        Optional<User> targetUser = Optional.empty();

        if (targetEmail != null) {
            targetUser = userRepository.findByEmail(targetEmail);
        }

        if (targetUser.isEmpty()) {
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);
            if (!admins.isEmpty()) {
                targetUser = Optional.of(admins.getFirst());
            }
        }

        if (targetUser.isPresent()) {
            User user = targetUser.get();
            user.setPassword(passwordEncoder.encode(recoveryPassword.trim()));
            user.setActive(true);
            user.setRole(UserRole.ADMIN);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.warn("🚨 [BREAK-GLASS SECURITY] A senha do administrador '{}' foi redefinida com sucesso via variável de ambiente (ADMIN_RECOVERY_PASSWORD). " +
                     "IMPORTANTE: Remova a variável ADMIN_RECOVERY_PASSWORD do ambiente/container após logar e atualizar suas credenciais!", 
                     user.getEmail());
        } else {
            String defaultEmail = targetEmail != null ? targetEmail : "admin@isperp.com.br";
            User newAdmin = User.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .name("Administrador do Sistema")
                    .email(defaultEmail)
                    .password(passwordEncoder.encode(recoveryPassword.trim()))
                    .role(UserRole.ADMIN)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(newAdmin);

            log.warn("🚨 [BREAK-GLASS SECURITY] Nenhum administrador existia no banco. Administrador supremo '{}' criado via variável de ambiente (ADMIN_RECOVERY_PASSWORD).", 
                     newAdmin.getEmail());
        }
    }
}
