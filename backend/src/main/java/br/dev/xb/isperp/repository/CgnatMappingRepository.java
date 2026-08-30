package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.CgnatMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CgnatMappingRepository extends JpaRepository<CgnatMapping, UUID> {

    List<CgnatMapping> findByNasId(UUID nasId);

    // Busca de CGNAT para IP público e Porta
    @Query("""
        SELECT c FROM CgnatMapping c
        WHERE c.publicIp = :publicIp
          AND :port BETWEEN c.portStart AND c.portEnd
    """)
    List<CgnatMapping> findMatchingMappings(
            @Param("publicIp") String publicIp,
            @Param("port") int port
    );
}
