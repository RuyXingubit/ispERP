package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.DashboardBiDTO;
import br.dev.xb.isperp.service.DashboardBiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bi")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardBiController {

    private final DashboardBiService dashboardBiService;

    /**
     * Retorna o consolidado de métricas executivas e BI do provedor (MRR, Churn, Inadimplência, ARPU, NOC).
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardBiDTO> getDashboardBi() {
        DashboardBiDTO metrics = dashboardBiService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }
}
