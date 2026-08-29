package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class HierarchicalBillingServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private HierarchicalBillingService hierarchicalBillingService;

    private UUID customerId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        customerId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve priorizar prazo customizado do contrato sobre todas as outras regras")
    void shouldPrioritizeContractCustomSuspensionDays() {
        Contract contract = Contract.builder()
                .customerId(customerId)
                .planId(planId)
                .customSuspensionDays(90) // Ex: Órgão público com contrato de 90 dias
                .build();

        int days = hierarchicalBillingService.resolveSuspensionDays(contract);
        assertEquals(90, days);
    }

    @Test
    @DisplayName("Deve aplicar 90 dias de carência para cliente do tipo Governo")
    void shouldApply90DaysForGovernmentCustomer() {
        Contract contract = Contract.builder()
                .customerId(customerId)
                .planId(planId)
                .customSuspensionDays(null)
                .build();

        Customer govCustomer = Customer.builder()
                .id(customerId)
                .isGovernment(true)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(govCustomer));

        int days = hierarchicalBillingService.resolveSuspensionDays(contract);
        assertEquals(90, days);
    }

    @Test
    @DisplayName("Deve aplicar prazo do plano quando não houver regra de contrato")
    void shouldApplyPlanSuspensionDaysWhenNoContractRule() {
        Contract contract = Contract.builder()
                .customerId(customerId)
                .planId(planId)
                .customSuspensionDays(null)
                .build();

        Customer regularCustomer = Customer.builder()
                .id(customerId)
                .isGovernment(false)
                .build();

        Plan plan = Plan.builder()
                .id(planId)
                .suspensionDays(2) // 2 dias úteis
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(regularCustomer));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        int days = hierarchicalBillingService.resolveSuspensionDays(contract);
        assertEquals(2, days);
    }

    @Test
    @DisplayName("Deve adiar corte em sextas-feiras ou fins de semana para a próxima segunda-feira")
    void shouldPostponeFridayAndWeekendCutoffsToNextMonday() {
        // Sexta-feira: 2026-08-28 -> Deve virar Segunda-feira 2026-08-31
        LocalDate friday = LocalDate.of(2026, 8, 28);
        LocalDate adjustedFriday = hierarchicalBillingService.adjustCutoffDateToBusinessDay(friday);
        assertEquals(DayOfWeek.MONDAY, adjustedFriday.getDayOfWeek());
        assertEquals(LocalDate.of(2026, 8, 31), adjustedFriday);

        // Sábado: 2026-08-29 -> Deve virar Segunda-feira 2026-08-31
        LocalDate saturday = LocalDate.of(2026, 8, 29);
        LocalDate adjustedSaturday = hierarchicalBillingService.adjustCutoffDateToBusinessDay(saturday);
        assertEquals(DayOfWeek.MONDAY, adjustedSaturday.getDayOfWeek());
        assertEquals(LocalDate.of(2026, 8, 31), adjustedSaturday);

        // Domingo: 2026-08-30 -> Deve virar Segunda-feira 2026-08-31
        LocalDate sunday = LocalDate.of(2026, 8, 30);
        LocalDate adjustedSunday = hierarchicalBillingService.adjustCutoffDateToBusinessDay(sunday);
        assertEquals(DayOfWeek.MONDAY, adjustedSunday.getDayOfWeek());
        assertEquals(LocalDate.of(2026, 8, 31), adjustedSunday);
    }

    @Test
    @DisplayName("Deve permitir corte apenas em dias úteis no período da tarde (14h às 17h30)")
    void shouldAllowCutoffOnlyInAfternoonBusinessHours() {
        // Terça-feira às 15:00 -> Permitido
        LocalDateTime tuesdayAfternoon = LocalDateTime.of(2026, 9, 1, 15, 0);
        assertTrue(hierarchicalBillingService.isAllowedCutoffWindow(tuesdayAfternoon));

        // Terça-feira às 08:00 -> Proibido
        LocalDateTime tuesdayMorning = LocalDateTime.of(2026, 9, 1, 8, 0);
        assertFalse(hierarchicalBillingService.isAllowedCutoffWindow(tuesdayMorning));

        // Sábado às 15:00 -> Proibido
        LocalDateTime saturdayAfternoon = LocalDateTime.of(2026, 8, 29, 15, 0);
        assertFalse(hierarchicalBillingService.isAllowedCutoffWindow(saturdayAfternoon));
    }
}
