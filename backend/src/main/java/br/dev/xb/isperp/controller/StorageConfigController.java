package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.StorageConfigRequest;
import br.dev.xb.isperp.dto.StorageConfigResponse;
import br.dev.xb.isperp.dto.StorageConnectionTestResponse;
import br.dev.xb.isperp.service.StorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/storage/config")
@RequiredArgsConstructor
@Tag(name = "Configuração de Armazenamento S3", description = "Endpoints para gerenciamento e teste de conectividade de S3 local (SeaweedFS) e provedores em nuvem")
@SuppressWarnings("null")
public class StorageConfigController {

    private final StorageConfigService storageConfigService;

    @GetMapping
    @Operation(summary = "Obtém a configuração ativa de armazenamento S3/Local")
    public ResponseEntity<StorageConfigResponse> getActiveConfig() {
        return ResponseEntity.ok(storageConfigService.getActiveConfig());
    }

    @PutMapping
    @Operation(summary = "Atualiza a configuração de armazenamento S3/Local")
    public ResponseEntity<StorageConfigResponse> saveConfig(@Valid @RequestBody StorageConfigRequest request) {
        return ResponseEntity.ok(storageConfigService.saveOrUpdate(request));
    }

    @PostMapping("/test")
    @Operation(summary = "Testa a conectividade com o endpoint S3 informado antes de salvar")
    public ResponseEntity<StorageConnectionTestResponse> testConnection(@RequestBody StorageConfigRequest request) {
        return ResponseEntity.ok(storageConfigService.testConnection(request));
    }
}
