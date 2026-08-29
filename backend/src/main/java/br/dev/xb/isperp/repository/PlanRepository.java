package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByActiveTrue();

    List<Plan> findByActiveTrueOrderByIdDesc();

    List<Plan> findByNameContainingIgnoreCase(String name);
}
