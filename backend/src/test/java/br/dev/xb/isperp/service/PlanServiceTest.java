package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.PlanCompositionCategory;
import br.dev.xb.isperp.entity.PlanCompositionItem;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    private Plan samplePlan;
    private UUID planId;

    @BeforeEach
    void setUp() {
        planId = UuidCreatorUtils.generateUuidV7();
        samplePlan = Plan.builder()
                .id(planId)
                .name("Fibra 600 Mega")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .price(new BigDecimal("119.90"))
                .description("Internet de alta velocidade com Wi-Fi 6")
                .svaIncluded("Paramount+ e antivírus")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve criar plano de internet com sucesso e assumir 100% SCM quando nenhum item extra for informado")
    void shouldCreatePlanSuccessfully() {
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        Plan created = planService.createPlan(samplePlan);

        assertNotNull(created);
        assertEquals("Fibra 600 Mega", created.getName());
        assertEquals(new BigDecimal("119.90"), created.getPrice());
        assertEquals(new BigDecimal("119.90"), created.getScmAmount());
        assertEquals(BigDecimal.ZERO, created.getSvaAmount());
        assertEquals(1, created.getCompositionItems().size());
        assertEquals(PlanCompositionCategory.SCM, created.getCompositionItems().get(0).getCategory());
        verify(planRepository, times(1)).save(samplePlan);
    }

    @Test
    @DisplayName("Deve calcular corretamente scmAmount e svaAmount quando múltiplos itens (SCM + Locação + E-books + Streaming) forem informados")
    void shouldCalculateMultiItemScmAndSvaCompositionCorrectly() {
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        List<PlanCompositionItem> items = new ArrayList<>();
        items.add(PlanCompositionItem.builder()
                .name("Conectividade Fibra SCM")
                .category(PlanCompositionCategory.SCM)
                .amount(new BigDecimal("59.90"))
                .fiscalCode("5307")
                .build());
        items.add(PlanCompositionItem.builder()
                .name("Locação Roteador Wi-Fi 6")
                .category(PlanCompositionCategory.EQUIPMENT_RENTAL)
                .amount(new BigDecimal("25.00"))
                .build());
        items.add(PlanCompositionItem.builder()
                .name("Acervo Digital E-Books")
                .category(PlanCompositionCategory.SVA_DIGITAL_CONTENT)
                .amount(new BigDecimal("20.00"))
                .build());
        items.add(PlanCompositionItem.builder()
                .name("Streaming Watch TV")
                .category(PlanCompositionCategory.SVA_STREAMING_APP)
                .amount(new BigDecimal("15.00"))
                .build());

        samplePlan.setCompositionItems(items);

        Plan created = planService.createPlan(samplePlan);

        assertEquals(new BigDecimal("59.90"), created.getScmAmount());
        assertEquals(new BigDecimal("60.00"), created.getSvaAmount());
        assertEquals(4, created.getCompositionItems().size());
    }

    @Test
    @DisplayName("Deve rejeitar plano cuja soma dos itens de composição divirja do preço total do plano")
    void shouldRejectMismatchedCompositionSum() {
        List<PlanCompositionItem> items = new ArrayList<>();
        items.add(PlanCompositionItem.builder()
                .name("Conectividade Fibra SCM")
                .category(PlanCompositionCategory.SCM)
                .amount(new BigDecimal("50.00"))
                .build());
        items.add(PlanCompositionItem.builder()
                .name("Locação Roteador Wi-Fi 6")
                .category(PlanCompositionCategory.EQUIPMENT_RENTAL)
                .amount(new BigDecimal("20.00"))
                .build());

        samplePlan.setCompositionItems(items);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> planService.createPlan(samplePlan)
        );
        assertTrue(ex.getMessage().contains("soma dos itens da composição"));
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar planos ativos")
    void shouldGetActivePlans() {
        when(planRepository.findByActiveTrueOrderByIdDesc()).thenReturn(List.of(samplePlan));

        List<Plan> activePlans = planService.getActivePlans();

        assertEquals(1, activePlans.size());
        assertEquals(planId, activePlans.get(0).getId());
    }

    @Test
    @DisplayName("Deve buscar plano por UUID")
    void shouldFindPlanById() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(samplePlan));

        Optional<Plan> found = planService.getPlanById(planId);

        assertTrue(found.isPresent());
        assertEquals("Fibra 600 Mega", found.get().getName());
    }
}

