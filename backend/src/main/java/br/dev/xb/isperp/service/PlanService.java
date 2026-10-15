package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.PlanCompositionCategory;
import br.dev.xb.isperp.entity.PlanCompositionItem;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PlanService {

    private final PlanRepository planRepository;

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public List<Plan> getActivePlans() {
        return planRepository.findByActiveTrueOrderByIdDesc();
    }

    public Optional<Plan> getPlanById(UUID id) {
        return planRepository.findById(id);
    }

    @Transactional
    public Plan createPlan(Plan plan) {
        validateAndNormalizeComposition(plan);
        return planRepository.save(plan);
    }

    @Transactional
    public Plan updatePlan(UUID id, Plan planDetails) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano não encontrado"));

        plan.setName(planDetails.getName());
        plan.setDownloadSpeed(planDetails.getDownloadSpeed());
        plan.setUploadSpeed(planDetails.getUploadSpeed());
        plan.setPrice(planDetails.getPrice());
        plan.setDescription(planDetails.getDescription());
        plan.setSvaIncluded(planDetails.getSvaIncluded());
        if (planDetails.getSuspensionDays() != null) {
            plan.setSuspensionDays(planDetails.getSuspensionDays());
        }
        if (planDetails.getAlwaysIssueNfcom() != null) {
            plan.setAlwaysIssueNfcom(planDetails.getAlwaysIssueNfcom());
        }
        if (planDetails.getActive() != null) {
            plan.setActive(planDetails.getActive());
        }

        if (plan.getCompositionItems() == null) {
            plan.setCompositionItems(new ArrayList<>());
        } else {
            plan.getCompositionItems().clear();
        }
        if (planDetails.getCompositionItems() != null) {
            for (PlanCompositionItem item : planDetails.getCompositionItems()) {
                plan.getCompositionItems().add(PlanCompositionItem.builder()
                        .id(item.getId() != null ? item.getId() : UuidCreatorUtils.generateUuidV7())
                        .plan(plan)
                        .name(item.getName())
                        .category(item.getCategory() != null ? item.getCategory() : PlanCompositionCategory.SCM)
                        .amount(item.getAmount())
                        .fiscalCode(item.getFiscalCode())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        validateAndNormalizeComposition(plan);
        return planRepository.save(plan);
    }

    public void validateAndNormalizeComposition(Plan plan) {
        BigDecimal planPrice = plan.getPrice() != null ? plan.getPrice() : BigDecimal.ZERO;
        if (plan.getCompositionItems() == null) {
            plan.setCompositionItems(new ArrayList<>());
        }

        if (plan.getCompositionItems().isEmpty()) {
            plan.getCompositionItems().add(PlanCompositionItem.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .plan(plan)
                    .name("Conectividade Fibra Óptica SCM - " + (plan.getName() != null ? plan.getName() : "Plano"))
                    .category(PlanCompositionCategory.SCM)
                    .amount(planPrice)
                    .fiscalCode("5307")
                    .createdAt(LocalDateTime.now())
                    .build());
            plan.recalculateCompositionTotals();
            return;
        }

        BigDecimal totalItems = BigDecimal.ZERO;
        for (PlanCompositionItem item : plan.getCompositionItems()) {
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Todo item da composição do plano deve possuir valor maior que zero.");
            }
            if (item.getName() == null || item.getName().isBlank()) {
                throw new IllegalArgumentException("Todo item da composição do plano deve possuir nome descritivo.");
            }
            item.setPlan(plan);
            totalItems = totalItems.add(item.getAmount());
        }

        if (totalItems.subtract(planPrice).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException(String.format(
                    "A soma dos itens da composição (R$ %.2f) deve ser igual ao preço total do plano (R$ %.2f).",
                    totalItems, planPrice
            ));
        }

        plan.recalculateCompositionTotals();
    }

    public void deletePlan(UUID id) {
        if (!planRepository.existsById(id)) {
            throw new RuntimeException("Plano não encontrado");
        }
        planRepository.deleteById(id);
    }
}

