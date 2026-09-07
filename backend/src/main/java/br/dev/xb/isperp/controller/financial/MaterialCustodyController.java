package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.MaterialCustodyApi;
import br.dev.xb.isperp.api.dto.MaterialCustodyDto;
import br.dev.xb.isperp.api.dto.MaterialTransferRequest;
import br.dev.xb.isperp.api.dto.MaterialTransferResponseDto;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.MaterialCustodyService;
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
public class MaterialCustodyController implements MaterialCustodyApi {

    private final MaterialCustodyService materialCustodyService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    public ResponseEntity<List<MaterialCustodyDto>> getMaterialsByUserId(UUID userId) {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiMaterialCustodyList(materialCustodyService.getMaterialsByUserId(userId)));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL', 'SUPPORT_N2')")
    public ResponseEntity<MaterialCustodyDto> allocateMaterialToUser(UUID userId, MaterialCustodyDto dto) {
        var domainDto = financialDomainMapper.toDomainMaterialCustody(dto);
        var allocated = materialCustodyService.allocateMaterialToUser(userId, domainDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiMaterialCustody(allocated));
    }

    @Override
    public ResponseEntity<MaterialTransferResponseDto> requestMaterialTransfer(UUID xUserId, MaterialTransferRequest request) {
        var domainRequest = financialDomainMapper.toDomainMaterialTransferRequest(request);
        var created = materialCustodyService.requestMaterialTransfer(xUserId, domainRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiMaterialTransferResponse(created));
    }

    @Override
    public ResponseEntity<MaterialTransferResponseDto> respondMaterialTransfer(UUID xUserId, UUID id, Boolean accept) {
        var responded = materialCustodyService.respondMaterialTransfer(xUserId, id, Boolean.TRUE.equals(accept));
        return ResponseEntity.ok(financialDomainMapper.toOpenApiMaterialTransferResponse(responded));
    }

    @Override
    public ResponseEntity<List<MaterialTransferResponseDto>> getPendingMaterialTransfers(UUID xUserId) {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiMaterialTransferResponseList(materialCustodyService.getPendingTransfersForReceiver(xUserId)));
    }
}
