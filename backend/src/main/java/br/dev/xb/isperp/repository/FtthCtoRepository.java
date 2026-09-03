package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FtthCto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FtthCtoRepository extends JpaRepository<FtthCto, UUID> {
    List<FtthCto> findAllByOrderByCreatedAtDesc();
    List<FtthCto> findByClosureId(UUID closureId);
    List<FtthCto> findByProjectId(UUID projectId);
}
