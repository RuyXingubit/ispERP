package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.util.UsernameGeneratorUtils;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientCredentialsConsumer {

    private static final String CONSUMER_NAME = "ClientCredentialsConsumer";

    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final IdempotencyService idempotencyService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleContractCreated(DomainEvent event) {
        if (!"CONTRACT_CREATED".equals(event.getEventType())) {
            return;
        }

        log.info("Gerando credenciais de acesso para novo contrato: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME, () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());

                String customerName = (String) data.get("customerName");
                String customerEmail = (String) data.get("customerEmail");
                UUID customerId = UUID.fromString(data.get("customerId").toString());
                UUID contractId = UUID.fromString(data.get("contractId").toString());

                // Gera login (ex: ruyfranca) e senha (ex: franca)
                String username = UsernameGeneratorUtils.generateUsername(customerName);
                String initialPassword = UsernameGeneratorUtils.generateInitialPassword(customerName);

                String userEmail = (customerEmail != null && !customerEmail.trim().isEmpty())
                        ? customerEmail.trim().toLowerCase()
                        : username + "@cliente.isperp";

                // Se o usuário ainda não existir no portal, cria o registro
                if (!userRepository.existsByEmail(userEmail)) {
                    User clientUser = User.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .name(customerName)
                            .email(userEmail)
                            .password(passwordEncoder.encode(initialPassword))
                            .role(UserRole.CLIENT)
                            .active(true)
                            .build();

                    userRepository.save(clientUser);
                    log.info("Acesso do cliente gerado com sucesso: login={}, email={}", username, userEmail);
                }

                // Publica evento notificando que o acesso foi gerado
                Map<String, Object> accessPayload = new HashMap<>();
                accessPayload.put("customerId", customerId.toString());
                accessPayload.put("contractId", contractId.toString());
                accessPayload.put("username", username);
                accessPayload.put("userEmail", userEmail);
                accessPayload.put("initialPassword", initialPassword);

                GenericDomainEvent accessEvent = GenericDomainEvent.builder()
                        .eventId(UuidCreatorUtils.generateUuidV7())
                        .eventType("CLIENT_ACCESS_GENERATED")
                        .aggregateType("Customer")
                        .aggregateId(customerId.toString())
                        .payload(accessPayload)
                        .build();

                domainEventPublisher.publish(accessEvent);

            } catch (Exception e) {
                log.error("Erro ao gerar credenciais do cliente: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao gerar credenciais do cliente", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Object payload) {
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        try {
            return objectMapper.readValue(payload.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter payload", e);
        }
    }
}
