package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IpamMapper {

    IpamAsn toEntity(IpamAsnRequest request);
    IpamAsnResponse toResponse(IpamAsn entity);
    void updateEntityFromRequest(IpamAsnRequest request, @MappingTarget IpamAsn entity);

    IpamVrf toEntity(IpamVrfRequest request);
    IpamVrfResponse toResponse(IpamVrf entity);
    void updateEntityFromRequest(IpamVrfRequest request, @MappingTarget IpamVrf entity);

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "vrf", ignore = true)
    @Mapping(target = "asn", ignore = true)
    IpamSubnet toEntity(IpamSubnetRequest request);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "vrfId", source = "vrf.id")
    @Mapping(target = "vrfName", source = "vrf.name")
    @Mapping(target = "asnId", source = "asn.id")
    @Mapping(target = "asnNumber", source = "asn.asn")
    @Mapping(target = "allocatedHosts", ignore = true)
    @Mapping(target = "utilizationPercentage", ignore = true)
    IpamSubnetResponse toResponse(IpamSubnet entity);

    void updateEntityFromRequest(IpamSubnetRequest request, @MappingTarget IpamSubnet entity);

    @Mapping(target = "subnet", ignore = true)
    IpamIpAddress toEntity(IpamIpAddressRequest request);

    @Mapping(target = "subnetId", source = "subnet.id")
    @Mapping(target = "subnetCidr", source = "subnet.cidr")
    @Mapping(target = "assignedToLabel", ignore = true)
    IpamIpAddressResponse toResponse(IpamIpAddress entity);

    void updateEntityFromRequest(IpamIpAddressRequest request, @MappingTarget IpamIpAddress entity);
}
