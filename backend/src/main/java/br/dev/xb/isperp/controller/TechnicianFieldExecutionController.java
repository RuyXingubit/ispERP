package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.TechnicianExecutionApi;
import br.dev.xb.isperp.api.dto.OltUnprovisionedOnu;
import br.dev.xb.isperp.api.dto.ProvisionOnu200Response;
import br.dev.xb.isperp.api.dto.ProvisionOnuRequest;
import br.dev.xb.isperp.api.dto.RadiusStatusResponse;
import br.dev.xb.isperp.dto.OltUnprovisionedOnuResponse;
import br.dev.xb.isperp.dto.TechnicianExecutionCompleteRequest;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.service.TechnicianFieldExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class TechnicianFieldExecutionController implements TechnicianExecutionApi {

    private final TechnicianFieldExecutionService fieldExecutionService;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    public ResponseEntity<List<OltUnprovisionedOnu>> listUnprovisionedOnus(UUID workOrderId) {
        List<OltUnprovisionedOnuResponse> onus = fieldExecutionService.listUnprovisionedOnus(workOrderId);
        List<OltUnprovisionedOnu> mapped = onus.stream().map(o -> {
            OltUnprovisionedOnu dto = new OltUnprovisionedOnu();
            dto.setNetworkDeviceId(o.getNetworkDeviceId());
            dto.setOltName(o.getOltName());
            dto.setSlotNumber(o.getSlotNumber());
            dto.setPortNumber(o.getPortNumber());
            dto.setPonName(o.getPonName());
            dto.setOnuSerial(o.getOnuSerial());
            dto.setOnuMac(o.getOnuMac());
            dto.setRxPowerDbm(o.getRxPowerDbm() != null ? o.getRxPowerDbm().doubleValue() : null);
            dto.setDetectedAt(o.getDetectedAt());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(mapped);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    public ResponseEntity<ProvisionOnu200Response> provisionOnu(UUID workOrderId, ProvisionOnuRequest request) {
        Map<String, Object> result = fieldExecutionService.provisionOnu(
                workOrderId,
                request.getOnuSerial(),
                request.getVlanId(),
                request.getPppoeUsername(),
                request.getPppoePassword()
        );
        Boolean success = (Boolean) result.getOrDefault("success", true);
        String message = (String) result.getOrDefault("message", "ONU provisionada com sucesso.");
        return ResponseEntity.ok(new ProvisionOnu200Response(success, message));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    public ResponseEntity<RadiusStatusResponse> getRadiusStatus(UUID workOrderId) {
        Map<String, Object> result = fieldExecutionService.checkRadiusSessionStatus(workOrderId);
        Boolean authenticated = (Boolean) result.getOrDefault("authenticated", false);
        RadiusStatusResponse response = new RadiusStatusResponse(workOrderId, authenticated);
        response.setUsername((String) result.get("username"));
        response.setFramedIpAddress((String) result.get("framedIpAddress"));
        response.setMacAddress((String) result.get("macAddress"));
        response.setNasIpAddress((String) result.get("nasIpAddress"));
        if (result.get("uptimeSeconds") instanceof Number n) {
            response.setUptimeSeconds(n.intValue());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/technician/execution/{workOrderId}/complete", "/api/technician/execution/{workOrderId}/complete"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    public ResponseEntity<WorkOrder> completeInstallation(
            @PathVariable UUID workOrderId,
            @Valid @RequestBody TechnicianExecutionCompleteRequest request) {
        return ResponseEntity.ok(fieldExecutionService.completeInstallation(workOrderId, request));
    }
}
