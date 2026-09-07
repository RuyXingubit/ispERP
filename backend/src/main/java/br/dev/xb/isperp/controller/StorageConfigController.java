package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.StorageConfigApi;
import br.dev.xb.isperp.api.dto.StorageConfigRequest;
import br.dev.xb.isperp.api.dto.StorageConfigResponse;
import br.dev.xb.isperp.api.dto.StorageConnectionTestResponse;
import br.dev.xb.isperp.service.StorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;

@RestController
@RequestMapping({"/storage/config", "/api/storage/config"})
@RequiredArgsConstructor
@Tag(name = "Configuração de Armazenamento S3", description = "Endpoints para gerenciamento e teste de conectividade de S3 local (SeaweedFS) e provedores em nuvem")
@SuppressWarnings("null")
public class StorageConfigController implements StorageConfigApi {

    private final StorageConfigService storageConfigService;

    @Override
    @GetMapping
    @Operation(summary = "Obtém a configuração ativa de armazenamento S3/Local")
    public ResponseEntity<StorageConfigResponse> getActiveStorageConfig() {
        return ResponseEntity.ok(toApiResponse(storageConfigService.getActiveConfig()));
    }

    @Override
    @PutMapping
    @Operation(summary = "Atualiza a configuração de armazenamento S3/Local")
    public ResponseEntity<StorageConfigResponse> saveStorageConfig(@Valid @RequestBody StorageConfigRequest request) {
        br.dev.xb.isperp.dto.StorageConfigRequest internalReq = toInternalRequest(request);
        return ResponseEntity.ok(toApiResponse(storageConfigService.saveOrUpdate(internalReq)));
    }

    @Override
    @PostMapping("/test")
    @Operation(summary = "Testa a conectividade com o endpoint S3 informado antes de salvar")
    public ResponseEntity<StorageConnectionTestResponse> testStorageConnection(@RequestBody StorageConfigRequest request) {
        br.dev.xb.isperp.dto.StorageConfigRequest internalReq = toInternalRequest(request);
        br.dev.xb.isperp.dto.StorageConnectionTestResponse testResult = storageConfigService.testConnection(internalReq);
        StorageConnectionTestResponse resp = new StorageConnectionTestResponse();
        resp.setSuccess(testResult.isSuccess());
        resp.setMessage(testResult.getMessage());
        resp.setDetails(testResult.getDetails());
        resp.setLatencyMs((int) testResult.getLatencyMs());
        return ResponseEntity.ok(resp);
    }

    private br.dev.xb.isperp.dto.StorageConfigRequest toInternalRequest(StorageConfigRequest req) {
        return br.dev.xb.isperp.dto.StorageConfigRequest.builder()
                .companyId(req.getCompanyId())
                .storageType(req.getStorageType() != null ? br.dev.xb.isperp.storage.StorageType.valueOf(req.getStorageType().name()) : br.dev.xb.isperp.storage.StorageType.S3)
                .provider(req.getProvider() != null ? br.dev.xb.isperp.storage.StorageProvider.valueOf(req.getProvider().name()) : br.dev.xb.isperp.storage.StorageProvider.SEAWEEDFS_LOCAL)
                .endpointUrl(req.getEndpointUrl())
                .bucketName(req.getBucketName() != null ? req.getBucketName() : "isperp-files")
                .region(req.getRegion() != null ? req.getRegion() : "us-east-1")
                .accessKey(req.getAccessKey())
                .secretKey(req.getSecretKey())
                .pathStyleAccess(req.getPathStyleAccess() != null ? req.getPathStyleAccess() : true)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();
    }

    private StorageConfigResponse toApiResponse(br.dev.xb.isperp.dto.StorageConfigResponse res) {
        if (res == null) return null;
        StorageConfigResponse apiRes = new StorageConfigResponse();
        apiRes.setId(res.getId());
        apiRes.setCompanyId(res.getCompanyId());
        if (res.getStorageType() != null) {
            apiRes.setStorageType(br.dev.xb.isperp.api.dto.StorageType.fromValue(res.getStorageType().name()));
        }
        if (res.getProvider() != null) {
            apiRes.setProvider(br.dev.xb.isperp.api.dto.StorageProvider.fromValue(res.getProvider().name()));
        }
        apiRes.setEndpointUrl(res.getEndpointUrl());
        apiRes.setBucketName(res.getBucketName());
        apiRes.setRegion(res.getRegion());
        apiRes.setAccessKey(res.getAccessKey());
        apiRes.setMaskedSecretKey(res.getMaskedSecretKey());
        apiRes.setPathStyleAccess(res.getPathStyleAccess());
        apiRes.setIsActive(res.getIsActive());
        if (res.getCreatedAt() != null) {
            apiRes.setCreatedAt(res.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (res.getUpdatedAt() != null) {
            apiRes.setUpdatedAt(res.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return apiRes;
    }
}
