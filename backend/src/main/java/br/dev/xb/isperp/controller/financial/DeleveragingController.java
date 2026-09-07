package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.DeleveragingApi;
import br.dev.xb.isperp.api.dto.DeleveragingProjectionDto;
import br.dev.xb.isperp.api.dto.SimulationRequest;
import br.dev.xb.isperp.api.dto.SimulationResponse;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.DeleveragingEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeleveragingController implements DeleveragingApi {

    private final DeleveragingEngineService deleveragingEngineService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<DeleveragingProjectionDto> get36MonthsDeleveragingProjection() {
        var domainProjection = deleveragingEngineService.calculate36MonthsProjection();
        return ResponseEntity.ok(financialDomainMapper.toOpenApiDeleveragingProjection(domainProjection));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<SimulationResponse> simulateNewInvestment(SimulationRequest request) {
        var domainRequest = financialDomainMapper.toDomainSimulationRequest(request);
        var domainResponse = deleveragingEngineService.simulateNewInvestment(domainRequest);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiSimulationResponse(domainResponse));
    }
}
