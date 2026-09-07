package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.DreReportsApi;
import br.dev.xb.isperp.api.dto.AccountingMethod;
import br.dev.xb.isperp.api.dto.DreReportDto;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.DreReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DreReportController implements DreReportsApi {

    private final DreReportService dreReportService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<DreReportDto> getDreReport(LocalDate startDate, LocalDate endDate, AccountingMethod method) {
        var domainMethod = method != null ? financialDomainMapper.toDomainAccountingMethod(method) : br.dev.xb.isperp.dto.financial.AccountingMethod.ACCRUAL;
        var domainReport = dreReportService.generateDre(startDate, endDate, domainMethod);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiDreReport(domainReport));
    }
}
