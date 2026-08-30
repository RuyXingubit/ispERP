package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.FiscalRegimeTransitionRequest;
import br.dev.xb.isperp.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalRegimeTransition;
import br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus;
import br.dev.xb.isperp.mapper.FiscalRegimeTransitionMapper;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalRegimeTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiscalRegimeTransitionServiceTest {

    @Mock
    private FiscalRegimeTransitionRepository transitionRepository;

    @Mock
    private FiscalCompanyRepository companyRepository;

    @Spy
    private FiscalRegimeTransitionMapper transitionMapper = Mappers.getMapper(FiscalRegimeTransitionMapper.class);

    @InjectMocks
    private FiscalRegimeTransitionService service;

    private FiscalCompany company;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        company = FiscalCompany.builder()
                .id(companyId)
                .cnpj("12.345.678/0001-90")
                .razaoSocial("ISP Telecom Ltda")
                .regimeTributario("SIMPLES_NACIONAL")
                .aliquotaIcms(BigDecimal.ZERO)
                .aliquotaPis(BigDecimal.ZERO)
                .aliquotaCofins(BigDecimal.ZERO)
                .aliquotaFust(new BigDecimal("0.65"))
                .aliquotaFunttel(new BigDecimal("0.50"))
                .fiscalConfirmed(true)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve aplicar transição imediatamente quando a data de vigência for hoje")
    void shouldApplyTransitionImmediatelyWhenDateIsToday() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(transitionRepository.save(any(FiscalRegimeTransition.class))).thenAnswer(inv -> inv.getArgument(0));

        FiscalRegimeTransitionRequest request = FiscalRegimeTransitionRequest.builder()
                .companyId(companyId)
                .newRegime("LUCRO_PRESUMIDO")
                .effectiveDate(LocalDate.now())
                .aliquotaIcms(new BigDecimal("18.00"))
                .aliquotaPis(new BigDecimal("0.65"))
                .aliquotaCofins(new BigDecimal("3.00"))
                .aliquotaFust(new BigDecimal("0.65"))
                .aliquotaFunttel(new BigDecimal("0.50"))
                .notes("Mudança anual para Lucro Presumido")
                .build();

        FiscalRegimeTransitionResponse response = service.scheduleOrApply(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(FiscalRegimeTransitionStatus.APPLIED);
        assertThat(response.getPreviousRegime()).isEqualTo("SIMPLES_NACIONAL");
        assertThat(response.getNewRegime()).isEqualTo("LUCRO_PRESUMIDO");

        // Verifica que a empresa foi atualizada imediatamente
        assertThat(company.getRegimeTributario()).isEqualTo("LUCRO_PRESUMIDO");
        assertThat(company.getAliquotaIcms()).isEqualByComparingTo("18.00");
        assertThat(company.getAliquotaPis()).isEqualByComparingTo("0.65");
        assertThat(company.getAliquotaCofins()).isEqualByComparingTo("3.00");
        verify(companyRepository).save(company);
    }

    @Test
    @DisplayName("Deve agendar transição quando a data de vigência for futura")
    void shouldScheduleTransitionWhenDateIsFuture() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(transitionRepository.save(any(FiscalRegimeTransition.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate futureDate = LocalDate.now().plusMonths(1);
        FiscalRegimeTransitionRequest request = FiscalRegimeTransitionRequest.builder()
                .companyId(companyId)
                .newRegime("LUCRO_REAL")
                .effectiveDate(futureDate)
                .aliquotaIcms(new BigDecimal("18.00"))
                .aliquotaPis(new BigDecimal("1.65"))
                .aliquotaCofins(new BigDecimal("7.60"))
                .notes("Agendamento para o próximo ano fiscal")
                .build();

        FiscalRegimeTransitionResponse response = service.scheduleOrApply(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(FiscalRegimeTransitionStatus.SCHEDULED);
        assertThat(response.getEffectiveDate()).isEqualTo(futureDate);

        // A empresa permanece inalterada até a vigência
        assertThat(company.getRegimeTributario()).isEqualTo("SIMPLES_NACIONAL");
        verify(companyRepository, never()).save(company);
    }

    @Test
    @DisplayName("Deve processar transições agendadas pendentes que atingiram a vigência")
    void shouldApplyPendingTransitionsWhenEffectiveDateReached() {
        FiscalRegimeTransition scheduled = FiscalRegimeTransition.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .previousRegime("SIMPLES_NACIONAL")
                .newRegime("LUCRO_PRESUMIDO")
                .effectiveDate(LocalDate.now())
                .aliquotaIcms(new BigDecimal("18.00"))
                .aliquotaPis(new BigDecimal("0.65"))
                .aliquotaCofins(new BigDecimal("3.00"))
                .aliquotaFust(new BigDecimal("0.65"))
                .aliquotaFunttel(new BigDecimal("0.50"))
                .status(FiscalRegimeTransitionStatus.SCHEDULED)
                .build();

        when(transitionRepository.findPendingTransitionsToApply(eq(FiscalRegimeTransitionStatus.SCHEDULED), any(LocalDate.class)))
                .thenReturn(List.of(scheduled));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        int applied = service.applyPendingTransitions();

        assertThat(applied).isEqualTo(1);
        assertThat(scheduled.getStatus()).isEqualTo(FiscalRegimeTransitionStatus.APPLIED);
        assertThat(company.getRegimeTributario()).isEqualTo("LUCRO_PRESUMIDO");
        verify(companyRepository).save(company);
        verify(transitionRepository).save(scheduled);
    }

    @Test
    @DisplayName("Deve cancelar transição agendada com sucesso")
    void shouldCancelScheduledTransition() {
        UUID transitionId = UUID.randomUUID();
        FiscalRegimeTransition scheduled = FiscalRegimeTransition.builder()
                .id(transitionId)
                .companyId(companyId)
                .previousRegime("SIMPLES_NACIONAL")
                .newRegime("LUCRO_PRESUMIDO")
                .status(FiscalRegimeTransitionStatus.SCHEDULED)
                .build();

        when(transitionRepository.findById(transitionId)).thenReturn(Optional.of(scheduled));
        when(transitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FiscalRegimeTransitionResponse response = service.cancelTransition(transitionId);

        assertThat(response.getStatus()).isEqualTo(FiscalRegimeTransitionStatus.CANCELLED);
    }
}
