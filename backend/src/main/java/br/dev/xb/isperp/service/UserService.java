package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User created = userRepository.save(user);
        auditLogService.logAction("USER_CREATED", "USER", created.getId().toString(), Map.of("email", created.getEmail(), "role", created.getRole().name()));
        return created;
    }

    public User updateUser(UUID id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!user.getEmail().equals(userDetails.getEmail()) && 
            userRepository.existsByEmail(userDetails.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setRole(userDetails.getRole());
        user.setActive(userDetails.getActive());
        if (userDetails.getCpf() != null) {
            user.setCpf(userDetails.getCpf());
        }

        if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank() && !userDetails.getPassword().equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        User updated = userRepository.save(user);
        auditLogService.logAction("USER_UPDATED", "USER", id.toString(), Map.of("email", updated.getEmail(), "role", updated.getRole().name()));
        return updated;
    }

    public User updateUserStatus(UUID id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setActive(active);
        User updated = userRepository.save(user);
        auditLogService.logAction("USER_STATUS_CHANGED", "USER", id.toString(), Map.of("active", active));
        return updated;
    }

    public User updateUserRole(UUID id, br.dev.xb.isperp.entity.UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setRole(role);
        User updated = userRepository.save(user);
        auditLogService.logAction("USER_ROLE_CHANGED", "USER", id.toString(), Map.of("role", role.name()));
        return updated;
    }

    public User resetPassword(UUID id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve ter no mínimo 6 caracteres");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setPassword(passwordEncoder.encode(newPassword));
        User updated = userRepository.save(user);
        auditLogService.logAction("USER_PASSWORD_RESET", "USER", id.toString(), Map.of("action", "ADMIN_RESET"));
        return updated;
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuário não encontrado");
        }
        auditLogService.logAction("USER_DELETED", "USER", id.toString(), Map.of("deletedId", id.toString()));
        userRepository.deleteById(id);
    }
}
