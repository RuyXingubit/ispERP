package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.entity.financial.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityLifecycleListenerTest {

    private final EntityLifecycleListener listener = new EntityLifecycleListener();

    @Test
    @DisplayName("Deve preencher id, createdAt, updatedAt e defaults em entidades LocalAuditableEntity")
    void shouldPopulateLocalAuditableEntities() {
        List<LocalAuditableEntity> entities = List.of(
                new Company(),
                new Customer(),
                new FiscalCompany(),
                new FiscalGatewayConfig(),
                new NetworkDevice(),
                new NotificationConfig(),
                new PaymentGatewayConfig(),
                new NfcomRecord(),
                new SiteSettings(),
                new Warehouse(),
                new ServiceRoute(),
                new StockTransfer(),
                new InventoryItem(),
                new OnuProvisioning(),
                new Contract(),
                new HelpdeskTicket(),
                new LegalCollectionRecord(),
                new Sale(),
                new SerializedAsset(),
                new ToolCustodyAgreement(),
                new WorkOrder()
        );

        for (LocalAuditableEntity entity : entities) {
            listener.onPrePersist(entity);
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();

            listener.onPreUpdate(entity);
            assertThat(entity.getUpdatedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("Deve preencher id, createdAt, updatedAt e defaults em entidades OffsetAuditableEntity e CustodyTransferLogEntity")
    void shouldPopulateOffsetAndCustodyEntities() {
        List<OffsetAuditableEntity> offsetEntities = List.of(
                new PayableInvoice(),
                new NetworkProject(),
                new ExpenseInstallment(),
                new BankDepositConfirmation(),
                new ChartOfAccount(),
                new SentinelAuditLog(),
                new OltPonPort(),
                new CgnatMapping(),
                new FtthCtoPort(),
                new FtthFusion(),
                new FtthPole(),
                new FtthPop(),
                new IpamAsn(),
                new IpamIpAddress(),
                new IpamSubnet(),
                new ContractSignature(),
                new InstallationMaterialDemand(),
                new ContractTemplate(),
                new FtthCable(),
                new FtthClosure(),
                new FtthCto(),
                new IpamVrf(),
                new Nas()
        );

        for (OffsetAuditableEntity entity : offsetEntities) {
            listener.onPrePersist(entity);
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();

            listener.onPreUpdate(entity);
            assertThat(entity.getUpdatedAt()).isNotNull();
        }

        CashTransferLog cashLog = new CashTransferLog();
        cashLog.setStatus(null);
        listener.onPrePersist(cashLog);
        assertThat(cashLog.getId()).isNotNull();
        assertThat(cashLog.getRequestedAt()).isNotNull();
        assertThat(cashLog.getStatus()).isEqualTo(CashTransferStatus.PENDING_ACCEPTANCE);

        MaterialTransferLog matLog = new MaterialTransferLog();
        matLog.setQuantity(null);
        matLog.setStatus(null);
        listener.onPrePersist(matLog);
        assertThat(matLog.getId()).isNotNull();
        assertThat(matLog.getRequestedAt()).isNotNull();
        assertThat(matLog.getQuantity()).isEqualTo(BigDecimal.ONE);
        assertThat(matLog.getStatus()).isEqualTo(CashTransferStatus.PENDING_ACCEPTANCE);

        listener.onPrePersist(new Object());
        listener.onPreUpdate(new Object());
    }

    @Test
    @DisplayName("Deve aplicar defaults quando os campos de negócio estiverem nulos antes do persist")
    void shouldInitializeNullBusinessDefaults() {
        Contract contract = new Contract();
        contract.setStatus(null);
        contract.setPendingOnboardingCredit(null);
        contract.setPendingProRataAdjustment(null);
        listener.onPrePersist(contract);
        assertThat(contract.getStatus()).isEqualTo(Contract.ContractStatus.PENDING_INSTALLATION);
        assertThat(contract.getPendingOnboardingCredit()).isEqualTo(BigDecimal.ZERO);

        HelpdeskTicket ticket = new HelpdeskTicket();
        ticket.setPriority(null);
        ticket.setChannel(null);
        ticket.setStatus(null);
        listener.onPrePersist(ticket);
        assertThat(ticket.getPriority()).isEqualTo(HelpdeskTicket.TicketPriority.NORMAL);

        LegalCollectionRecord legal = new LegalCollectionRecord();
        legal.setStatus(null);
        legal.setTotalDebtInvoices(BigDecimal.TEN);
        legal.setEquipmentIndemnityAmount(BigDecimal.ONE);
        listener.onPrePersist(legal);
        assertThat(legal.getTotalClaimAmount()).isEqualByComparingTo("11");

        WorkOrder wo = new WorkOrder();
        wo.setType(null);
        wo.setStatus(null);
        listener.onPrePersist(wo);
        assertThat(wo.getType()).isEqualTo(WorkOrder.WorkOrderType.INSTALACAO);

        ContractSignature sig = new ContractSignature();
        sig.setSymbolicAmount(null);
        sig.setStatus(null);
        listener.onPrePersist(sig);
        assertThat(sig.getSymbolicAmount()).isEqualByComparingTo("1.00");

        InstallationMaterialDemand demand = new InstallationMaterialDemand();
        demand.setEstimatedDropMeters(null);
        demand.setFastConnectorsCount(null);
        demand.setPtoRosetteCount(null);
        demand.setStatus(null);
        listener.onPrePersist(demand);
        assertThat(demand.getEstimatedDropMeters()).isEqualTo(50);

        ContractTemplate tpl = new ContractTemplate();
        tpl.setVersion(null);
        tpl.setIsActive(null);
        listener.onPrePersist(tpl);
        assertThat(tpl.getVersion()).isEqualTo(1);
    }
}
