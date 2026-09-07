package br.dev.xb.isperp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FinancialDomainMapper {

    // 1. Cash Custody
    br.dev.xb.isperp.api.dto.CashCustodyDto toOpenApiCashCustody(br.dev.xb.isperp.dto.financial.CashCustodyDto dto);
    List<br.dev.xb.isperp.api.dto.CashCustodyDto> toOpenApiCashCustodyList(List<br.dev.xb.isperp.dto.financial.CashCustodyDto> list);
    br.dev.xb.isperp.dto.financial.CashSettlementRequest toDomainCashSettlementRequest(br.dev.xb.isperp.api.dto.CashSettlementRequest request);
    br.dev.xb.isperp.dto.financial.CashTransferRequest toDomainCashTransferRequest(br.dev.xb.isperp.api.dto.CashTransferRequest request);
    br.dev.xb.isperp.api.dto.CashTransferResponseDto toOpenApiCashTransferResponse(br.dev.xb.isperp.dto.financial.CashTransferResponseDto dto);
    List<br.dev.xb.isperp.api.dto.CashTransferResponseDto> toOpenApiCashTransferResponseList(List<br.dev.xb.isperp.dto.financial.CashTransferResponseDto> list);
    br.dev.xb.isperp.dto.financial.BankDepositRequest toDomainBankDepositRequest(br.dev.xb.isperp.api.dto.BankDepositRequest request);
    br.dev.xb.isperp.api.dto.BankDepositResponseDto toOpenApiBankDepositResponse(br.dev.xb.isperp.dto.financial.BankDepositResponseDto dto);
    List<br.dev.xb.isperp.api.dto.BankDepositResponseDto> toOpenApiBankDepositResponseList(List<br.dev.xb.isperp.dto.financial.BankDepositResponseDto> list);
    br.dev.xb.isperp.dto.financial.BankDepositAuditRequest toDomainBankDepositAuditRequest(br.dev.xb.isperp.api.dto.BankDepositAuditRequest request);

    // 2. Chart of Accounts
    br.dev.xb.isperp.api.dto.ChartOfAccountDto toOpenApiChartOfAccount(br.dev.xb.isperp.dto.financial.ChartOfAccountDto dto);
    List<br.dev.xb.isperp.api.dto.ChartOfAccountDto> toOpenApiChartOfAccountList(List<br.dev.xb.isperp.dto.financial.ChartOfAccountDto> list);
    br.dev.xb.isperp.dto.financial.ChartOfAccountDto toDomainChartOfAccount(br.dev.xb.isperp.api.dto.ChartOfAccountDto dto);

    // 3. Deleveraging
    br.dev.xb.isperp.api.dto.DeleveragingProjectionDto toOpenApiDeleveragingProjection(br.dev.xb.isperp.dto.financial.DeleveragingProjectionDto dto);
    br.dev.xb.isperp.dto.financial.SimulationRequest toDomainSimulationRequest(br.dev.xb.isperp.api.dto.SimulationRequest request);
    br.dev.xb.isperp.api.dto.SimulationResponse toOpenApiSimulationResponse(br.dev.xb.isperp.dto.financial.SimulationResponse response);

    // 4. DRE
    br.dev.xb.isperp.api.dto.DreReportDto toOpenApiDreReport(br.dev.xb.isperp.dto.financial.DreReportDto dto);
    br.dev.xb.isperp.dto.financial.AccountingMethod toDomainAccountingMethod(br.dev.xb.isperp.api.dto.AccountingMethod method);

    // 5. Material Custody
    br.dev.xb.isperp.api.dto.MaterialCustodyDto toOpenApiMaterialCustody(br.dev.xb.isperp.dto.financial.MaterialCustodyDto dto);
    List<br.dev.xb.isperp.api.dto.MaterialCustodyDto> toOpenApiMaterialCustodyList(List<br.dev.xb.isperp.dto.financial.MaterialCustodyDto> list);
    br.dev.xb.isperp.dto.financial.MaterialCustodyDto toDomainMaterialCustody(br.dev.xb.isperp.api.dto.MaterialCustodyDto dto);
    br.dev.xb.isperp.dto.financial.MaterialTransferRequest toDomainMaterialTransferRequest(br.dev.xb.isperp.api.dto.MaterialTransferRequest request);
    br.dev.xb.isperp.api.dto.MaterialTransferResponseDto toOpenApiMaterialTransferResponse(br.dev.xb.isperp.dto.financial.MaterialTransferResponseDto dto);
    List<br.dev.xb.isperp.api.dto.MaterialTransferResponseDto> toOpenApiMaterialTransferResponseList(List<br.dev.xb.isperp.dto.financial.MaterialTransferResponseDto> list);

    // 6. Network Projects
    br.dev.xb.isperp.api.dto.NetworkProjectPaybackDto toOpenApiNetworkProjectPayback(br.dev.xb.isperp.dto.financial.NetworkProjectPaybackDto dto);
    List<br.dev.xb.isperp.api.dto.NetworkProjectPaybackDto> toOpenApiNetworkProjectPaybackList(List<br.dev.xb.isperp.dto.financial.NetworkProjectPaybackDto> list);
    br.dev.xb.isperp.dto.financial.NetworkProjectRequest toDomainNetworkProjectRequest(br.dev.xb.isperp.api.dto.NetworkProjectRequest request);
    br.dev.xb.isperp.api.dto.NetworkProjectResponse toOpenApiNetworkProjectResponse(br.dev.xb.isperp.entity.financial.NetworkProject entity);

    // 7. Payables
    br.dev.xb.isperp.api.dto.PayableInvoiceDto toOpenApiPayableInvoice(br.dev.xb.isperp.dto.financial.PayableInvoiceDto dto);
    List<br.dev.xb.isperp.api.dto.PayableInvoiceDto> toOpenApiPayableInvoiceList(List<br.dev.xb.isperp.dto.financial.PayableInvoiceDto> list);
    br.dev.xb.isperp.dto.financial.PayableInvoiceRequest toDomainPayableInvoiceRequest(br.dev.xb.isperp.api.dto.PayableInvoiceRequest request);
    br.dev.xb.isperp.api.dto.ExpenseInstallmentDto toOpenApiExpenseInstallment(br.dev.xb.isperp.dto.financial.ExpenseInstallmentDto dto);
    List<br.dev.xb.isperp.api.dto.ExpenseInstallmentDto> toOpenApiExpenseInstallmentList(List<br.dev.xb.isperp.dto.financial.ExpenseInstallmentDto> list);

    // 8. Sentinel
    br.dev.xb.isperp.api.dto.SentinelAuditLogDto toOpenApiSentinelAuditLog(br.dev.xb.isperp.dto.financial.SentinelAuditLogDto dto);
    List<br.dev.xb.isperp.api.dto.SentinelAuditLogDto> toOpenApiSentinelAuditLogList(List<br.dev.xb.isperp.dto.financial.SentinelAuditLogDto> list);

    // 9. Work Order Fees
    br.dev.xb.isperp.api.dto.WorkOrderFeeDto toOpenApiWorkOrderFee(br.dev.xb.isperp.dto.financial.WorkOrderFeeDto dto);
    List<br.dev.xb.isperp.api.dto.WorkOrderFeeDto> toOpenApiWorkOrderFeeList(List<br.dev.xb.isperp.dto.financial.WorkOrderFeeDto> list);
    br.dev.xb.isperp.dto.financial.WorkOrderFeeWaiverRequest toDomainWorkOrderFeeWaiverRequest(br.dev.xb.isperp.api.dto.WorkOrderFeeWaiverRequest request);
    br.dev.xb.isperp.dto.financial.WorkOrderFeeAuditRequest toDomainWorkOrderFeeAuditRequest(br.dev.xb.isperp.api.dto.WorkOrderFeeAuditRequest request);
}
