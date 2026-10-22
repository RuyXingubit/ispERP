package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.PlanCompositionItemDto;
import br.dev.xb.isperp.api.dto.PlanCreateRequest;
import br.dev.xb.isperp.api.dto.PlanResponse;
import br.dev.xb.isperp.api.dto.PlanUpdateRequest;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.PlanCompositionCategory;
import br.dev.xb.isperp.entity.PlanCompositionItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@SuppressWarnings("java:S2583")
public interface PlanMapper extends CommonMapperConverters {

    PlanResponse toResponse(Plan plan);

    List<PlanResponse> toResponseList(List<Plan> plans);

    @Mapping(target = "plan", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PlanCompositionItem toItemEntity(PlanCompositionItemDto dto);

    PlanCompositionItemDto toItemDto(PlanCompositionItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "scmAmount", ignore = true)
    @Mapping(target = "svaAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Plan toEntity(PlanCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "scmAmount", ignore = true)
    @Mapping(target = "svaAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(PlanUpdateRequest request, @MappingTarget Plan plan);

    @AfterMapping
    default void enrichPlanResponse(Plan source, @MappingTarget PlanResponse target) {
        if (source == null || target == null) return;
        double priceVal = source.getPrice() != null ? source.getPrice().doubleValue() : 0.0;
        if (target.getScmAmount() == null) {
            target.setScmAmount(source.getScmAmount() != null ? source.getScmAmount().doubleValue() : priceVal);
        }
        if (target.getSvaAmount() == null) {
            target.setSvaAmount(source.getSvaAmount() != null ? source.getSvaAmount().doubleValue() : 0.0);
        }
        if (target.getCompositionItems() == null || target.getCompositionItems().isEmpty()) {
            PlanCompositionItemDto defaultScm = new PlanCompositionItemDto(
                    "Conectividade Fibra Óptica SCM - " + (source.getName() != null ? source.getName() : "Plano"),
                    br.dev.xb.isperp.api.dto.PlanCompositionCategory.SCM,
                    priceVal
            );
            defaultScm.setFiscalCode("5307");
            target.setCompositionItems(List.of(defaultScm));
        }
    }
}

