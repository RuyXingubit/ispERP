package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CreateSaleRequest;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.Sale;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.repository.SaleRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private SaleService saleService;

    private CreateSaleRequest saleRequest;
    private Plan samplePlan;
    private UUID planId;

    @BeforeEach
    void setUp() {
        planId = UuidCreatorUtils.generateUuidV7();
        samplePlan = Plan.builder()
                .id(planId)
                .name("Fibra 500 Mega")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(new BigDecimal("99.90"))
                .active(true)
                .build();

        saleRequest = CreateSaleRequest.builder()
                .planId(planId)
                .customerName("João Pereira")
                .customerCpf("529.982.247-25") // CPF Válido
                .customerEmail("joao@email.com")
                .customerPhone("11988887777")
                .installationAddress("Av. Paulista, 1500, Apto 42")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .preferredDueDate(10)
                .notificationChannel("WHATSAPP")
                .sellerName("Vendedor 1")
                .build();
    }

    @Test
    @DisplayName("Deve submeter venda, persistir e publicar evento SALE_SUBMITTED")
    void shouldSubmitSaleAndPublishEvent() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(samplePlan));
        when(saleRepository.save(any(Sale.class))).thenAnswer(i -> i.getArgument(0));

        Sale createdSale = saleService.submitSale(saleRequest);

        assertNotNull(createdSale);
        assertEquals("52998224725", createdSale.getCustomerCpf(), "CPF deve ser normalizado");
        assertEquals(Sale.SaleStatus.SUBMITTED, createdSale.getStatus());

        verify(saleRepository, times(1)).save(any(Sale.class));
        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve rejeitar venda com CPF inválido")
    void shouldThrowExceptionWhenCpfIsInvalid() {
        saleRequest.setCustomerCpf("000.000.000-00");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> saleService.submitSale(saleRequest));
        assertTrue(ex.getMessage().contains("CPF inválido"));

        verify(saleRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Deve rejeitar venda se plano estiver inativo")
    void shouldThrowExceptionWhenPlanIsInactive() {
        samplePlan.setActive(false);
        when(planRepository.findById(planId)).thenReturn(Optional.of(samplePlan));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> saleService.submitSale(saleRequest));
        assertTrue(ex.getMessage().contains("inativo"));

        verify(saleRepository, never()).save(any());
    }
}
