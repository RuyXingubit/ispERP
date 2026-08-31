package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FtthCable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FtthCableRepository extends JpaRepository<FtthCable, UUID> {
    List<FtthCable> findAllByOrderByCreatedAtDesc();
}
