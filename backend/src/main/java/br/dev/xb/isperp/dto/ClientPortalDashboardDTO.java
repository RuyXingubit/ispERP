package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.Plan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientPortalDashboardDTO {

    private Customer customer;
    private Contract contract;
    private Plan currentPlan;
    private List<Plan> availableUpgradePlans;
    private List<Invoice> pendingInvoices;
    private List<Invoice> paidInvoices;
    private List<Invoice> overdueInvoices;
    private boolean isConnectionBlocked;
    private boolean canRequestTrustUnblock;
    private String connectionStatusMessage;
}
