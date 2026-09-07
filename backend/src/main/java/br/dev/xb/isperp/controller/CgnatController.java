package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.CgnatApi;
import br.dev.xb.isperp.api.dto.CgnatMappingRequest;
import br.dev.xb.isperp.api.dto.CgnatMappingResponse;
import br.dev.xb.isperp.api.dto.CgnatScriptImportRequest;
import br.dev.xb.isperp.api.dto.CgnatScriptImportResponse;
import br.dev.xb.isperp.api.dto.NasVendorType;
import br.dev.xb.isperp.service.CgnatParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class CgnatController implements CgnatApi {

    private final CgnatParserService cgnatParserService;

    @Override
    @GetMapping({"/cgnat/mappings", "/api/cgnat/mappings"})
    public ResponseEntity<List<CgnatMappingResponse>> listCgnatMappings() {
        return ResponseEntity.ok(cgnatParserService.getAllMappings().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/cgnat/mappings/nas/{nasId}", "/api/cgnat/mappings/nas/{nasId}"})
    public ResponseEntity<List<CgnatMappingResponse>> listCgnatMappingsByNas(@PathVariable UUID nasId) {
        return ResponseEntity.ok(cgnatParserService.getMappingsByNas(nasId).stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping({"/cgnat/mappings", "/api/cgnat/mappings"})
    public ResponseEntity<CgnatMappingResponse> createCgnatMapping(@RequestBody CgnatMappingRequest request) {
        br.dev.xb.isperp.dto.CgnatMappingRequest internalReq = br.dev.xb.isperp.dto.CgnatMappingRequest.builder()
                .nasId(request.getNasId())
                .vendorType(request.getVendorType() != null ? br.dev.xb.isperp.radius.NasVendorType.valueOf(request.getVendorType().name()) : br.dev.xb.isperp.radius.NasVendorType.MIKROTIK)
                .publicIp(request.getPublicIp())
                .portStart(request.getPortStart())
                .portEnd(request.getPortEnd())
                .privateIpStart(request.getPrivateIpStart())
                .privateIpEnd(request.getPrivateIpEnd())
                .protocol(request.getProtocol() != null ? request.getProtocol() : "BOTH")
                .notes(request.getNotes())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(cgnatParserService.createMapping(internalReq)));
    }

    @Override
    @PostMapping({"/cgnat/import-script", "/api/cgnat/import-script"})
    public ResponseEntity<CgnatScriptImportResponse> importCgnatScript(@RequestBody CgnatScriptImportRequest request) {
        br.dev.xb.isperp.dto.CgnatScriptImportRequest internalReq = br.dev.xb.isperp.dto.CgnatScriptImportRequest.builder()
                .nasId(request.getNasId())
                .vendorType(request.getVendorType() != null ? br.dev.xb.isperp.radius.NasVendorType.valueOf(request.getVendorType().name()) : br.dev.xb.isperp.radius.NasVendorType.MIKROTIK)
                .scriptContent(request.getScriptContent())
                .replaceExisting(Boolean.TRUE.equals(request.getReplaceExisting()))
                .build();
        br.dev.xb.isperp.dto.CgnatScriptImportResponse internalRes = cgnatParserService.importScript(internalReq);

        CgnatScriptImportResponse res = new CgnatScriptImportResponse();
        res.setTotalParsed(internalRes.getTotalParsed());
        res.setTotalSaved(internalRes.getTotalSaved());
        res.setWarnings(internalRes.getWarnings());
        if (internalRes.getImportedMappings() != null) {
            res.setImportedMappings(internalRes.getImportedMappings().stream()
                    .map(this::toApiResponse)
                    .collect(Collectors.toList()));
        }
        return ResponseEntity.ok(res);
    }

    @Override
    @DeleteMapping({"/cgnat/mappings/{id}", "/api/cgnat/mappings/{id}"})
    public ResponseEntity<Void> deleteCgnatMapping(@PathVariable UUID id) {
        cgnatParserService.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }

    private CgnatMappingResponse toApiResponse(br.dev.xb.isperp.dto.CgnatMappingResponse internal) {
        if (internal == null) return null;
        CgnatMappingResponse res = new CgnatMappingResponse();
        res.setId(internal.getId());
        res.setNasId(internal.getNasId());
        res.setNasName(internal.getNasName());
        if (internal.getVendorType() != null) {
            res.setVendorType(NasVendorType.fromValue(internal.getVendorType().name()));
        }
        res.setPublicIp(internal.getPublicIp());
        res.setPortStart(internal.getPortStart());
        res.setPortEnd(internal.getPortEnd());
        res.setPrivateIpStart(internal.getPrivateIpStart());
        res.setPrivateIpEnd(internal.getPrivateIpEnd());
        res.setProtocol(internal.getProtocol());
        res.setNotes(internal.getNotes());
        res.setCreatedAt(internal.getCreatedAt());
        res.setUpdatedAt(internal.getUpdatedAt());
        return res;
    }
}
