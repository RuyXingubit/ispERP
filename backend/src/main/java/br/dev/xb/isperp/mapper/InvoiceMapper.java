package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.InvoiceCompositionItemDto;
import br.dev.xb.isperp.api.dto.InvoiceResponse;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.InvoiceCompositionItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ValueMapping;

import java.util.List;

@Mapper(componentModel = "spring")
@SuppressWarnings("java:S2583")
public interface InvoiceMapper extends CommonMapperConverters {

    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponseList(List<Invoice> invoices);

    InvoiceCompositionItemDto toCompositionItemDto(InvoiceCompositionItem item);

    @AfterMapping
    default void enrichInvoiceResponse(Invoice source, @MappingTarget InvoiceResponse target) {
        if (source == null || target == null) return;
        double amtVal = source.getAmount() != null ? source.getAmount().doubleValue() : 0.0;
        if (target.getScmAmount() == null) {
            target.setScmAmount(source.getScmAmount() != null ? source.getScmAmount().doubleValue() : amtVal);
        }
        if (target.getSvaAmount() == null) {
            target.setSvaAmount(source.getSvaAmount() != null ? source.getSvaAmount().doubleValue() : 0.0);
        }
        if (target.getCompositionItems() == null || target.getCompositionItems().isEmpty()) {
            InvoiceCompositionItemDto defaultScm = new InvoiceCompositionItemDto(
                    "Conectividade Fibra Óptica SCM",
                    br.dev.xb.isperp.api.dto.PlanCompositionCategory.SCM,
                    amtVal
            );
            defaultScm.setFiscalCode("5307");
            target.setCompositionItems(List.of(defaultScm));
        }
    }

    @ValueMapping(source = "CANCELED", target = "CANCELLED")
    br.dev.xb.isperp.api.dto.InvoiceStatus toDtoStatus(Invoice.InvoiceStatus status);

    @ValueMapping(source = "CANCELLED", target = "CANCELED")
    Invoice.InvoiceStatus toEntityStatus(br.dev.xb.isperp.api.dto.InvoiceStatus status);
}

