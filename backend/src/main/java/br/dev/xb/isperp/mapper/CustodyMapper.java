package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.financial.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustodyMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userRole", source = "user.role")
    CashCustodyDto toDto(UserCashCustody entity);

    List<CashCustodyDto> toCashCustodyDtoList(List<UserCashCustody> entities);

    @Mapping(target = "senderUserId", source = "sender.id")
    @Mapping(target = "senderUserName", source = "sender.name")
    @Mapping(target = "receiverUserId", source = "receiver.id")
    @Mapping(target = "receiverUserName", source = "receiver.name")
    CashTransferResponseDto toDto(CashTransferLog entity);

    List<CashTransferResponseDto> toCashTransferDtoList(List<CashTransferLog> entities);

    @Mapping(target = "depositorUserId", source = "depositor.id")
    @Mapping(target = "depositorUserName", source = "depositor.name")
    @Mapping(target = "depositorCpf", source = "depositor.cpf")
    @Mapping(target = "auditedByUserId", source = "auditedBy.id")
    @Mapping(target = "auditedByUserName", source = "auditedBy.name")
    BankDepositResponseDto toDto(BankDepositConfirmation entity);

    List<BankDepositResponseDto> toBankDepositDtoList(List<BankDepositConfirmation> entities);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "userCpf", source = "user.cpf")
    MaterialCustodyDto toDto(UserMaterialCustody entity);

    List<MaterialCustodyDto> toMaterialCustodyDtoList(List<UserMaterialCustody> entities);

    @Mapping(target = "senderUserId", source = "sender.id")
    @Mapping(target = "senderUserName", source = "sender.name")
    @Mapping(target = "receiverUserId", source = "receiver.id")
    @Mapping(target = "receiverUserName", source = "receiver.name")
    @Mapping(target = "materialCustodyId", source = "materialCustody.id")
    @Mapping(target = "itemName", source = "materialCustody.itemName")
    @Mapping(target = "serialNumber", source = "materialCustody.serialNumber")
    MaterialTransferResponseDto toDto(MaterialTransferLog entity);

    List<MaterialTransferResponseDto> toMaterialTransferDtoList(List<MaterialTransferLog> entities);
}
