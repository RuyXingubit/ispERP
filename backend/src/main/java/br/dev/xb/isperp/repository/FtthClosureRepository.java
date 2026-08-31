package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FtthClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FtthClosureRepository extends JpaRepository<FtthClosure, UUID> {
    List<FtthClosure> findAllByOrderByCreatedAtDesc();
}
