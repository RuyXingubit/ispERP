package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findByStatusOrderByCreatedAtDesc(Sale.SaleStatus status);

    List<Sale> findByCustomerCpf(String customerCpf);
}
