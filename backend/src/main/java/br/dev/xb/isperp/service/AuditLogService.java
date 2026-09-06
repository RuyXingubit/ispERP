package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.AuditLogResponseDto;
import br.dev.xb.isperp.entity.AuditLog;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.repository.AuditLogRepository;
import br.dev.xb.isperp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuditLog logAction(
            @Nullable UUID userId,
            String action,
            String entityName,
            @Nullable String entityId,
            @Nullable Map<String, Object> details
    ) {
        try {
            String jsonDetails = null;
            if (details != null && !details.isEmpty()) {
                jsonDetails = objectMapper.writeValueAsString(details);
            }

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .details(jsonDetails)
                    .createdAt(LocalDateTime.now())
                    .build();

            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Falha ao persistir log de auditoria para action={}, entity={}: {}", action, entityName, e.getMessage());
            return null;
        }
    }

    @Transactional
    public AuditLog logAction(
            String action,
            String entityName,
            @Nullable String entityId,
            @Nullable Map<String, Object> details
    ) {
        UUID currentUserId = resolveCurrentUserId();
        return logAction(currentUserId, action, entityName, entityId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> searchLogs(
            @Nullable UUID userId,
            @Nullable String entityName,
            @Nullable String action,
            @Nullable LocalDateTime startDate,
            @Nullable LocalDateTime endDate,
            Pageable pageable
    ) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }

            if (entityName != null && !entityName.trim().isEmpty() && !entityName.equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("entityName")), entityName.trim().toUpperCase()));
            }

            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.like(cb.upper(root.get("action")), "%" + action.trim().toUpperCase() + "%"));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Garante ordenação decrescente por data de criação por padrão
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return auditLogRepository.findAll(spec, sortedPageable).map(this::toResponseDto);
    }

    private AuditLogResponseDto toResponseDto(AuditLog logEntity) {
        String userName = "Sistema";
        String userEmail = "sistema@isperp.local";

        if (logEntity.getUser() != null) {
            userName = logEntity.getUser().getName();
            userEmail = logEntity.getUser().getEmail();
        } else if (logEntity.getUserId() != null) {
            User user = userRepository.findById(logEntity.getUserId()).orElse(null);
            if (user != null) {
                userName = user.getName();
                userEmail = user.getEmail();
            }
        }

        return AuditLogResponseDto.builder()
                .id(logEntity.getId())
                .userId(logEntity.getUserId())
                .userName(userName)
                .userEmail(userEmail)
                .action(logEntity.getAction())
                .entityName(logEntity.getEntityName())
                .entityId(logEntity.getEntityId())
                .details(logEntity.getDetails())
                .createdAt(logEntity.getCreatedAt())
                .build();
    }

    @Nullable
    private UUID resolveCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                return userRepository.findByEmail(auth.getName())
                        .map(User::getId)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.debug("Não foi possível inferir o usuário autenticado para auditoria: {}", e.getMessage());
        }
        return null;
    }
}
