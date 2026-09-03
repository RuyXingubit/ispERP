package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.NetworkProjectPaybackDto;
import br.dev.xb.isperp.dto.financial.NetworkProjectRequest;
import br.dev.xb.isperp.entity.financial.NetworkProject;
import br.dev.xb.isperp.service.financial.NetworkProjectService;
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
@RequestMapping("/api/financial/network-projects")
@RequiredArgsConstructor
@Tag(name = "Payback de Projetos de Rede (Mapa de Guerra)", description = "Associação de CTOs a centros de custo de expansão, cálculo de payback por bairro e direcionador comercial")
public class NetworkProjectController {

    private final NetworkProjectService projectService;

    @GetMapping("/payback")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Retorna todos os projetos de expansão com cálculo de payback acumulado e direcionador comercial de vendas")
    public ResponseEntity<List<NetworkProjectPaybackDto>> getAllProjectsWithPayback() {
        return ResponseEntity.ok(projectService.getAllProjectsWithPayback());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Cadastra um novo projeto de expansão de rede para acompanhamento de CAPEX")
    public ResponseEntity<NetworkProject> createProject(@Valid @RequestBody NetworkProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PostMapping("/{projectId}/assign-cto/{ctoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Vincula uma CTO instalada na rua ao projeto de rede correspondente")
    public ResponseEntity<Void> assignCtoToProject(@PathVariable UUID projectId, @PathVariable UUID ctoId) {
        projectService.assignCtoToProject(ctoId, projectId);
        return ResponseEntity.noContent().build();
    }
}
