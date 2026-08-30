package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.FiscalRegimeTransitionRequest;
import br.dev.xb.isperp.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalRegimeTransition;
import br.dev.xb.isperp.exception.BusinessException;
import br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus;
import br.dev.xb.isperp.mapper.FiscalRegimeTransitionMapper;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalRegimeTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FiscalRegimeTransitionService {

    private final FiscalRegimeTransitionRepository transitionRepository;
    private final FiscalCompanyRepository companyRepository;
    private final FiscalRegimeTransitionMapper transitionMapper;

    @Transactional
    public FiscalRegimeTransitionResponse scheduleOrApply(FiscalRegimeTransitionRequest request) {
        FiscalCompany company = resolveCompany(request.getCompanyId());

        String previousRegime = company.getRegimeTributario();
        LocalDate today = LocalDate.now();
        boolean isImmediate = !request.getEffectiveDate().isAfter(today);

        FiscalRegimeTransition transition = transitionMapper.toEntity(request);
        transition.setCompanyId(company.getId());
        transition.setPreviousRegime(previousRegime);

        if (isImmediate) {
            transition.setStatus(FiscalRegimeTransitionStatus.APPLIED);
            transition.setAppliedAt(LocalDateTime.now());

            // Aplica imediatamente na empresa
            applyRegimeToCompany(company, transition);
            companyRepository.save(company);

            log.info("⚡ [FiscalRegime] Transição IMEDIATA aplicada para a empresa {}: {} -> {} (Vigência: {})",
                    company.getRazaoSocial(), previousRegime, transition.getNewRegime(), transition.getEffectiveDate());
        } else {
            transition.setStatus(FiscalRegimeTransitionStatus.SCHEDULED);
            log.info("📅 [FiscalRegime] Transição AGENDADA para a empresa {}: {} -> {} para vigência em {}",
                    company.getRazaoSocial(), previousRegime, transition.getNewRegime(), transition.getEffectiveDate());
        }

        FiscalRegimeTransition saved = transitionRepository.save(transition);
        return transitionMapper.toDto(saved);
    }

    @Transactional
    public int applyPendingTransitions() {
        LocalDate today = LocalDate.now();
        List<FiscalRegimeTransition> pending = transitionRepository.findPendingTransitionsToApply(
                FiscalRegimeTransitionStatus.SCHEDULED, today);

        if (pending.isEmpty()) {
            return 0;
        }

        int appliedCount = 0;
        for (FiscalRegimeTransition transition : pending) {
            Optional<FiscalCompany> companyOpt = companyRepository.findById(transition.getCompanyId());
            if (companyOpt.isPresent()) {
                FiscalCompany company = companyOpt.get();
                applyRegimeToCompany(company, transition);
                companyRepository.save(company);

                transition.setStatus(FiscalRegimeTransitionStatus.APPLIED);
                transition.setAppliedAt(LocalDateTime.now());
                transitionRepository.save(transition);

                appliedCount++;
                log.info("🚀 [FiscalRegimeScheduler] Transição agendada aplicada com sucesso para {}: {} -> {} (Vigência: {})",
                        company.getRazaoSocial(), transition.getPreviousRegime(), transition.getNewRegime(), transition.getEffectiveDate());
            }
        }

        return appliedCount;
    }

    @Transactional
    public FiscalRegimeTransitionResponse cancelTransition(UUID transitionId) {
        FiscalRegimeTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new BusinessException("Transição fiscal não encontrada com ID: " + transitionId));

        if (transition.getStatus() != FiscalRegimeTransitionStatus.SCHEDULED) {
            throw new BusinessException("Apenas transições com status AGENDADO podem ser canceladas.");
        }

        transition.setStatus(FiscalRegimeTransitionStatus.CANCELLED);
        FiscalRegimeTransition saved = transitionRepository.save(transition);
        log.info("🚫 [FiscalRegime] Transição fiscal cancelada: {}", transitionId);
        return transitionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FiscalRegimeTransitionResponse> getHistory(UUID companyId) {
        FiscalCompany company = resolveCompany(companyId);
        List<FiscalRegimeTransition> list = transitionRepository.findByCompanyIdOrderByEffectiveDateDescCreatedAtDesc(company.getId());
        return transitionMapper.toDtoList(list);
    }

    private void applyRegimeToCompany(FiscalCompany company, FiscalRegimeTransition transition) {
        company.setRegimeTributario(transition.getNewRegime());
        company.setAliquotaIcms(transition.getAliquotaIcms());
        company.setAliquotaPis(transition.getAliquotaPis());
        company.setAliquotaCofins(transition.getAliquotaCofins());
        company.setAliquotaFust(transition.getAliquotaFust());
        company.setAliquotaFunttel(transition.getAliquotaFunttel());
        company.setFiscalConfirmed(true);
        company.setFiscalConfirmedAt(LocalDateTime.now());
    }

    private FiscalCompany resolveCompany(UUID companyId) {
        if (companyId != null) {
            return companyRepository.findById(companyId)
                    .orElseThrow(() -> new BusinessException("Empresa fiscal não encontrada com ID: " + companyId));
        }
        return companyRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new BusinessException("Nenhuma empresa fiscal ativa cadastrada no sistema."));
    }
}
