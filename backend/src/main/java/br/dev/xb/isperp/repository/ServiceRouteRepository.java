package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.ServiceRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRouteRepository extends JpaRepository<ServiceRoute, UUID> {

    Optional<ServiceRoute> findByCode(String code);

    List<ServiceRoute> findByRouteDate(LocalDate routeDate);

    List<ServiceRoute> findByTechnicianUserIdAndRouteDate(UUID technicianUserId, LocalDate routeDate);
}
