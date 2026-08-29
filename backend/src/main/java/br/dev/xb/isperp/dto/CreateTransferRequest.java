package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.entity.StockTransfer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequest {

    @NotNull(message = "Depósito de origem é obrigatório")
    private UUID originWarehouseId;

    @NotNull(message = "Depósito de destino é obrigatório")
    private UUID destinationWarehouseId;

    private UUID carrierUserId; // Colaborador / Portador responsável

    @NotBlank(message = "Nome do portador é obrigatório")
    private String carrierName;

    @NotBlank(message = "Documento do portador é obrigatório")
    private String carrierDocument;

    private StockTransfer.CarrierType carrierType;

    private List<UUID> assetIds; // Lista de ONTs / equipamentos serializados

    private String notes;
}
