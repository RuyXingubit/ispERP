package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.AccountType;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.entity.financial.DreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID> {
    Optional<ChartOfAccount> findByCode(String code);
    List<ChartOfAccount> findByParentIsNullOrderByCodeAsc();
    List<ChartOfAccount> findByParentIdOrderByCodeAsc(UUID parentId);
    List<ChartOfAccount> findByAccountType(AccountType accountType);
    List<ChartOfAccount> findByDreCategory(DreCategory dreCategory);
}
