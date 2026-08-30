package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Nas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NasRepository extends JpaRepository<Nas, UUID> {
    Optional<Nas> findByNasname(String nasname);
}
