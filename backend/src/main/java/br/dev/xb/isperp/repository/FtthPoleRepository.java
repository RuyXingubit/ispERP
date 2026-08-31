package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FtthPole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FtthPoleRepository extends JpaRepository<FtthPole, UUID> {
    List<FtthPole> findAllByOrderByCreatedAtDesc();
}
