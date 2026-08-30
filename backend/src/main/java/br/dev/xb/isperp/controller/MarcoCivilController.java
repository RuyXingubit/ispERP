package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.MarcoCivilReportRequest;
import br.dev.xb.isperp.dto.MarcoCivilReportResponse;
import br.dev.xb.isperp.dto.MarcoCivilSearchRequest;
import br.dev.xb.isperp.dto.MarcoCivilSearchResult;
import br.dev.xb.isperp.service.MarcoCivilInvestigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marco-civil")
@RequiredArgsConstructor
@Tag(name = "Marco Civil da Internet (Lei 12.965/2014)", description = "Investigação Forense, Identificação de Assinante e Laudos Oficiais")
public class MarcoCivilController {

    private final MarcoCivilInvestigationService marcoCivilInvestigationService;

    @PostMapping("/search")
    @Operation(summary = "Busca forense reversa para identificar assinante por IP, Porta e Data/Hora")
    public ResponseEntity<MarcoCivilSearchResult> searchSubscriber(@Valid @RequestBody MarcoCivilSearchRequest request) {
        return ResponseEntity.ok(marcoCivilInvestigationService.searchSubscriber(request));
    }

    @PostMapping("/reports")
    @Operation(summary = "Emite laudo pericial oficial com token de validação pública e assinatura criptográfica SHA-256")
    public ResponseEntity<MarcoCivilReportResponse> generateOfficialReport(@Valid @RequestBody MarcoCivilReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marcoCivilInvestigationService.generateOfficialReport(request));
    }
}
