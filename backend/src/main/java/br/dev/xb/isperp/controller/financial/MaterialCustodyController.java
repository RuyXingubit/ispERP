package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.MaterialCustodyDto;
import br.dev.xb.isperp.dto.financial.MaterialTransferRequest;
import br.dev.xb.isperp.dto.financial.MaterialTransferResponseDto;
import br.dev.xb.isperp.service.financial.MaterialCustodyService;
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
@RequestMapping("/api/financial/custody/materials")
@RequiredArgsConstructor
@Tag(name = "Custódia Material por CPF", description = "Endpoints para controle de carga patrimonial de equipamentos e ferramentas no CPF do técnico")
public class MaterialCustodyController {

    private final MaterialCustodyService materialCustodyService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lista equipamentos e ferramentas sob custódia de um técnico específico")
    public ResponseEntity<List<MaterialCustodyDto>> getMaterialsByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(materialCustodyService.getMaterialsByUserId(userId));
    }

    @PostMapping("/user/{userId}/allocate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL', 'SUPPORT_N2')")
    @Operation(summary = "Almoxarifado aloca itens e seriais diretamente na carga do CPF do colaborador")
    public ResponseEntity<MaterialCustodyDto> allocateMaterial(
            @PathVariable UUID userId,
            @Valid @RequestBody MaterialCustodyDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialCustodyService.allocateMaterialToUser(userId, dto));
    }

    @PostMapping("/transfer/request")
    @Operation(summary = "Técnico solicita transferência de peças para outro técnico (Duplo Aceite na rua)")
    public ResponseEntity<MaterialTransferResponseDto> requestMaterialTransfer(
            @RequestHeader("X-User-Id") UUID senderUserId,
            @Valid @RequestBody MaterialTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialCustodyService.requestMaterialTransfer(senderUserId, request));
    }

    @PostMapping("/transfer/{id}/respond")
    @Operation(summary = "Técnico recebedor aceita ou rejeita a carga de material recebida")
    public ResponseEntity<MaterialTransferResponseDto> respondMaterialTransfer(
            @RequestHeader("X-User-Id") UUID receiverUserId,
            @PathVariable UUID id,
            @RequestParam boolean accept) {
        return ResponseEntity.ok(materialCustodyService.respondMaterialTransfer(receiverUserId, id, accept));
    }

    @GetMapping("/transfer/pending")
    @Operation(summary = "Lista transferências de equipamentos pendentes de aceite para o técnico logado")
    public ResponseEntity<List<MaterialTransferResponseDto>> getPendingTransfers(
            @RequestHeader("X-User-Id") UUID receiverUserId) {
        return ResponseEntity.ok(materialCustodyService.getPendingTransfersForReceiver(receiverUserId));
    }
}
