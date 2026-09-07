package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.CustomersApi;
import br.dev.xb.isperp.api.dto.CustomerCreateRequest;
import br.dev.xb.isperp.api.dto.CustomerResponse;
import br.dev.xb.isperp.api.dto.CustomerUpdateRequest;
import br.dev.xb.isperp.api.dto.ResetCustomerPinRequest;
import br.dev.xb.isperp.api.dto.ResetCustomerPinResponse;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.mapper.CustomerMapper;
import br.dev.xb.isperp.service.AuditLogService;
import br.dev.xb.isperp.service.ClientPortalService;
import br.dev.xb.isperp.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CustomerController implements CustomersApi {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    private final ClientPortalService clientPortalService;
    private final AuditLogService auditLogService;

    @Override
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerMapper.toResponseList(customerService.getAllCustomers()));
    }

    @Override
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {
        return ResponseEntity.ok(customerMapper.toResponseList(customerService.getActiveCustomers()));
    }

    @Override
    public ResponseEntity<CustomerResponse> getCustomerById(UUID id) {
        return customerService.getCustomerById(id)
                .map(customerMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<CustomerResponse> getCustomerByCpf(String cpf) {
        return customerService.getCustomerByCpf(cpf)
                .map(customerMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<CustomerResponse>> searchCustomers(String q, String name, String cpf) {
        if (cpf != null && !cpf.isBlank()) {
            return ResponseEntity.ok(customerMapper.toResponseList(customerService.searchCustomersByCpf(cpf)));
        }
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(customerMapper.toResponseList(customerService.searchCustomersByName(name)));
        }
        if (q != null && !q.isBlank()) {
            String cleanDigits = q.replaceAll("\\D", "");
            if (cleanDigits.length() >= 3) {
                List<Customer> byCpf = customerService.searchCustomersByCpf(cleanDigits);
                if (!byCpf.isEmpty()) {
                    return ResponseEntity.ok(customerMapper.toResponseList(byCpf));
                }
            }
            return ResponseEntity.ok(customerMapper.toResponseList(customerService.searchCustomersByName(q)));
        }
        return ResponseEntity.ok(customerMapper.toResponseList(customerService.getAllCustomers()));
    }

    @Override
    public ResponseEntity<List<CustomerResponse>> searchCustomersByName(String name) {
        return ResponseEntity.ok(customerMapper.toResponseList(customerService.searchCustomersByName(name)));
    }

    @Override
    public ResponseEntity<List<CustomerResponse>> searchCustomersByCpf(String cpf) {
        return ResponseEntity.ok(customerMapper.toResponseList(customerService.searchCustomersByCpf(cpf)));
    }

    @Override
    public ResponseEntity<CustomerResponse> createCustomer(CustomerCreateRequest request) {
        Customer entity = customerMapper.toEntity(request);
        Customer created = customerService.createCustomer(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<CustomerResponse> updateCustomer(UUID id, CustomerUpdateRequest request) {
        Customer customerDetails = new Customer();
        customerMapper.updateEntityFromRequest(request, customerDetails);
        Customer updated = customerService.updateCustomer(id, customerDetails);
        return ResponseEntity.ok(customerMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteCustomer(UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> activateCustomer(UUID id) {
        customerService.activateCustomer(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deactivateCustomer(UUID id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Reseta ou define um PIN temporário para o cliente pelo atendente/administrador.
     */
    @Override
    public ResponseEntity<ResetCustomerPinResponse> resetCustomerPin(
            UUID id,
            ResetCustomerPinRequest request) {

        boolean force = request.getForceChange() == null || Boolean.TRUE.equals(request.getForceChange());
        clientPortalService.resetPinByOperator(id, request.getTemporaryPin(), force);

        auditLogService.logAction(
                "CUSTOMER_PIN_RESET",
                "CUSTOMER",
                id.toString(),
                Map.of("forceChange", force)
        );

        ResetCustomerPinResponse response = new ResetCustomerPinResponse();
        response.setMessage("PIN do cliente resetado com sucesso pelo operador.");
        response.setCustomerId(id);
        response.setForceChange(force);

        return ResponseEntity.ok(response);
    }
}