package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.RadCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RadCheckRepository extends JpaRepository<RadCheck, UUID> {
    List<RadCheck> findByUsername(String username);
    Optional<RadCheck> findByUsernameAndAttribute(String username, String attribute);
    void deleteByUsername(String username);
}
