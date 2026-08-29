package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByCode(String code);

    List<InventoryItem> findByCategory(String category);
}
