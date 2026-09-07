package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.NasApi;
import br.dev.xb.isperp.api.dto.NasRequest;
import br.dev.xb.isperp.api.dto.NasResponse;
import br.dev.xb.isperp.api.dto.NasVendorType;
import br.dev.xb.isperp.service.NasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "RADIUS - NAS / BNG", description = "Gestão de Servidores NAS e Roteadores BNG do FreeRADIUS")
public class NasController implements NasApi {

    private final NasService nasService;

    @Override
    public ResponseEntity<List<NasResponse>> getAllNas() {
        List<NasResponse> list = nasService.getAllNas().stream()
                .map(this::toApi)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<NasResponse> getNasById(UUID id) {
        return ResponseEntity.ok(toApi(nasService.getNasById(id)));
    }

    @Override
    public ResponseEntity<NasResponse> createNas(NasRequest request) {
        br.dev.xb.isperp.dto.NasResponse created = nasService.createNas(fromApi(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toApi(created));
    }

    @Override
    public ResponseEntity<NasResponse> updateNas(UUID id, NasRequest request) {
        br.dev.xb.isperp.dto.NasResponse updated = nasService.updateNas(id, fromApi(request));
        return ResponseEntity.ok(toApi(updated));
    }

    @Override
    public ResponseEntity<Void> deleteNas(UUID id) {
        nasService.deleteNas(id);
        return ResponseEntity.noContent().build();
    }

    private NasResponse toApi(br.dev.xb.isperp.dto.NasResponse r) {
        if (r == null) return null;
        NasResponse resp = new NasResponse();
        resp.setId(r.getId());
        resp.setCompanyId(r.getCompanyId());
        resp.setNasname(r.getNasname());
        resp.setShortname(r.getShortname());
        resp.setType(r.getType());
        resp.setPorts(r.getPorts());
        resp.setSecret(r.getSecret());
        resp.setServer(r.getServer());
        resp.setCommunity(r.getCommunity());
        resp.setDescription(r.getDescription());
        if (r.getVendorType() != null) {
            resp.setVendorType(NasVendorType.fromValue(r.getVendorType().name()));
        }
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        return resp;
    }

    private br.dev.xb.isperp.dto.NasRequest fromApi(NasRequest req) {
        if (req == null) return null;
        return br.dev.xb.isperp.dto.NasRequest.builder()
                .companyId(req.getCompanyId())
                .nasname(req.getNasname())
                .shortname(req.getShortname())
                .type(req.getType())
                .ports(req.getPorts())
                .secret(req.getSecret())
                .server(req.getServer())
                .community(req.getCommunity())
                .description(req.getDescription())
                .vendorType(req.getVendorType() != null ? br.dev.xb.isperp.radius.NasVendorType.valueOf(req.getVendorType().getValue()) : br.dev.xb.isperp.radius.NasVendorType.MIKROTIK)
                .build();
    }
}
