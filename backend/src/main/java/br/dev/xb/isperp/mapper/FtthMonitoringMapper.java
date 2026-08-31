package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FtthMonitoringMapper {

    @Mapping(target = "oltName", ignore = true)
    @Mapping(target = "healthPercentage", ignore = true)
    @Mapping(target = "connectedCableName", ignore = true)
    OltPonPortResponse toPonPortResponse(OltPonPort port);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "operStatus", ignore = true)
    @Mapping(target = "adminStatus", ignore = true)
    @Mapping(target = "temperatureCelsius", ignore = true)
    @Mapping(target = "totalOnus", ignore = true)
    @Mapping(target = "onlineOnus", ignore = true)
    @Mapping(target = "losOnus", ignore = true)
    @Mapping(target = "dyingGaspOnus", ignore = true)
    @Mapping(target = "offlineOnus", ignore = true)
    @Mapping(target = "lastPolledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OltPonPort toPonPortEntity(OltPonPortRequest request);

    @Mapping(target = "oltName", ignore = true)
    @Mapping(target = "ponName", ignore = true)
    @Mapping(target = "incidentTypeDescription", expression = "java(incident.getIncidentType() != null ? incident.getIncidentType().getDescription() : \"\")")
    @Mapping(target = "affectedCtoNames", ignore = true)
    @Mapping(target = "affectedCableName", ignore = true)
    @Mapping(target = "workOrderProtocol", ignore = true)
    FtthIncidentResponse toIncidentResponse(FtthIncident incident);
}
