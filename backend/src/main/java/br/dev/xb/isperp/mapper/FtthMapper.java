package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FtthMapper {

    FtthPopResponse toPopResponse(FtthPop pop);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FtthPop toPopEntity(FtthPopRequest request);

    FtthPoleResponse toPoleResponse(FtthPole pole);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FtthPole toPoleEntity(FtthPoleRequest request);

    @Mapping(target = "fibers", ignore = true)
    FtthCableResponse toCableResponse(FtthCable cable);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FtthCable toCableEntity(FtthCableRequest request);

    @Mapping(target = "poleCode", ignore = true)
    @Mapping(target = "usedFusionsCount", ignore = true)
    FtthClosureResponse toClosureResponse(FtthClosure closure);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FtthClosure toClosureEntity(FtthClosureRequest request);

    @Mapping(target = "outputPorts", expression = "java(splitter.getSplitterType() != null ? splitter.getSplitterType().getOutputPorts() : 2)")
    FtthSplitterResponse toSplitterResponse(FtthSplitter splitter);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    FtthSplitter toSplitterEntity(FtthSplitterRequest request);

    @Mapping(target = "poleCode", ignore = true)
    @Mapping(target = "freePortsCount", ignore = true)
    @Mapping(target = "occupiedPortsCount", ignore = true)
    @Mapping(target = "occupancyPercentage", ignore = true)
    @Mapping(target = "ports", ignore = true)
    FtthCtoResponse toCtoResponse(FtthCto cto);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FtthCto toCtoEntity(FtthCtoRequest request);

    @Mapping(target = "onuSerial", ignore = true)
    @Mapping(target = "onuMac", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "pppoeUser", ignore = true)
    FtthCtoPortResponse toCtoPortResponse(FtthCtoPort port);

    @Mapping(target = "sourceCableName", ignore = true)
    @Mapping(target = "sourceFiberColor", ignore = true)
    @Mapping(target = "targetCableName", ignore = true)
    @Mapping(target = "targetFiberColor", ignore = true)
    @Mapping(target = "targetSplitterName", ignore = true)
    @Mapping(target = "targetCtoName", ignore = true)
    FtthFusionResponse toFusionResponse(FtthFusion fusion);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FtthFusion toFusionEntity(FtthFusionRequest request);
}
