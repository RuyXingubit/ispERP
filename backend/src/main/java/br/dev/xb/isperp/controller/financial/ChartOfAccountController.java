package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.ChartOfAccountDto;
import br.dev.xb.isperp.service.financial.ChartOfAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/financial/chart-of-accounts")
@RequiredArgsConstructor
@Tag(name = "Plano de Contas Dinâmico", description = "Endpoints para manutenção da árvore hierárquica contábil de telecomunicações")
public class ChartOfAccountController {

    private final ChartOfAccountService chartOfAccountService;

    @GetMapping("/tree")
    @Operation(summary = "Retorna a árvore hierárquica completa do plano de contas de 5 níveis")
    public ResponseEntity<List<ChartOfAccountDto>> getTree() {
        return ResponseEntity.ok(chartOfAccountService.getTree());
    }

    @GetMapping
    @Operation(summary = "Lista todas as contas contábeis de forma plana")
    public ResponseEntity<List<ChartOfAccountDto>> getAllFlat() {
        return ResponseEntity.ok(chartOfAccountService.getAllFlat());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Cadastra uma nova conta contábil analítica ou sintética")
    public ResponseEntity<ChartOfAccountDto> createAccount(@Valid @RequestBody ChartOfAccountDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chartOfAccountService.createAccount(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Atualiza uma conta contábil existente")
    public ResponseEntity<ChartOfAccountDto> updateAccount(@PathVariable UUID id, @Valid @RequestBody ChartOfAccountDto dto) {
        return ResponseEntity.ok(chartOfAccountService.updateAccount(id, dto));
    }
}
