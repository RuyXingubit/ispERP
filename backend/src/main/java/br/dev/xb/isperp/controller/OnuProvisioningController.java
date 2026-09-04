package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.OnusApi;
import br.dev.xb.isperp.api.dto.OnuResponse;
import br.dev.xb.isperp.api.dto.OnuStatusResponse;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.mapper.OnuMapper;
import br.dev.xb.isperp.service.NetworkProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OnuProvisioningController implements OnusApi {

    private final NetworkProvisioningService provisioningService;
    private final OnuMapper onuMapper;

    @Override
    public ResponseEntity<List<OnuResponse>> getAllOnus() {
        return ResponseEntity.ok(onuMapper.toResponseList(provisioningService.getAllProvisionings()));
    }

    @Override
    public ResponseEntity<OnuResponse> getOnuById(UUID id) {
        Optional<OnuProvisioning> onu = provisioningService.getProvisioningById(id);
        return onu.map(onuMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<OnuResponse> getOnuByContractId(UUID contractId) {
        Optional<OnuProvisioning> onu = provisioningService.getProvisioningByContractId(contractId);
        return onu.map(onuMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<OnuResponse> blockOnu(UUID contractId, String reason) {
        OnuProvisioning blocked = provisioningService.blockInternetAccess(contractId, reason != null ? reason : "Inadimplência");
        return ResponseEntity.ok(onuMapper.toResponse(blocked));
    }

    @Override
    public ResponseEntity<OnuResponse> unblockOnu(UUID contractId) {
        OnuProvisioning unblocked = provisioningService.unblockInternetAccess(contractId);
        return ResponseEntity.ok(onuMapper.toResponse(unblocked));
    }

    @Override
    public ResponseEntity<OnuStatusResponse> diagnoseOnu(UUID id) {
        br.dev.xb.isperp.network.dto.OnuStatusResponse status = provisioningService.diagnoseOnuSignal(id);
        return ResponseEntity.ok(onuMapper.toApiStatusResponse(status));
    }
}
