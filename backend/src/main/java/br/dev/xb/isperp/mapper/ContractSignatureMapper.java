package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.SignatureSessionResponse;
import br.dev.xb.isperp.entity.ContractSignature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContractSignatureMapper {

    @Mapping(target = "signatureUrl", ignore = true)
    SignatureSessionResponse toResponse(ContractSignature entity);

    List<SignatureSessionResponse> toResponseList(List<ContractSignature> entities);
}
