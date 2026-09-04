package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.PlansApi;
import br.dev.xb.isperp.api.dto.PlanCreateRequest;
import br.dev.xb.isperp.api.dto.PlanResponse;
import br.dev.xb.isperp.api.dto.PlanUpdateRequest;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.mapper.PlanMapper;
import br.dev.xb.isperp.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plans")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PlanController implements PlansApi {

    private final PlanService planService;
    private final PlanMapper planMapper;

    @Override
    @GetMapping
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planMapper.toResponseList(planService.getAllPlans()));
    }

    @Override
    @GetMapping("/active")
    public ResponseEntity<List<PlanResponse>> getActivePlans() {
        return ResponseEntity.ok(planMapper.toResponseList(planService.getActivePlans()));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable UUID id) {
        return planService.getPlanById(id)
                .map(planMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanCreateRequest request) {
        try {
            Plan entity = planMapper.toEntity(request);
            Plan created = planService.createPlan(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(planMapper.toResponse(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable UUID id, @Valid @RequestBody PlanUpdateRequest request) {
        try {
            Plan planDetails = new Plan();
            planMapper.updateEntityFromRequest(request, planDetails);
            Plan updated = planService.updatePlan(id, planDetails);
            return ResponseEntity.ok(planMapper.toResponse(updated));
        } catch (RuntimeException e) {
            if ("Plano não encontrado".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        try {
            planService.deletePlan(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
