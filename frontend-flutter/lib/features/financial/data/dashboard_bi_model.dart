/// Modelo de dados real consumido de GET /bi/metrics ou GET /dashboard/metrics
class OverdueInvoiceItem {
  final String id;
  final String contractId;
  final double amount;
  final String dueDate;

  OverdueInvoiceItem({
    required this.id,
    required this.contractId,
    required this.amount,
    required this.dueDate,
  });

  factory OverdueInvoiceItem.fromJson(Map<String, dynamic> json) {
    return OverdueInvoiceItem(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      dueDate: json['dueDate']?.toString() ?? '',
    );
  }
}

class DashboardBiModel {
  final double mrr;
  final double arr;
  final double arpu;
  final double overdueAmount;
  final double defaultRate;
  final double pixConversionRate;
  final double totalReceivedMonth;
  final int totalCustomers;
  final int activeContracts;
  final int suspendedContracts;
  final int pendingInstallationContracts;
  final int canceledContractsLast30Days;
  final double churnRate;
  final int totalOnus;
  final int provisionedOnus;
  final int criticalSignalOnus;
  final int totalNetworkDevices;
  final List<OverdueInvoiceItem> recentOverdueInvoices;

  DashboardBiModel({
    required this.mrr,
    required this.arr,
    required this.arpu,
    required this.overdueAmount,
    required this.defaultRate,
    required this.pixConversionRate,
    required this.totalReceivedMonth,
    required this.totalCustomers,
    required this.activeContracts,
    required this.suspendedContracts,
    required this.pendingInstallationContracts,
    required this.canceledContractsLast30Days,
    required this.churnRate,
    required this.totalOnus,
    required this.provisionedOnus,
    required this.criticalSignalOnus,
    required this.totalNetworkDevices,
    required this.recentOverdueInvoices,
  });

  factory DashboardBiModel.fromJson(Map<String, dynamic> json) {
    final overdueList = (json['recentOverdueInvoices'] as List<dynamic>?)
            ?.map((e) => OverdueInvoiceItem.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [];

    return DashboardBiModel(
      mrr: (json['mrr'] as num?)?.toDouble() ?? 0.0,
      arr: (json['arr'] as num?)?.toDouble() ?? 0.0,
      arpu: (json['arpu'] as num?)?.toDouble() ?? 0.0,
      overdueAmount: (json['overdueAmount'] as num?)?.toDouble() ?? 0.0,
      defaultRate: (json['defaultRate'] as num?)?.toDouble() ?? 0.0,
      pixConversionRate: (json['pixConversionRate'] as num?)?.toDouble() ?? 0.0,
      totalReceivedMonth: (json['totalReceivedMonth'] as num?)?.toDouble() ?? 0.0,
      totalCustomers: (json['totalCustomers'] as num?)?.toInt() ?? 0,
      activeContracts: (json['activeContracts'] as num?)?.toInt() ?? 0,
      suspendedContracts: (json['suspendedContracts'] as num?)?.toInt() ?? 0,
      pendingInstallationContracts: (json['pendingInstallationContracts'] as num?)?.toInt() ?? 0,
      canceledContractsLast30Days: (json['canceledContractsLast30Days'] as num?)?.toInt() ?? 0,
      churnRate: (json['churnRate'] as num?)?.toDouble() ?? 0.0,
      totalOnus: (json['totalOnus'] as num?)?.toInt() ?? 0,
      provisionedOnus: (json['provisionedOnus'] as num?)?.toInt() ?? 0,
      criticalSignalOnus: (json['criticalSignalOnus'] as num?)?.toInt() ?? 0,
      totalNetworkDevices: (json['totalNetworkDevices'] as num?)?.toInt() ?? 0,
      recentOverdueInvoices: overdueList,
    );
  }
}
