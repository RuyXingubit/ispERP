package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FtthFusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FtthFusionRepository extends JpaRepository<FtthFusion, UUID> {
    List<FtthFusion> findByClosureId(UUID closureId);
    List<FtthFusion> findByClosureIdAndTrayNumber(UUID closureId, int trayNumber);
    Optional<FtthFusion> findBySourceCableIdAndSourceFiberNumber(UUID sourceCableId, int sourceFiberNumber);
    Optional<FtthFusion> findByTargetCableIdAndTargetFiberNumber(UUID targetCableId, int targetFiberNumber);
}
