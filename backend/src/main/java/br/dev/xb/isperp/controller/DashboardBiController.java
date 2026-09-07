package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.DashboardBiApi;
import br.dev.xb.isperp.api.dto.DashboardBiResponse;
import br.dev.xb.isperp.dto.DashboardBiDTO;
import br.dev.xb.isperp.mapper.DashboardBiMapper;
import br.dev.xb.isperp.service.DashboardBiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class DashboardBiController implements DashboardBiApi {

    private final DashboardBiService dashboardBiService;
    private final DashboardBiMapper dashboardBiMapper;

    /**
     * Retorna o consolidado de métricas executivas e BI do provedor (MRR, Churn, Inadimplência, ARPU, NOC).
     */
    @Override
    public ResponseEntity<DashboardBiResponse> getDashboardBi() {
        DashboardBiDTO metrics = dashboardBiService.getDashboardMetrics();
        return ResponseEntity.ok(dashboardBiMapper.toOpenApiResponse(metrics));
    }
}
