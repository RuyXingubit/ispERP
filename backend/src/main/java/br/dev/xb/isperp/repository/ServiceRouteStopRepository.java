package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.ServiceRouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceRouteStopRepository extends JpaRepository<ServiceRouteStop, UUID> {

    List<ServiceRouteStop> findByRouteIdOrderBySequenceOrderAsc(UUID routeId);

    List<ServiceRouteStop> findByWorkOrderId(UUID workOrderId);
}
