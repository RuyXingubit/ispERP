import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Parâmetros de filtro para geração do DRE (Competência vs Caixa).
class DreFilterParams {
  final String startDate;
  final String endDate;
  final String method; // 'ACCRUAL' | 'CASH'

  const DreFilterParams({
    required this.startDate,
    required this.endDate,
    this.method = 'ACCRUAL',
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is DreFilterParams &&
          runtimeType == other.runtimeType &&
          startDate == other.startDate &&
          endDate == other.endDate &&
          method == other.method;

  @override
  int get hashCode => Object.hash(startDate, endDate, method);
}

/// Modelo do Demonstrativo do Resultado do Exercício Telecom (DreReportDto).
class DreReportModel {
  final String periodStart;
  final String periodEnd;
  final String accountingMethod;
  final double grossRevenue;
  final double taxDeductions;
  final double netRevenue;
  final double directCostsInterconnection;
  final double contributionMargin;
  final double opexPersonnel;
  final double opexInfrastructure;
  final double opexAdministrative;
  final double opexCommercial;
  final double totalOpex;
  final double ebitda;
  final double ebitdaMarginPercentage;
  final double financialResult;
  final double netIncome;
  final double netMarginPercentage;

  const DreReportModel({
    required this.periodStart,
    required this.periodEnd,
    required this.accountingMethod,
    required this.grossRevenue,
    required this.taxDeductions,
    required this.netRevenue,
    required this.directCostsInterconnection,
    required this.contributionMargin,
    required this.opexPersonnel,
    required this.opexInfrastructure,
    required this.opexAdministrative,
    required this.opexCommercial,
    required this.totalOpex,
    required this.ebitda,
    required this.ebitdaMarginPercentage,
    required this.financialResult,
    required this.netIncome,
    required this.netMarginPercentage,
  });

  factory DreReportModel.fromJson(Map<String, dynamic> json) {
    return DreReportModel(
      periodStart: json['periodStart']?.toString() ?? '',
      periodEnd: json['periodEnd']?.toString() ?? '',
      accountingMethod: json['accountingMethod']?.toString() ?? 'ACCRUAL',
      grossRevenue: (json['grossRevenue'] as num?)?.toDouble() ?? 0.0,
      taxDeductions: (json['taxDeductions'] as num?)?.toDouble() ?? 0.0,
      netRevenue: (json['netRevenue'] as num?)?.toDouble() ?? 0.0,
      directCostsInterconnection:
          (json['directCostsInterconnection'] as num?)?.toDouble() ?? 0.0,
      contributionMargin: (json['contributionMargin'] as num?)?.toDouble() ?? 0.0,
      opexPersonnel: (json['opexPersonnel'] as num?)?.toDouble() ?? 0.0,
      opexInfrastructure: (json['opexInfrastructure'] as num?)?.toDouble() ?? 0.0,
      opexAdministrative: (json['opexAdministrative'] as num?)?.toDouble() ?? 0.0,
      opexCommercial: (json['opexCommercial'] as num?)?.toDouble() ?? 0.0,
      totalOpex: (json['totalOpex'] as num?)?.toDouble() ?? 0.0,
      ebitda: (json['ebitda'] as num?)?.toDouble() ?? 0.0,
      ebitdaMarginPercentage:
          (json['ebitdaMarginPercentage'] as num?)?.toDouble() ?? 0.0,
      financialResult: (json['financialResult'] as num?)?.toDouble() ?? 0.0,
      netIncome: (json['netIncome'] as num?)?.toDouble() ?? 0.0,
      netMarginPercentage: (json['netMarginPercentage'] as num?)?.toDouble() ?? 0.0,
    );
  }
}

/// Ponto mensal da curva de 36 meses de Desalavancagem (MonthlyProjectionPointDto).
class MonthlyProjectionPointModel {
  final String yearMonth;
  final double projectedMrr;
  final double projectedDirectCosts;
  final double projectedOpex;
  final double projectedDebtInstallments;
  final double projectedNetCashFlow;
  final double cumulativeCashBalance;
  final bool isNegativeBalance;

  const MonthlyProjectionPointModel({
    required this.yearMonth,
    required this.projectedMrr,
    required this.projectedDirectCosts,
    required this.projectedOpex,
    required this.projectedDebtInstallments,
    required this.projectedNetCashFlow,
    required this.cumulativeCashBalance,
    required this.isNegativeBalance,
  });

  factory MonthlyProjectionPointModel.fromJson(Map<String, dynamic> json) {
    return MonthlyProjectionPointModel(
      yearMonth: json['yearMonth']?.toString() ?? '',
      projectedMrr: (json['projectedMrr'] as num?)?.toDouble() ?? 0.0,
      projectedDirectCosts:
          (json['projectedDirectCosts'] as num?)?.toDouble() ?? 0.0,
      projectedOpex: (json['projectedOpex'] as num?)?.toDouble() ?? 0.0,
      projectedDebtInstallments:
          (json['projectedDebtInstallments'] as num?)?.toDouble() ?? 0.0,
      projectedNetCashFlow:
          (json['projectedNetCashFlow'] as num?)?.toDouble() ?? 0.0,
      cumulativeCashBalance:
          (json['cumulativeCashBalance'] as num?)?.toDouble() ?? 0.0,
      isNegativeBalance: json['isNegativeBalance'] as bool? ?? false,
    );
  }
}

/// Projeção de Desalavancagem de 36 Meses com os 3 Números Sagrados (DeleveragingProjectionDto).
class DeleveragingProjectionModel {
  final double currentEbitda;
  final double currentMrr;
  final double monthlyChurnRatePercentage;
  final double startingCashBalance;
  final String worstMonthYearMonth;
  final double worstMonthProjectedBalance;
  final String breakEvenYearMonth;
  final int monthsUntilFreedom;
  final List<MonthlyProjectionPointModel> timeline;

  const DeleveragingProjectionModel({
    required this.currentEbitda,
    required this.currentMrr,
    required this.monthlyChurnRatePercentage,
    required this.startingCashBalance,
    required this.worstMonthYearMonth,
    required this.worstMonthProjectedBalance,
    required this.breakEvenYearMonth,
    required this.monthsUntilFreedom,
    this.timeline = const [],
  });

  factory DeleveragingProjectionModel.fromJson(Map<String, dynamic> json) {
    final rawTimeline = json['timeline'];
    return DeleveragingProjectionModel(
      currentEbitda: (json['currentEbitda'] as num?)?.toDouble() ?? 0.0,
      currentMrr: (json['currentMrr'] as num?)?.toDouble() ?? 0.0,
      monthlyChurnRatePercentage:
          (json['monthlyChurnRatePercentage'] as num?)?.toDouble() ?? 0.0,
      startingCashBalance:
          (json['startingCashBalance'] as num?)?.toDouble() ?? 0.0,
      worstMonthYearMonth: json['worstMonthYearMonth']?.toString() ?? '-',
      worstMonthProjectedBalance:
          (json['worstMonthProjectedBalance'] as num?)?.toDouble() ?? 0.0,
      breakEvenYearMonth: json['breakEvenYearMonth']?.toString() ?? '-',
      monthsUntilFreedom: (json['monthsUntilFreedom'] as num?)?.toInt() ?? 0,
      timeline: rawTimeline is List
          ? rawTimeline
              .map((e) => MonthlyProjectionPointModel.fromJson(e as Map<String, dynamic>))
              .toList()
          : const [],
    );
  }
}

/// Resultado do Simulador de Investimento / CAPEX (SimulationResponse).
class InvestmentSimulationResultModel {
  final bool feasible;
  final double monthlyInstallmentAmount;
  final String simulatedWorstMonth;
  final double simulatedWorstBalance;
  final double balanceImpactAtWorstMonth;
  final String originalBreakEvenMonth;
  final String simulatedBreakEvenMonth;
  final int delayInMonthsForFreedom;
  final String riskAnalysisSummary;

  const InvestmentSimulationResultModel({
    required this.feasible,
    required this.monthlyInstallmentAmount,
    required this.simulatedWorstMonth,
    required this.simulatedWorstBalance,
    required this.balanceImpactAtWorstMonth,
    required this.originalBreakEvenMonth,
    required this.simulatedBreakEvenMonth,
    required this.delayInMonthsForFreedom,
    required this.riskAnalysisSummary,
  });

  factory InvestmentSimulationResultModel.fromJson(Map<String, dynamic> json) {
    return InvestmentSimulationResultModel(
      feasible: json['feasible'] as bool? ?? false,
      monthlyInstallmentAmount:
          (json['monthlyInstallmentAmount'] as num?)?.toDouble() ?? 0.0,
      simulatedWorstMonth: json['simulatedWorstMonth']?.toString() ?? '-',
      simulatedWorstBalance:
          (json['simulatedWorstBalance'] as num?)?.toDouble() ?? 0.0,
      balanceImpactAtWorstMonth:
          (json['balanceImpactAtWorstMonth'] as num?)?.toDouble() ?? 0.0,
      originalBreakEvenMonth: json['originalBreakEvenMonth']?.toString() ?? '-',
      simulatedBreakEvenMonth: json['simulatedBreakEvenMonth']?.toString() ?? '-',
      delayInMonthsForFreedom:
          (json['delayInMonthsForFreedom'] as num?)?.toInt() ?? 0,
      riskAnalysisSummary: json['riskAnalysisSummary']?.toString() ?? '',
    );
  }
}

/// Modelo de Payback de Projeto de Expansão FTTH (NetworkProjectPaybackDto).
class NetworkProjectPaybackModel {
  final String projectId;
  final String name;
  final String neighborhood;
  final String city;
  final String status;
  final double budgetAmount;
  final int targetSubscribers;
  final String startDate;
  final int ctoCount;
  final int totalPorts;
  final int occupiedPorts;
  final double penetrationRate;
  final double monthlyRevenue;
  final double accumulatedRevenue;
  final int paybackMonthsEstimated;
  final bool isPaybackAchieved;
  final String commercialPriority;

  const NetworkProjectPaybackModel({
    required this.projectId,
    required this.name,
    required this.neighborhood,
    required this.city,
    required this.status,
    required this.budgetAmount,
    required this.targetSubscribers,
    required this.startDate,
    required this.ctoCount,
    required this.totalPorts,
    required this.occupiedPorts,
    required this.penetrationRate,
    required this.monthlyRevenue,
    required this.accumulatedRevenue,
    required this.paybackMonthsEstimated,
    required this.isPaybackAchieved,
    required this.commercialPriority,
  });

  factory NetworkProjectPaybackModel.fromJson(Map<String, dynamic> json) {
    return NetworkProjectPaybackModel(
      projectId: json['projectId']?.toString() ?? json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      neighborhood: json['neighborhood']?.toString() ?? '',
      city: json['city']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PLANNING',
      budgetAmount: (json['budgetAmount'] as num?)?.toDouble() ?? 0.0,
      targetSubscribers: (json['targetSubscribers'] as num?)?.toInt() ?? 0,
      startDate: json['startDate']?.toString() ?? '',
      ctoCount: (json['ctoCount'] as num?)?.toInt() ?? 0,
      totalPorts: (json['totalPorts'] as num?)?.toInt() ?? 0,
      occupiedPorts: (json['occupiedPorts'] as num?)?.toInt() ?? 0,
      penetrationRate: (json['penetrationRate'] as num?)?.toDouble() ?? 0.0,
      monthlyRevenue: (json['monthlyRevenue'] as num?)?.toDouble() ?? 0.0,
      accumulatedRevenue:
          (json['accumulatedRevenue'] as num?)?.toDouble() ?? 0.0,
      paybackMonthsEstimated:
          (json['paybackMonthsEstimated'] as num?)?.toInt() ?? 0,
      isPaybackAchieved: json['isPaybackAchieved'] as bool? ?? false,
      commercialPriority: json['commercialPriority']?.toString() ?? 'NORMAL',
    );
  }
}

/// Modelo de Alerta Pericial do Sentinela IA (SentinelAuditLogDto).
class SentinelAuditAlertModel {
  final String id;
  final String auditType;
  final String severity;
  final String title;
  final String description;
  final String geminiAnalysis;
  final String recommendedAction;
  final bool resolved;
  final String? createdAt;

  const SentinelAuditAlertModel({
    required this.id,
    required this.auditType,
    required this.severity,
    required this.title,
    required this.description,
    required this.geminiAnalysis,
    required this.recommendedAction,
    required this.resolved,
    this.createdAt,
  });

  factory SentinelAuditAlertModel.fromJson(Map<String, dynamic> json) {
    return SentinelAuditAlertModel(
      id: json['id']?.toString() ?? '',
      auditType: json['auditType']?.toString() ?? 'AUDITORIA_PATRIMONIAL',
      severity: json['severity']?.toString() ?? 'HIGH',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      geminiAnalysis: json['geminiAnalysis']?.toString() ?? '',
      recommendedAction: json['recommendedAction']?.toString() ?? '',
      resolved: json['resolved'] as bool? ?? false,
      createdAt: json['createdAt']?.toString(),
    );
  }
}

/// Repositório de Inteligência Estratégica Financeira:
/// DRE Telecom, Motor de Desalavancagem 36M, Payback FTTH e Sentinela Pericial.
class StrategicFinanceRepository {
  final ApiClient _apiClient;

  StrategicFinanceRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Gera o DRE consolidado (GET /financial/reports/dre).
  Future<DreReportModel?> getDreReport(DreFilterParams params) async {
    try {
      final response = await _dio.get(
        '/financial/reports/dre',
        queryParameters: {
          'startDate': params.startDate,
          'endDate': params.endDate,
          'method': params.method,
        },
      );
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return DreReportModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Calcula a projeção de 36 meses de desalavancagem (GET /financial/deleveraging/projection).
  Future<DeleveragingProjectionModel?> getDeleveragingProjection() async {
    try {
      final response = await _dio.get('/financial/deleveraging/projection');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return DeleveragingProjectionModel.fromJson(
          response.data as Map<String, dynamic>,
        );
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Simula novo investimento parcelado / CAPEX (POST /financial/deleveraging/simulate).
  Future<InvestmentSimulationResultModel?> simulateNewInvestment({
    required String description,
    required double totalAmount,
    required int installmentsCount,
    required String firstDueDate,
  }) async {
    final response = await _dio.post(
      '/financial/deleveraging/simulate',
      data: {
        'description': description,
        'totalAmount': totalAmount,
        'installmentsCount': installmentsCount,
        'firstDueDate': firstDueDate,
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return InvestmentSimulationResultModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    return null;
  }

  /// Lista todos os projetos de expansão FTTH com cálculo de payback (GET /financial/network-projects/payback).
  Future<List<NetworkProjectPaybackModel>> getProjectsWithPayback() async {
    try {
      final response = await _dio.get('/financial/network-projects/payback');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => NetworkProjectPaybackModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Cadastra novo projeto de expansão de rede FTTH (POST /financial/network-projects).
  Future<bool> createNetworkProject({
    required String name,
    required String neighborhood,
    required String city,
    required double budgetAmount,
    required int targetSubscribers,
    required String startDate,
  }) async {
    final response = await _dio.post(
      '/financial/network-projects',
      data: {
        'name': name,
        'neighborhood': neighborhood,
        'city': city,
        'budgetAmount': budgetAmount,
        'targetSubscribers': targetSubscribers,
        'startDate': startDate,
      },
    );
    return response.statusCode == 200 || response.statusCode == 201;
  }

  /// Vincula CTO da rede óptica a um projeto financeiro (POST /financial/network-projects/{projectId}/assign-cto/{ctoId}).
  Future<bool> assignCtoToProject({
    required String projectId,
    required String ctoId,
  }) async {
    final response = await _dio.post(
      '/financial/network-projects/$projectId/assign-cto/$ctoId',
    );
    return response.statusCode == 200 || response.statusCode == 204;
  }

  /// Lista alertas ativos do Sentinela Pericial (GET /financial/sentinel/alerts).
  Future<List<SentinelAuditAlertModel>> getActiveSentinelAlerts() async {
    try {
      final response = await _dio.get('/financial/sentinel/alerts');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => SentinelAuditAlertModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Dispara varredura pericial manual (POST /financial/sentinel/sweep).
  Future<List<SentinelAuditAlertModel>> triggerSentinelSweep() async {
    final response = await _dio.post('/financial/sentinel/sweep');
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .map((e) => SentinelAuditAlertModel.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// Marca alerta pericial como resolvido (POST /financial/sentinel/alerts/{id}/resolve).
  Future<SentinelAuditAlertModel?> resolveSentinelAlert(String alertId) async {
    final response = await _dio.post('/financial/sentinel/alerts/$alertId/resolve');
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return SentinelAuditAlertModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    return null;
  }
}

final strategicFinanceRepositoryProvider =
    Provider<StrategicFinanceRepository>((ref) {
  final api = ref.watch(apiClientProvider);
  return StrategicFinanceRepository(api);
});

final dreReportProvider =
    FutureProvider.autoDispose.family<DreReportModel?, DreFilterParams>((ref, params) async {
  final repo = ref.watch(strategicFinanceRepositoryProvider);
  return repo.getDreReport(params);
});

final deleveragingProjectionProvider =
    FutureProvider.autoDispose<DeleveragingProjectionModel?>((ref) async {
  final repo = ref.watch(strategicFinanceRepositoryProvider);
  return repo.getDeleveragingProjection();
});

final networkProjectsPaybackProvider =
    FutureProvider.autoDispose<List<NetworkProjectPaybackModel>>((ref) async {
  final repo = ref.watch(strategicFinanceRepositoryProvider);
  return repo.getProjectsWithPayback();
});

final activeSentinelAlertsProvider =
    FutureProvider.autoDispose<List<SentinelAuditAlertModel>>((ref) async {
  final repo = ref.watch(strategicFinanceRepositoryProvider);
  return repo.getActiveSentinelAlerts();
});
