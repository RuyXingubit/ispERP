package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public List<Plan> getActivePlans() {
        return planRepository.findByActiveTrueOrderByIdDesc();
    }

    public Optional<Plan> getPlanById(@NonNull UUID id) {
        return planRepository.findById(id);
    }

    public Plan createPlan(@NonNull Plan plan) {
        return planRepository.save(plan);
    }

    public Plan updatePlan(@NonNull UUID id, @NonNull Plan planDetails) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano não encontrado"));

        plan.setName(planDetails.getName());
        plan.setDownloadSpeed(planDetails.getDownloadSpeed());
        plan.setUploadSpeed(planDetails.getUploadSpeed());
        plan.setPrice(planDetails.getPrice());
        plan.setDescription(planDetails.getDescription());
        plan.setSvaIncluded(planDetails.getSvaIncluded());
        plan.setActive(planDetails.getActive());

        return planRepository.save(plan);
    }

    public void deletePlan(@NonNull UUID id) {
        if (!planRepository.existsById(id)) {
            throw new RuntimeException("Plano não encontrado");
        }
        planRepository.deleteById(id);
    }
}
