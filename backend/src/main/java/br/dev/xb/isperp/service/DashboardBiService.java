package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.DashboardBiDTO;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class DashboardBiService {

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final OnuProvisioningRepository onuRepository;
    private final NetworkDeviceRepository networkDeviceRepository;

    @Transactional(readOnly = true)
    public DashboardBiDTO getDashboardMetrics() {
        LocalDate today = LocalDate.now();

        // 1. Contratos & Base
        List<Contract> allContracts = contractRepository.findAll();
        long activeContractsCount = allContracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
                .count();
        long suspendedContractsCount = allContracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.SUSPENDED)
                .count();
        long pendingInstallCount = allContracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.PENDING_INSTALLATION)
                .count();
        long canceledContractsCount = allContracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.CANCELED)
                .count();

        long totalCustomers = customerRepository.count();

        // 2. MRR (Monthly Recurring Revenue) & ARR
        BigDecimal mrr = allContracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
                .map(Contract::getMonthlyFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        // 3. ARPU (Ticket Médio por Assinante)
        BigDecimal arpu = activeContractsCount > 0
                ? mrr.divide(BigDecimal.valueOf(activeContractsCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 4. Churn Rate
        long totalActiveAndCanceled = activeContractsCount + canceledContractsCount;
        BigDecimal churnRate = totalActiveAndCanceled > 0
                ? BigDecimal.valueOf(canceledContractsCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalActiveAndCanceled), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. Financeiro & Inadimplência
        List<Invoice> allInvoices = invoiceRepository.findAll();

        BigDecimal overdueAmount = BigDecimal.ZERO;
        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalReceivedMonth = BigDecimal.ZERO;
        long totalPaid = 0;
        long paidWithPix = 0;

        List<Map<String, Object>> recentOverdueList = new ArrayList<>();

        for (Invoice inv : allInvoices) {
            BigDecimal amount = inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO;
            totalInvoiced = totalInvoiced.add(amount);

            if (inv.getStatus() == Invoice.InvoiceStatus.PAID) {
                totalPaid++;
                paidWithPix++; // Todas as baixas instantâneas do Xingubit Pay
                if (inv.getPaidAt() != null && inv.getPaidAt().getMonth() == today.getMonth()) {
                    totalReceivedMonth = totalReceivedMonth.add(amount);
                }
            } else if (inv.getStatus() == Invoice.InvoiceStatus.OVERDUE ||
                    (inv.getStatus() == Invoice.InvoiceStatus.PENDING && inv.getDueDate().isBefore(today))) {
                overdueAmount = overdueAmount.add(amount);
                if (recentOverdueList.size() < 5) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", inv.getId().toString());
                    item.put("contractId", inv.getContractId().toString());
                    item.put("amount", inv.getAmount());
                    item.put("dueDate", inv.getDueDate().toString());
                    recentOverdueList.add(item);
                }
            }
        }

        BigDecimal defaultRate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0
                ? overdueAmount.multiply(BigDecimal.valueOf(100)).divide(totalInvoiced, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal pixConversionRate = totalPaid > 0
                ? BigDecimal.valueOf(paidWithPix).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPaid), 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);

        // 6. NOC & Rede Óptica
        List<OnuProvisioning> allOnus = onuRepository.findAll();
        long totalOnus = allOnus.size();
        long provisionedOnus = allOnus.stream()
                .filter(o -> o.getStatus() == OnuProvisioning.OnuStatus.PROVISIONED)
                .count();

        BigDecimal criticalThreshold = new BigDecimal("-25.00");
        List<Map<String, Object>> criticalAlerts = new ArrayList<>();

        long criticalSignalCount = 0;
        for (OnuProvisioning onu : allOnus) {
            if (onu.getRxPowerDbm() != null && onu.getRxPowerDbm().compareTo(criticalThreshold) < 0) {
                criticalSignalCount++;
                if (criticalAlerts.size() < 5) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("id", onu.getId().toString());
                    alert.put("mac", onu.getOnuMac());
                    alert.put("serial", onu.getOnuSerial());
                    alert.put("rxPowerDbm", onu.getRxPowerDbm());
                    criticalAlerts.add(alert);
                }
            }
        }

        long totalDevices = networkDeviceRepository.count();

        return DashboardBiDTO.builder()
                .mrr(mrr)
                .arr(arr)
                .arpu(arpu)
                .overdueAmount(overdueAmount)
                .defaultRate(defaultRate)
                .pixConversionRate(pixConversionRate)
                .totalReceivedMonth(totalReceivedMonth)
                .totalCustomers(totalCustomers)
                .activeContracts(activeContractsCount)
                .suspendedContracts(suspendedContractsCount)
                .pendingInstallationContracts(pendingInstallCount)
                .canceledContractsLast30Days(canceledContractsCount)
                .churnRate(churnRate)
                .totalOnus(totalOnus)
                .provisionedOnus(provisionedOnus)
                .criticalSignalOnus(criticalSignalCount)
                .totalNetworkDevices(totalDevices)
                .recentOverdueInvoices(recentOverdueList)
                .criticalSignalAlerts(criticalAlerts)
                .build();
    }
}
