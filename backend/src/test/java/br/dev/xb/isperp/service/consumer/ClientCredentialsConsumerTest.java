package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ClientCredentialsConsumerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ClientCredentialsConsumer clientCredentialsConsumer;

    private GenericDomainEvent contractCreatedEvent;

    @BeforeEach
    void setUp() {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        UUID contractId = UuidCreatorUtils.generateUuidV7();

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("contractNumber", "CTR-2026001");
        payload.put("customerId", customerId.toString());
        payload.put("customerName", "Ruy Barbosa Borges França");
        payload.put("customerEmail", "ruy@xingubit.com.br");

        contractCreatedEvent = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("CONTRACT_CREATED")
                .aggregateType("Contract")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();
    }

    @Test
    @DisplayName("Deve gerar usuário com role CLIENT, senha criptografada e emitir CLIENT_ACCESS_GENERATED")
    void shouldGenerateClientCredentials() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());

        when(userRepository.existsByEmail("ruy@xingubit.com.br")).thenReturn(false);
        when(passwordEncoder.encode("franca")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        clientCredentialsConsumer.handleContractCreated(contractCreatedEvent);

        verify(userRepository, times(1)).save(argThat(user ->
                "Ruy Barbosa Borges França".equals(user.getName()) &&
                "ruy@xingubit.com.br".equals(user.getEmail()) &&
                br.dev.xb.isperp.entity.UserRole.CLIENT.equals(user.getRole())
        ));

        verify(domainEventPublisher, times(1)).publish(argThat(event ->
                "CLIENT_ACCESS_GENERATED".equals(event.getEventType())
        ));
    }
}
