package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.ContractTemplatesApi;
import br.dev.xb.isperp.api.dto.ContractTemplateRequest;
import br.dev.xb.isperp.api.dto.ContractTemplateResponse;
import br.dev.xb.isperp.api.dto.ContractTemplateVariableInfo;
import br.dev.xb.isperp.api.dto.DocumentType;
import br.dev.xb.isperp.api.dto.TemplatePreviewRequest;
import br.dev.xb.isperp.api.dto.TemplatePreviewResponse;
import br.dev.xb.isperp.service.ContractTemplateEngine;
import br.dev.xb.isperp.service.ContractTemplateService;
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
public class ContractTemplateController implements ContractTemplatesApi {

    private final ContractTemplateService templateService;
    private final ContractTemplateEngine templateEngine;

    @Override
    @GetMapping({"/contracts/templates", "/api/contracts/templates"})
    public ResponseEntity<List<ContractTemplateResponse>> listContractTemplates(@RequestParam(required = false) UUID companyId) {
        return ResponseEntity.ok(templateService.listTemplates(companyId).stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/contracts/templates/variables", "/api/contracts/templates/variables"})
    public ResponseEntity<List<ContractTemplateVariableInfo>> getContractTemplateVariables() {
        return ResponseEntity.ok(templateService.getAvailableVariables().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/contracts/templates/{id}", "/api/contracts/templates/{id}"})
    public ResponseEntity<ContractTemplateResponse> getContractTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiResponse(templateService.getTemplateById(id)));
    }

    @Override
    @PostMapping({"/contracts/templates", "/api/contracts/templates"})
    public ResponseEntity<ContractTemplateResponse> createContractTemplate(@RequestBody ContractTemplateRequest request) {
        br.dev.xb.isperp.dto.ContractTemplateRequest internalReq = br.dev.xb.isperp.dto.ContractTemplateRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .documentType(request.getDocumentType() != null
                        ? br.dev.xb.isperp.signature.DocumentType.valueOf(request.getDocumentType().name())
                        : null)
                .version(request.getVersion())
                .isActive(request.getIsActive())
                .contentMarkdown(request.getContentMarkdown())
                .consentClause(request.getConsentClause())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(templateService.createTemplate(internalReq)));
    }

    @Override
    @PutMapping({"/contracts/templates/{id}", "/api/contracts/templates/{id}"})
    public ResponseEntity<ContractTemplateResponse> updateContractTemplate(
            @PathVariable UUID id,
            @RequestBody ContractTemplateRequest request
    ) {
        br.dev.xb.isperp.dto.ContractTemplateRequest internalReq = br.dev.xb.isperp.dto.ContractTemplateRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .documentType(request.getDocumentType() != null
                        ? br.dev.xb.isperp.signature.DocumentType.valueOf(request.getDocumentType().name())
                        : null)
                .version(request.getVersion())
                .isActive(request.getIsActive())
                .contentMarkdown(request.getContentMarkdown())
                .consentClause(request.getConsentClause())
                .build();
        return ResponseEntity.ok(toApiResponse(templateService.updateTemplate(id, internalReq)));
    }

    @Override
    @DeleteMapping({"/contracts/templates/{id}", "/api/contracts/templates/{id}"})
    public ResponseEntity<Void> deleteContractTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping({"/contracts/templates/{id}/clone", "/api/contracts/templates/{id}/clone"})
    public ResponseEntity<ContractTemplateResponse> cloneContractTemplate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(templateService.cloneTemplate(id)));
    }

    @Override
    @PostMapping({"/contracts/templates/preview", "/api/contracts/templates/preview"})
    public ResponseEntity<TemplatePreviewResponse> previewContractTemplate(@RequestBody TemplatePreviewRequest request) {
        String content = request != null && request.getContent() != null ? request.getContent() : "";
        String rendered = templateEngine.render(content, null, null, null, null, null);
        TemplatePreviewResponse response = new TemplatePreviewResponse();
        response.setRendered(rendered);
        return ResponseEntity.ok(response);
    }

    private ContractTemplateResponse toApiResponse(br.dev.xb.isperp.dto.ContractTemplateResponse internal) {
        if (internal == null) return null;
        ContractTemplateResponse res = new ContractTemplateResponse();
        res.setId(internal.getId());
        res.setCompanyId(internal.getCompanyId());
        res.setName(internal.getName());
        if (internal.getDocumentType() != null) {
            res.setDocumentType(DocumentType.fromValue(internal.getDocumentType().name()));
        }
        res.setVersion(internal.getVersion());
        res.setIsActive(internal.getIsActive());
        res.setContentMarkdown(internal.getContentMarkdown());
        res.setConsentClause(internal.getConsentClause());
        res.setCreatedAt(internal.getCreatedAt());
        res.setUpdatedAt(internal.getUpdatedAt());
        return res;
    }

    private ContractTemplateVariableInfo toApiResponse(br.dev.xb.isperp.dto.ContractTemplateVariableInfo internal) {
        if (internal == null) return null;
        ContractTemplateVariableInfo res = new ContractTemplateVariableInfo();
        res.setTag(internal.getTag());
        res.setLabel(internal.getLabel());
        res.setCategory(internal.getCategory());
        res.setExample(internal.getExample());
        res.setDescription(internal.getDescription());
        return res;
    }
}
