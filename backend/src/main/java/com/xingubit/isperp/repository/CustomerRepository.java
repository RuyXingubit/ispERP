package com.xingubit.isperp.repository;

import com.xingubit.isperp.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByCpf(String cpf);
    
    Optional<Customer> findByEmail(String email);
    
    boolean existsByCpf(String cpf);
    
    boolean existsByEmail(String email);
    
    List<Customer> findByActiveTrue();
    
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    List<Customer> findByCpfContaining(String cpf);
}