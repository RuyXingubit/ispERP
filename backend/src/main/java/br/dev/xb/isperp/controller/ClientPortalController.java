package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.ClientPortalApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.mapper.ContractMapper;
import br.dev.xb.isperp.mapper.CustomerMapper;
import br.dev.xb.isperp.mapper.InvoiceMapper;
import br.dev.xb.isperp.mapper.PlanMapper;
import br.dev.xb.isperp.service.ClientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class ClientPortalController implements ClientPortalApi {

    private final ClientPortalService clientPortalService;
    private final CustomerMapper customerMapper;
    private final ContractMapper contractMapper;
    private final PlanMapper planMapper;
    private final InvoiceMapper invoiceMapper;

    /**
     * Autenticação do cliente por CPF/CNPJ com validação de PIN de 4 dígitos.
     */
    @Override
    public ResponseEntity<ClientAuthResponse> authenticateClientPortal(ClientAuthRequest request) {
        br.dev.xb.isperp.dto.ClientAuthRequest internalReq = br.dev.xb.isperp.dto.ClientAuthRequest.builder()
                .document(request.getDocument())
                .pin(request.getPin())
                .build();
        br.dev.xb.isperp.dto.ClientAuthResponse internalResp = clientPortalService.authenticateClient(internalReq);
        return ResponseEntity.ok(toApi(internalResp));
    }

    // Sobrecarga para compatibilidade interna
    public ResponseEntity<br.dev.xb.isperp.dto.ClientAuthResponse> authenticate(br.dev.xb.isperp.dto.ClientAuthRequest request) {
        return ResponseEntity.ok(clientPortalService.authenticateClient(request));
    }

    /**
     * Define ou atualiza o PIN de 4 dígitos do cliente.
     */
    @Override
    public ResponseEntity<PortalMessageResponse> setClientPortalPin(SetClientPinRequest request) {
        br.dev.xb.isperp.dto.SetClientPinRequest internalReq = br.dev.xb.isperp.dto.SetClientPinRequest.builder()
                .customerId(request.getCustomerId())
                .newPin(request.getNewPin())
                .currentPin(request.getCurrentPin())
                .build();
        clientPortalService.setPin(internalReq);
        return ResponseEntity.ok(new PortalMessageResponse("PIN de 4 dígitos configurado com sucesso."));
    }

    // Sobrecarga para compatibilidade com Map legado
    public ResponseEntity<Map<String, String>> setPin(br.dev.xb.isperp.dto.SetClientPinRequest request) {
        clientPortalService.setPin(request);
        return ResponseEntity.ok(Map.of("message", "PIN de 4 dígitos configurado com sucesso."));
    }

    /**
     * Retorna o dashboard completo do assinante.
     */
    @Override
    public ResponseEntity<ClientPortalDashboardResponse> getClientPortalDashboard(
            @Nullable UUID customerId,
            @Nullable UUID xCustomerId) {

        UUID targetId = resolveCustomerId(customerId, xCustomerId);
        br.dev.xb.isperp.dto.ClientPortalDashboardDTO dto = clientPortalService.getClientDashboard(targetId);

        ClientPortalDashboardResponse resp = new ClientPortalDashboardResponse();
        if (dto.getCustomer() != null) {
            resp.setCustomer(customerMapper.toResponse(dto.getCustomer()));
        }
        if (dto.getContract() != null) {
            resp.setContract(contractMapper.toResponse(dto.getContract()));
        }
        if (dto.getCurrentPlan() != null) {
            resp.setCurrentPlan(planMapper.toResponse(dto.getCurrentPlan()));
        }
        if (dto.getAvailableUpgradePlans() != null) {
            resp.setAvailableUpgradePlans(planMapper.toResponseList(dto.getAvailableUpgradePlans()));
        }
        if (dto.getPendingInvoices() != null) {
            resp.setPendingInvoices(invoiceMapper.toResponseList(dto.getPendingInvoices()));
        }
        if (dto.getPaidInvoices() != null) {
            resp.setPaidInvoices(invoiceMapper.toResponseList(dto.getPaidInvoices()));
        }
        if (dto.getOverdueInvoices() != null) {
            resp.setOverdueInvoices(invoiceMapper.toResponseList(dto.getOverdueInvoices()));
        }
        resp.setIsConnectionBlocked(dto.isConnectionBlocked());
        resp.setCanRequestTrustUnblock(dto.isCanRequestTrustUnblock());
        resp.setConnectionStatusMessage(dto.getConnectionStatusMessage());

        return ResponseEntity.ok(resp);
    }

    // Sobrecarga para dashboard com DTO interno
    public ResponseEntity<br.dev.xb.isperp.dto.ClientPortalDashboardDTO> getDashboard(
            @Nullable UUID customerId,
            @Nullable UUID headerCustomerId) {
        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        return ResponseEntity.ok(clientPortalService.getClientDashboard(targetId));
    }

    /**
     * Atualiza dados de contato do cliente.
     */
    @Override
    public ResponseEntity<CustomerResponse> updateClientPortalProfile(
            UpdateClientProfileRequest request,
            @Nullable UUID customerId,
            @Nullable UUID xCustomerId) {

        UUID targetId = resolveCustomerId(customerId, xCustomerId);
        br.dev.xb.isperp.dto.UpdateClientProfileRequest internalReq = br.dev.xb.isperp.dto.UpdateClientProfileRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .build();
        Customer updated = clientPortalService.updateProfile(targetId, internalReq);
        return ResponseEntity.ok(customerMapper.toResponse(updated));
    }

    // Sobrecarga para profile com entidade
    public ResponseEntity<Customer> updateProfile(
            @Nullable UUID customerId,
            @Nullable UUID headerCustomerId,
            br.dev.xb.isperp.dto.UpdateClientProfileRequest request) {
        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        return ResponseEntity.ok(clientPortalService.updateProfile(targetId, request));
    }

    /**
     * Altera a senha do assinante.
     */
    @Override
    public ResponseEntity<PortalMessageResponse> changeClientPortalPassword(
            ChangePasswordRequest request,
            @Nullable UUID customerId,
            @Nullable UUID xCustomerId) {

        UUID targetId = resolveCustomerId(customerId, xCustomerId);
        br.dev.xb.isperp.dto.ChangePasswordRequest internalReq = br.dev.xb.isperp.dto.ChangePasswordRequest.builder()
                .currentPassword(request.getCurrentPassword())
                .newPassword(request.getNewPassword())
                .build();
        clientPortalService.changePassword(targetId, internalReq);
        return ResponseEntity.ok(new PortalMessageResponse("Senha alterada com sucesso"));
    }

    // Sobrecarga para alteração de senha
    public ResponseEntity<Map<String, String>> changePassword(
            @Nullable UUID customerId,
            @Nullable UUID headerCustomerId,
            br.dev.xb.isperp.dto.ChangePasswordRequest request) {
        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        clientPortalService.changePassword(targetId, request);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }

    /**
     * Executa solicitação de Upgrade de Plano pelo cliente.
     */
    @Override
    public ResponseEntity<ContractResponse> upgradeClientPortalPlan(
            PlanUpgradeRequest request,
            @Nullable UUID customerId,
            @Nullable UUID xCustomerId) {

        UUID targetId = resolveCustomerId(customerId, xCustomerId);
        Contract updated = clientPortalService.requestPlanUpgrade(targetId, request.getContractId(), request.getNewPlanId());
        return ResponseEntity.ok(contractMapper.toResponse(updated));
    }

    // Sobrecarga com payload genérico Map
    public ResponseEntity<Contract> upgradePlan(
            @Nullable UUID customerId,
            @Nullable UUID headerCustomerId,
            Map<String, UUID> payload) {
        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        UUID contractId = payload.get("contractId");
        UUID newPlanId = payload.get("newPlanId");
        Contract updated = clientPortalService.requestPlanUpgrade(targetId, contractId, newPlanId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Solicita o Desbloqueio em Confiança (48h).
     */
    @Override
    public ResponseEntity<TrustUnblockResponse> requestClientPortalTrustUnblock(
            TrustUnblockRequest request,
            @Nullable UUID customerId,
            @Nullable UUID xCustomerId) {

        UUID targetId = resolveCustomerId(customerId, xCustomerId);
        TrustUnblock trustUnblock = clientPortalService.requestTrustUnblock(targetId, request.getContractId());
        return ResponseEntity.ok(toApi(trustUnblock));
    }

    // Sobrecarga com Map
    public ResponseEntity<TrustUnblock> requestTrustUnblock(
            @Nullable UUID customerId,
            @Nullable UUID headerCustomerId,
            Map<String, UUID> payload) {
        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        UUID contractId = payload.get("contractId");
        TrustUnblock trustUnblock = clientPortalService.requestTrustUnblock(targetId, contractId);
        return ResponseEntity.ok(trustUnblock);
    }

    private UUID resolveCustomerId(@Nullable UUID queryId, @Nullable UUID headerId) {
        if (queryId != null) return queryId;
        if (headerId != null) return headerId;

        // Sem fallback: acesso não identificado é estritamente rejeitado com 401 Unauthorized
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Acesso não autorizado. Identifique-se com seu CPF ou CNPJ.");
    }

    private ClientAuthResponse toApi(br.dev.xb.isperp.dto.ClientAuthResponse r) {
        if (r == null) return null;
        ClientAuthResponse resp = new ClientAuthResponse();
        if (r.getStatus() != null) {
            resp.setStatus(ClientAuthStatus.fromValue(r.getStatus()));
        }
        resp.setMessage(r.getMessage());
        resp.setCustomerId(r.getCustomerId());
        resp.setCustomerName(r.getCustomerName());
        resp.setMaskedDocument(r.getMaskedDocument());
        resp.setHasPin(r.getHasPin());
        if (r.getCustomer() != null) {
            resp.setCustomer(customerMapper.toResponse(r.getCustomer()));
        }
        return resp;
    }

    private TrustUnblockResponse toApi(TrustUnblock u) {
        if (u == null) return null;
        TrustUnblockResponse resp = new TrustUnblockResponse();
        resp.setId(u.getId());
        resp.setContractId(u.getContractId());
        if (u.getRequestedAt() != null) {
            resp.setRequestedAt(u.getRequestedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }
        if (u.getExpiresAt() != null) {
            resp.setExpiresAt(u.getExpiresAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }
        resp.setUnblockType(u.getUnblockType());
        resp.setGrantedByUserId(u.getGrantedByUserId());
        resp.setInvoiceId(u.getInvoiceId());
        resp.setStatus(u.getStatus());
        if (u.getCreatedAt() != null) {
            resp.setCreatedAt(u.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }
        return resp;
    }
}
