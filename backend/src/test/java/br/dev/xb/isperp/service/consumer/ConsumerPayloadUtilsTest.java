package br.dev.xb.isperp.service.consumer;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ConsumerPayloadUtilsTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContractRepository contractRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve extrair payload de Map ou String JSON e falhar em JSON inválido")
    void shouldExtractPayloadFromMapAndJson() {
        Map<String, Object> original = Map.of("key", "value");
        assertThat(ConsumerPayloadUtils.extractPayload(objectMapper, original)).isEqualTo(original);

        Map<String, Object> fromJson = ConsumerPayloadUtils.extractPayload(objectMapper, "{\"key\":\"value\"}");
        assertThat(fromJson).containsEntry("key", "value");

        assertThatThrownBy(() -> ConsumerPayloadUtils.extractPayload(objectMapper, "{invalid-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve cadastrar cliente e criar contrato a partir do payload de venda")
    void shouldUpsertCustomerAndCreateContract() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerCpf", "12345678901");
        data.put("customerName", "Maria Silva");
        data.put("customerEmail", "maria@teste.com");
        data.put("customerPhone", "11999999999");
        data.put("installationAddress", "Rua A, 100");
        data.put("city", "São Paulo");
        data.put("state", "SP");
        data.put("zipCode", "01001-000");
        data.put("planId", UuidCreatorUtils.generateUuidV7().toString());
        data.put("saleId", UuidCreatorUtils.generateUuidV7().toString());
        data.put("monthlyFee", "99.90");
        data.put("preferredDueDate", "15");
        data.put("latitude", "-23.550520");
        data.put("longitude", "-46.633308");
        data.put("ctoId", UuidCreatorUtils.generateUuidV7().toString());
        data.put("ctoPortNumber", "4");

        when(customerRepository.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsumerPayloadUtils.CustomerAndContract result = ConsumerPayloadUtils.upsertCustomerAndCreateContract(
                customerRepository,
                contractRepository,
                data,
                Contract.ContractStatus.DRAFT
        );

        assertThat(result.customer().getName()).isEqualTo("Maria Silva");
        assertThat(result.contract().getStatus()).isEqualTo(Contract.ContractStatus.DRAFT);
        assertThat(result.contract().getDueDay()).isEqualTo(15);

        GenericDomainEvent event = ConsumerPayloadUtils.buildEvent("TEST_EVENT", "Contract", "123", data);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("TEST_EVENT");
    }

    @Test
    @DisplayName("Deve executar ação idempotente apenas quando eventType coincidir")
    void shouldExecuteForEventTypeWhenMatching() {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        Map<String, Object> payload = Map.of("contractId", "abc-123");
        GenericDomainEvent event = ConsumerPayloadUtils.buildEvent("CONTRACT_CREATED", "Contract", "abc-123", payload);

        ConsumerPayloadUtils.executeForEventType(
                event, "OTHER_EVENT", "TestConsumer", idempotencyService, objectMapper, data -> {}
        );
        verifyNoInteractions(idempotencyService);

        doAnswer(inv -> {
            Runnable action = inv.getArgument(2);
            action.run();
            return null;
        }).when(idempotencyService).executeIdempotent(eq(event.getEventId()), eq("TestConsumer"), any(Runnable.class));

        Map<String, Object> captured = new HashMap<>();
        ConsumerPayloadUtils.executeForEventType(
                event, "CONTRACT_CREATED", "TestConsumer", idempotencyService, objectMapper, captured::putAll
        );
        assertThat(captured).containsEntry("contractId", "abc-123");
    }
}
