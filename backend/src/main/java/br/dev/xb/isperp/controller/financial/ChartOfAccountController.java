package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.ChartOfAccountsApi;
import br.dev.xb.isperp.api.dto.ChartOfAccountDto;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.ChartOfAccountService;
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
public class ChartOfAccountController implements ChartOfAccountsApi {

    private final ChartOfAccountService chartOfAccountService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    public ResponseEntity<List<ChartOfAccountDto>> getChartOfAccountsTree() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiChartOfAccountList(chartOfAccountService.getTree()));
    }

    @Override
    public ResponseEntity<List<ChartOfAccountDto>> getAllChartOfAccountsFlat() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiChartOfAccountList(chartOfAccountService.getAllFlat()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<ChartOfAccountDto> createChartOfAccount(ChartOfAccountDto dto) {
        var domainDto = financialDomainMapper.toDomainChartOfAccount(dto);
        var created = chartOfAccountService.createAccount(domainDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiChartOfAccount(created));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<ChartOfAccountDto> updateChartOfAccount(UUID id, ChartOfAccountDto dto) {
        var domainDto = financialDomainMapper.toDomainChartOfAccount(dto);
        var updated = chartOfAccountService.updateAccount(id, domainDto);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiChartOfAccount(updated));
    }
}
