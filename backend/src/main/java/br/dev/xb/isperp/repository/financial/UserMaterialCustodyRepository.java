package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.MaterialType;
import br.dev.xb.isperp.entity.financial.UserMaterialCustody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMaterialCustodyRepository extends JpaRepository<UserMaterialCustody, UUID> {
    List<UserMaterialCustody> findByUserId(UUID userId);
    List<UserMaterialCustody> findByUserIdAndItemType(UUID userId, MaterialType itemType);
    Optional<UserMaterialCustody> findBySerialNumber(String serialNumber);
}
