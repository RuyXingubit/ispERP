package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.CustomerCreateRequest;
import br.dev.xb.isperp.api.dto.CustomerResponse;
import br.dev.xb.isperp.api.dto.CustomerUpdateRequest;
import br.dev.xb.isperp.entity.Customer;
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
public interface CustomerMapper extends CommonMapperConverters {

    CustomerResponse toResponse(Customer customer);

    List<CustomerResponse> toResponseList(List<Customer> customers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "isGovernment", constant = "false")
    @Mapping(target = "alwaysIssueNfcom", constant = "false")
    @Mapping(target = "portalPin", ignore = true)
    @Mapping(target = "pinForceChange", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "isGovernment", ignore = true)
    @Mapping(target = "alwaysIssueNfcom", ignore = true)
    @Mapping(target = "portalPin", ignore = true)
    @Mapping(target = "pinForceChange", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CustomerUpdateRequest request, @MappingTarget Customer customer);
}
