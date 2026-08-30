package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.CgnatMappingRequest;
import br.dev.xb.isperp.dto.CgnatMappingResponse;
import br.dev.xb.isperp.dto.CgnatScriptImportRequest;
import br.dev.xb.isperp.dto.CgnatScriptImportResponse;
import br.dev.xb.isperp.service.CgnatParserService;
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
@RequestMapping("/api/cgnat")
@RequiredArgsConstructor
@Tag(name = "CGNAT & Mapeamento Forense", description = "Gestão de Blocos CGNAT e Importação Multi-Vendor")
public class CgnatController {

    private final CgnatParserService cgnatParserService;

    @GetMapping("/mappings")
    @Operation(summary = "Lista todos os blocos e mapeamentos de CGNAT cadastrados")
    public ResponseEntity<List<CgnatMappingResponse>> getAllMappings() {
        return ResponseEntity.ok(cgnatParserService.getAllMappings());
    }

    @GetMapping("/mappings/nas/{nasId}")
    @Operation(summary = "Lista mapeamentos CGNAT associados a um NAS específico")
    public ResponseEntity<List<CgnatMappingResponse>> getMappingsByNas(@PathVariable UUID nasId) {
        return ResponseEntity.ok(cgnatParserService.getMappingsByNas(nasId));
    }

    @PostMapping("/mappings")
    @Operation(summary = "Cadastra manualmente um mapeamento CGNAT")
    public ResponseEntity<CgnatMappingResponse> createMapping(@Valid @RequestBody CgnatMappingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cgnatParserService.createMapping(request));
    }

    @PostMapping("/import-script")
    @Operation(summary = "Importa e faz o parse de scripts de firewall (MikroTik, Huawei, A10, Cisco) ou CSV")
    public ResponseEntity<CgnatScriptImportResponse> importScript(@Valid @RequestBody CgnatScriptImportRequest request) {
        return ResponseEntity.ok(cgnatParserService.importScript(request));
    }

    @DeleteMapping("/mappings/{id}")
    @Operation(summary = "Remove um mapeamento CGNAT")
    public ResponseEntity<Void> deleteMapping(@PathVariable UUID id) {
        cgnatParserService.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }
}
