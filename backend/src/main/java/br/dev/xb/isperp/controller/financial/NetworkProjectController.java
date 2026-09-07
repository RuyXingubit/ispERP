package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.NetworkProjectsApi;
import br.dev.xb.isperp.api.dto.NetworkProjectPaybackDto;
import br.dev.xb.isperp.api.dto.NetworkProjectRequest;
import br.dev.xb.isperp.api.dto.NetworkProjectResponse;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.NetworkProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NetworkProjectController implements NetworkProjectsApi {

    private final NetworkProjectService projectService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<List<NetworkProjectPaybackDto>> getAllProjectsWithPayback() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiNetworkProjectPaybackList(projectService.getAllProjectsWithPayback()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<NetworkProjectResponse> createNetworkProject(NetworkProjectRequest request) {
        var domainRequest = financialDomainMapper.toDomainNetworkProjectRequest(request);
        var created = projectService.createProject(domainRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiNetworkProjectResponse(created));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<Void> assignCtoToProject(UUID projectId, UUID ctoId) {
        projectService.assignCtoToProject(ctoId, projectId);
        return ResponseEntity.noContent().build();
    }
}
