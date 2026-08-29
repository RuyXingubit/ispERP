package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardBiDTO {

    // Métricas Financeiras & Faturamento
    private BigDecimal mrr; // Monthly Recurring Revenue
    private BigDecimal arr; // Annual Recurring Revenue (MRR * 12)
    private BigDecimal arpu; // Average Revenue Per User (Ticket Médio)
    private BigDecimal overdueAmount; // Total em R$ vencido
    private BigDecimal defaultRate; // % de Inadimplência
    private BigDecimal pixConversionRate; // % de faturas pagas via Pix Xingubit Pay
    private BigDecimal totalReceivedMonth; // Total arrecadado no mês corrente

    // Métricas Comerciais & Base
    private long totalCustomers;
    private long activeContracts;
    private long suspendedContracts;
    private long pendingInstallationContracts;
    private long canceledContractsLast30Days;
    private BigDecimal churnRate; // % de cancelamento nos últimos 30 dias

    // Métricas de NOC & Rede Óptica
    private long totalOnus;
    private long provisionedOnus;
    private long criticalSignalOnus; // Sinal < -25.00 dBm
    private long totalNetworkDevices; // OLTs e concentradores

    // Listas de Ação Rápida para o Gestor
    private List<Map<String, Object>> recentOverdueInvoices;
    private List<Map<String, Object>> criticalSignalAlerts;
}
