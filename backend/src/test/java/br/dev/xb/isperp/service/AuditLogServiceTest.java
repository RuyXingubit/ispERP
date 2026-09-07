package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.AuditLogResponseDto;
import br.dev.xb.isperp.entity.AuditLog;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.repository.AuditLogRepository;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    private User operator;
    private UUID operatorId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        operatorId = UuidCreatorUtils.generateUuidV7();
        operator = User.builder()
                .id(operatorId)
                .name("Operador Admin")
                .email("admin@provedor.com.br")
                .role(UserRole.ADMIN)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve registrar log de auditoria com sucesso persistindo detalhes em JSON")
    void shouldLogActionSuccessfully() throws Exception {
        UUID entityId = UuidCreatorUtils.generateUuidV7();
        Map<String, Object> details = Map.of("motivo", "Baixa manual de fatura", "valor", "120.00");
        String jsonString = "{\"motivo\":\"Baixa manual de fatura\",\"valor\":\"120.00\"}";

        when(objectMapper.writeValueAsString(details)).thenReturn(jsonString);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(UuidCreatorUtils.generateUuidV7());
            return log;
        });

        AuditLog result = auditLogService.logAction(
                operatorId,
                "INVOICE_PAID",
                "INVOICE",
                entityId.toString(),
                details
        );

        assertNotNull(result);
        assertEquals(operatorId, result.getUserId());
        assertEquals("INVOICE_PAID", result.getAction());
        assertEquals("INVOICE", result.getEntityName());
        assertEquals(entityId.toString(), result.getEntityId());
        assertEquals(jsonString, result.getDetails());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Não deve lançar exceção se houver falha ao serializar ou persistir auditoria")
    void shouldHandleExceptionGracefullyWhenAuditFails() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Erro JSON") {});

        AuditLog result = auditLogService.logAction(
                operatorId,
                "SECURITY_ALERT",
                "AUTH",
                "123",
                Map.of("key", "value")
        );

        assertNull(result);
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve inferir userId do usuário autenticado no SecurityContextHolder")
    void shouldInferUserIdFromSecurityContextHolder() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@provedor.com.br", null, List.of())
        );

        when(userRepository.findByEmail("admin@provedor.com.br")).thenReturn(Optional.of(operator));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.logAction(
                "USER_ROLE_CHANGED",
                "USER",
                "user-123",
                null
        );

        assertNotNull(result);
        assertEquals(operatorId, result.getUserId());
        assertEquals("USER_ROLE_CHANGED", result.getAction());
    }

    @Test
    @DisplayName("Deve pesquisar logs de auditoria aplicando filtros dinâmicos e paginação")
    void shouldSearchLogsWithFilters() {
        AuditLog log1 = AuditLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .userId(operatorId)
                .user(operator)
                .action("FINANCIAL_DEBIT")
                .entityName("FINANCIAL")
                .entityId("entry-01")
                .details("{\"valor\":\"500.00\"}")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log1), PageRequest.of(0, 10), 1);
        when(auditLogRepository.findAll(org.mockito.ArgumentMatchers.<Specification<AuditLog>>any(), any(Pageable.class))).thenReturn(page);

        Page<AuditLogResponseDto> result = auditLogService.searchLogs(
                operatorId,
                "FINANCIAL",
                "DEBIT",
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now(),
                PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        AuditLogResponseDto dto = result.getContent().get(0);
        assertEquals(operator.getName(), dto.getUserName());
        assertEquals(operator.getEmail(), dto.getUserEmail());
        assertEquals("FINANCIAL_DEBIT", dto.getAction());
        assertEquals("FINANCIAL", dto.getEntityName());
    }
}
