package com.xingubit.isperp.service;

import com.xingubit.isperp.entity.Customer;
import com.xingubit.isperp.repository.CustomerRepository;
import com.xingubit.isperp.util.CpfValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public List<Customer> getActiveCustomers() {
        return customerRepository.findByActiveTrue();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> getCustomerByCpf(String cpf) {
        return customerRepository.findByCpf(cpf);
    }

    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public List<Customer> searchCustomersByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Customer> searchCustomersByCpf(String cpf) {
        return customerRepository.findByCpfContaining(cpf);
    }

    public Customer createCustomer(Customer customer) {
        // Limpar e validar CPF
        String cleanCpf = CpfValidator.clean(customer.getCpf());
        if (!CpfValidator.isValid(cleanCpf)) {
            throw new RuntimeException("CPF inválido");
        }
        
        // Verificar se o CPF já existe
        if (customerRepository.existsByCpf(cleanCpf)) {
            throw new RuntimeException("CPF já cadastrado");
        }

        // Verificar se o email já existe (se fornecido)
        if (customer.getEmail() != null && !customer.getEmail().isEmpty() 
            && customerRepository.existsByEmail(customer.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }

        // Salvar com CPF limpo
        customer.setCpf(cleanCpf);
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Limpar e validar CPF
        String cleanCpf = CpfValidator.clean(customerDetails.getCpf());
        if (!CpfValidator.isValid(cleanCpf)) {
            throw new RuntimeException("CPF inválido");
        }

        // Verificar se o CPF já existe (exceto para o próprio cliente)
        Optional<Customer> existingCustomerByCpf = customerRepository.findByCpf(cleanCpf);
        if (existingCustomerByCpf.isPresent() && !existingCustomerByCpf.get().getId().equals(id)) {
            throw new RuntimeException("CPF já cadastrado");
        }

        // Verificar se o email já existe (exceto para o próprio cliente)
        if (customerDetails.getEmail() != null && !customerDetails.getEmail().isEmpty()) {
            Optional<Customer> existingCustomerByEmail = customerRepository.findByEmail(customerDetails.getEmail());
            if (existingCustomerByEmail.isPresent() && !existingCustomerByEmail.get().getId().equals(id)) {
                throw new RuntimeException("Email já cadastrado");
            }
        }

        customer.setName(customerDetails.getName());
        customer.setCpf(cleanCpf);
        customer.setEmail(customerDetails.getEmail());
        customer.setPhone(customerDetails.getPhone());
        customer.setAddress(customerDetails.getAddress());
        customer.setCity(customerDetails.getCity());
        customer.setState(customerDetails.getState());
        customer.setZipCode(customerDetails.getZipCode());
        customer.setActive(customerDetails.getActive());

        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        
        customerRepository.delete(customer);
    }

    public void deactivateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        
        customer.setActive(false);
        customerRepository.save(customer);
    }

    public void activateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        
        customer.setActive(true);
        customerRepository.save(customer);
    }
}