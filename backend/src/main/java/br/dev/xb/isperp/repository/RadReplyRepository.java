package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.RadReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RadReplyRepository extends JpaRepository<RadReply, UUID> {
    List<RadReply> findByUsername(String username);
    void deleteByUsername(String username);
}
