package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.DeleveragingProjectionDto;
import br.dev.xb.isperp.dto.financial.SimulationRequest;
import br.dev.xb.isperp.dto.financial.SimulationResponse;
import br.dev.xb.isperp.service.financial.DeleveragingEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/financial/deleveraging")
@RequiredArgsConstructor
@Tag(name = "Motor de Desalavancagem e Curva de Saída do Vermelho", description = "Projeção contínua de caixa para 36 meses, ponto do fundo do poço e simulador E Se")
public class DeleveragingController {

    private final DeleveragingEngineService deleveragingEngineService;

    @GetMapping("/projection")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Calcula a curva contínua de caixa para os próximos 36 meses com os 3 números sagrados")
    public ResponseEntity<DeleveragingProjectionDto> get36MonthsProjection() {
        return ResponseEntity.ok(deleveragingEngineService.calculate36MonthsProjection());
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Simula o impacto financeiro de um novo investimento parcelado no caixa e na data da alforria")
    public ResponseEntity<SimulationResponse> simulateNewInvestment(@Valid @RequestBody SimulationRequest request) {
        return ResponseEntity.ok(deleveragingEngineService.simulateNewInvestment(request));
    }
}
