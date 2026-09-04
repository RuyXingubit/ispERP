package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface InventoryMapper {

    // -------------------------------------------------------------------------
    // Catálogo de Insumos / Estoque
    // -------------------------------------------------------------------------
    InventoryItemResponse toResponse(InventoryItem item);

    List<InventoryItemResponse> toInventoryItemResponseList(List<InventoryItem> items);

    // -------------------------------------------------------------------------
    // Depósitos & Almoxarifados Físicos
    // -------------------------------------------------------------------------
    WarehouseResponse toResponse(Warehouse warehouse);

    List<WarehouseResponse> toWarehouseResponseList(List<Warehouse> warehouses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Warehouse toEntity(WarehouseCreateRequest request);

    // -------------------------------------------------------------------------
    // Ativos Serializados
    // -------------------------------------------------------------------------
    @Mapping(target = "category", expression = "java(toDtoAssetCategory(asset.getCategory()))")
    @Mapping(target = "status", expression = "java(toDtoAssetStatus(asset.getStatus()))")
    @Mapping(target = "replacementValue", expression = "java(bigDecimalToDouble(asset.getReplacementValue()))")
    SerializedAssetResponse toResponse(SerializedAsset asset);

    List<SerializedAssetResponse> toSerializedAssetResponseList(List<SerializedAsset> assets);

    // -------------------------------------------------------------------------
    // Transferências Inter-Bases
    // -------------------------------------------------------------------------
    @Mapping(target = "carrierType", expression = "java(toDtoCarrierType(transfer.getCarrierType()))")
    @Mapping(target = "status", expression = "java(toDtoTransferStatus(transfer.getStatus()))")
    StockTransferResponse toResponse(StockTransfer transfer);

    List<StockTransferResponse> toStockTransferResponseList(List<StockTransfer> transfers);

    @Mapping(target = "carrierType", expression = "java(toEntityCarrierType(request.getCarrierType()))")
    br.dev.xb.isperp.dto.CreateTransferRequest toServiceRequest(CreateTransferRequest request);

    // -------------------------------------------------------------------------
    // Termos de Cautela & Ferramental
    // -------------------------------------------------------------------------
    @Mapping(target = "status", expression = "java(toDtoAgreementStatus(agreement.getStatus()))")
    @Mapping(target = "totalPromissoryValue", expression = "java(bigDecimalToDouble(agreement.getTotalPromissoryValue()))")
    ToolCustodyAgreementResponse toResponse(ToolCustodyAgreement agreement);

    List<ToolCustodyAgreementResponse> toToolCustodyAgreementResponseList(List<ToolCustodyAgreement> agreements);

    @Mapping(target = "totalPromissoryValue", expression = "java(doubleToBigDecimal(request.getTotalPromissoryValue()))")
    br.dev.xb.isperp.dto.CheckoutToolRequest toServiceRequest(CheckoutToolRequest request);

    // -------------------------------------------------------------------------
    // Conversores de Enums
    // -------------------------------------------------------------------------
    default AssetCategory toDtoAssetCategory(SerializedAsset.AssetCategory category) {
        if (category == null) return null;
        return AssetCategory.valueOf(category.name());
    }

    default SerializedAsset.AssetCategory toEntityAssetCategory(AssetCategory category) {
        if (category == null) return null;
        return SerializedAsset.AssetCategory.valueOf(category.getValue());
    }

    default AssetStatus toDtoAssetStatus(SerializedAsset.AssetStatus status) {
        if (status == null) return null;
        return AssetStatus.valueOf(status.name());
    }

    default SerializedAsset.AssetStatus toEntityAssetStatus(AssetStatus status) {
        if (status == null) return null;
        return SerializedAsset.AssetStatus.valueOf(status.getValue());
    }

    default CarrierType toDtoCarrierType(StockTransfer.CarrierType type) {
        if (type == null) return null;
        return CarrierType.valueOf(type.name());
    }

    default StockTransfer.CarrierType toEntityCarrierType(CarrierType type) {
        if (type == null) return null;
        return StockTransfer.CarrierType.valueOf(type.getValue());
    }

    default TransferStatus toDtoTransferStatus(StockTransfer.TransferStatus status) {
        if (status == null) return null;
        return TransferStatus.valueOf(status.name());
    }

    default StockTransfer.TransferStatus toEntityTransferStatus(TransferStatus status) {
        if (status == null) return null;
        return StockTransfer.TransferStatus.valueOf(status.getValue());
    }

    default AgreementStatus toDtoAgreementStatus(ToolCustodyAgreement.AgreementStatus status) {
        if (status == null) return null;
        return AgreementStatus.valueOf(status.name());
    }

    default ToolCustodyAgreement.AgreementStatus toEntityAgreementStatus(AgreementStatus status) {
        if (status == null) return null;
        return ToolCustodyAgreement.AgreementStatus.valueOf(status.getValue());
    }

    // -------------------------------------------------------------------------
    // Conversores de Tipos Numéricos e Datas
    // -------------------------------------------------------------------------
    default Double bigDecimalToDouble(BigDecimal value) {
        if (value == null) return null;
        return value.doubleValue();
    }

    default BigDecimal doubleToBigDecimal(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value);
    }

    default OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    default LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.toLocalDateTime();
    }
}
