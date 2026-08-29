package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Plan;
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
    @DisplayName("Deve criar plano de internet com sucesso")
    void shouldCreatePlanSuccessfully() {
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        Plan created = planService.createPlan(samplePlan);

        assertNotNull(created);
        assertEquals("Fibra 600 Mega", created.getName());
        assertEquals(new BigDecimal("119.90"), created.getPrice());
        verify(planRepository, times(1)).save(samplePlan);
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
