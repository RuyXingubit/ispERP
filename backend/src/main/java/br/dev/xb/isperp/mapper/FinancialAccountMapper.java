package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.entity.financial.ExpenseInstallment;
import br.dev.xb.isperp.entity.financial.PayableInvoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FinancialAccountMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentCode", source = "parent.code")
    @Mapping(target = "children", ignore = true)
    ChartOfAccountDto toDto(ChartOfAccount entity);

    List<ChartOfAccountDto> toChartOfAccountDtoList(List<ChartOfAccount> entities);

    @Mapping(target = "chartOfAccountId", source = "chartOfAccount.id")
    @Mapping(target = "chartOfAccountCode", source = "chartOfAccount.code")
    @Mapping(target = "chartOfAccountName", source = "chartOfAccount.name")
    @Mapping(target = "installments", source = "installments")
    PayableInvoiceDto toDto(PayableInvoice entity);

    List<PayableInvoiceDto> toPayableInvoiceDtoList(List<PayableInvoice> entities);

    ExpenseInstallmentDto toDto(ExpenseInstallment entity);

    List<ExpenseInstallmentDto> toExpenseInstallmentDtoList(List<ExpenseInstallment> entities);

    @Mapping(target = "workOrderId", source = "id")
    @Mapping(target = "protocol", expression = "java(\"OS-\" + workOrder.getId().toString().substring(0, 8).toUpperCase())")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "serviceType", expression = "java(workOrder.getType() != null ? workOrder.getType().name() : null)")
    @Mapping(target = "standardFeeAmount", source = "standardFeeAmount")
    @Mapping(target = "feeStatus", source = "feeStatus")
    @Mapping(target = "waiverReason", source = "waiverReason")
    @Mapping(target = "waiverRequestedByUserId", source = "waiverRequestedByUserId")
    @Mapping(target = "waiverRequestedByName", ignore = true)
    @Mapping(target = "waiverAuditedByUserId", source = "waiverAuditedByUserId")
    @Mapping(target = "waiverAuditedByName", ignore = true)
    @Mapping(target = "waiverAuditedAt", source = "waiverAuditedAt")
    WorkOrderFeeDto toDto(WorkOrder workOrder);

    List<WorkOrderFeeDto> toWorkOrderFeeDtoList(List<WorkOrder> workOrders);
}
