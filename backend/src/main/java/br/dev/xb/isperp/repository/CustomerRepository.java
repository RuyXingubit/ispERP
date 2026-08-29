package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    Optional<Customer> findByCpf(String cpf);
    
    Optional<Customer> findByEmail(String email);
    
    boolean existsByCpf(String cpf);
    
    boolean existsByEmail(String email);
    
    List<Customer> findByActiveTrue();
    
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    List<Customer> findByCpfContaining(String cpf);
}