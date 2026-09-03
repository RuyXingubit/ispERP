package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.AccountingMethod;
import br.dev.xb.isperp.dto.financial.DreReportDto;
import br.dev.xb.isperp.service.financial.DreReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/financial/reports/dre")
@RequiredArgsConstructor
@Tag(name = "DRE Telecom em Tempo Real", description = "Demonstração do Resultado do Exercício com EBITDA e Margens em Regime de Competência e Caixa")
public class DreReportController {

    private final DreReportService dreReportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Gera o DRE oficial consolidado para o período informado")
    public ResponseEntity<DreReportDto> getDreReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ACCRUAL") AccountingMethod method) {
        return ResponseEntity.ok(dreReportService.generateDre(startDate, endDate, method));
    }
}
