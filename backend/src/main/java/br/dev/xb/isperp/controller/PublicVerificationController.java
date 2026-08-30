package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.PublicValidationResponse;
import br.dev.xb.isperp.service.MarcoCivilInvestigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/marco-civil")
@RequiredArgsConstructor
@Tag(name = "Validação Pública - Marco Civil", description = "Validação de autenticidade de laudos periciais e QR Code")
public class PublicVerificationController {

    private final MarcoCivilInvestigationService marcoCivilInvestigationService;

    @GetMapping("/validate/{token}")
    @Operation(summary = "Valida a autenticidade de um laudo pericial a partir do token ou QR Code")
    public ResponseEntity<PublicValidationResponse> validateReport(@PathVariable String token) {
        return ResponseEntity.ok(marcoCivilInvestigationService.validatePublicToken(token));
    }
}
