package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.ContractTemplateRequest;
import br.dev.xb.isperp.dto.ContractTemplateResponse;
import br.dev.xb.isperp.dto.ContractTemplateVariableInfo;
import br.dev.xb.isperp.service.ContractTemplateEngine;
import br.dev.xb.isperp.service.ContractTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContractTemplateController {

    private final ContractTemplateService templateService;
    private final ContractTemplateEngine templateEngine;

    @GetMapping
    public ResponseEntity<List<ContractTemplateResponse>> listTemplates(@RequestParam(required = false) UUID companyId) {
        return ResponseEntity.ok(templateService.listTemplates(companyId));
    }

    @GetMapping("/variables")
    public ResponseEntity<List<ContractTemplateVariableInfo>> getAvailableVariables() {
        return ResponseEntity.ok(templateService.getAvailableVariables());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractTemplateResponse> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @PostMapping
    public ResponseEntity<ContractTemplateResponse> createTemplate(@Valid @RequestBody ContractTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody ContractTemplateRequest request
    ) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ContractTemplateResponse> cloneTemplate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.cloneTemplate(id));
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, String>> previewTemplate(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        String rendered = templateEngine.render(content, null, null, null, null, null);
        return ResponseEntity.ok(Map.of("rendered", rendered));
    }
}
