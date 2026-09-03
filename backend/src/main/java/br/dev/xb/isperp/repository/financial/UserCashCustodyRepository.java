package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.UserCashCustody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCashCustodyRepository extends JpaRepository<UserCashCustody, UUID> {
    Optional<UserCashCustody> findByUserId(UUID userId);
    Optional<UserCashCustody> findByCpf(String cpf);
}
