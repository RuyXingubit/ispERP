import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Modelo de Conta Contábil do Plano de Contas (ChartOfAccountDto).
class ChartOfAccountModel {
  final String id;
  final String? parentId;
  final String? parentCode;
  final String code;
  final String name;
  final String accountType;
  final String dreCategory;
  final int level;
  final bool acceptsEntries;
  final bool active;
  final List<ChartOfAccountModel> children;

  const ChartOfAccountModel({
    required this.id,
    this.parentId,
    this.parentCode,
    required this.code,
    required this.name,
    required this.accountType,
    required this.dreCategory,
    required this.level,
    required this.acceptsEntries,
    required this.active,
    this.children = const [],
  });

  factory ChartOfAccountModel.fromJson(Map<String, dynamic> json) {
    final rawChildren = json['children'];
    return ChartOfAccountModel(
      id: json['id']?.toString() ?? '',
      parentId: json['parentId']?.toString(),
      parentCode: json['parentCode']?.toString(),
      code: json['code']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      accountType: json['accountType']?.toString() ?? 'OPEX',
      dreCategory: json['dreCategory']?.toString() ?? 'OPEX_ADMIN',
      level: (json['level'] as num?)?.toInt() ?? 1,
      acceptsEntries: json['acceptsEntries'] as bool? ?? true,
      active: json['active'] as bool? ?? true,
      children: rawChildren is List
          ? rawChildren
              .map((c) => ChartOfAccountModel.fromJson(c as Map<String, dynamic>))
              .toList()
          : const [],
    );
  }
}

/// Modelo de Parcela de Conta a Pagar (ExpenseInstallmentDto).
class ExpenseInstallmentModel {
  final String id;
  final String payableInvoiceId;
  final int installmentNumber;
  final String dueDate;
  final double amount;
  final double? paidAmount;
  final String? paidAt;
  final String status;
  final String? paymentMethod;
  final String? receiptUrl;

  const ExpenseInstallmentModel({
    required this.id,
    required this.payableInvoiceId,
    required this.installmentNumber,
    required this.dueDate,
    required this.amount,
    this.paidAmount,
    this.paidAt,
    required this.status,
    this.paymentMethod,
    this.receiptUrl,
  });

  factory ExpenseInstallmentModel.fromJson(Map<String, dynamic> json) {
    return ExpenseInstallmentModel(
      id: json['id']?.toString() ?? '',
      payableInvoiceId: json['payableInvoiceId']?.toString() ?? '',
      installmentNumber: (json['installmentNumber'] as num?)?.toInt() ?? 1,
      dueDate: json['dueDate']?.toString() ?? '',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      paidAmount: (json['paidAmount'] as num?)?.toDouble(),
      paidAt: json['paidAt']?.toString(),
      status: json['status']?.toString() ?? 'PENDING',
      paymentMethod: json['paymentMethod']?.toString(),
      receiptUrl: json['receiptUrl']?.toString(),
    );
  }
}

/// Modelo de Despesa / Nota Fiscal a Pagar (PayableInvoiceDto).
class PayableInvoiceModel {
  final String id;
  final String supplierName;
  final String? supplierDocument;
  final String chartOfAccountId;
  final String? chartOfAccountCode;
  final String? chartOfAccountName;
  final String description;
  final String? invoiceNumber;
  final double totalAmount;
  final String issueDate;
  final String status;
  final String? projectId;
  final String? projectName;
  final List<ExpenseInstallmentModel> installments;
  final String? createdAt;

  const PayableInvoiceModel({
    required this.id,
    required this.supplierName,
    this.supplierDocument,
    required this.chartOfAccountId,
    this.chartOfAccountCode,
    this.chartOfAccountName,
    required this.description,
    this.invoiceNumber,
    required this.totalAmount,
    required this.issueDate,
    required this.status,
    this.projectId,
    this.projectName,
    this.installments = const [],
    this.createdAt,
  });

  factory PayableInvoiceModel.fromJson(Map<String, dynamic> json) {
    final rawInst = json['installments'];
    return PayableInvoiceModel(
      id: json['id']?.toString() ?? '',
      supplierName: json['supplierName']?.toString() ?? '',
      supplierDocument: json['supplierDocument']?.toString(),
      chartOfAccountId: json['chartOfAccountId']?.toString() ?? '',
      chartOfAccountCode: json['chartOfAccountCode']?.toString(),
      chartOfAccountName: json['chartOfAccountName']?.toString(),
      description: json['description']?.toString() ?? '',
      invoiceNumber: json['invoiceNumber']?.toString(),
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0.0,
      issueDate: json['issueDate']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING',
      projectId: json['projectId']?.toString(),
      projectName: json['projectName']?.toString(),
      installments: rawInst is List
          ? rawInst
              .map((i) => ExpenseInstallmentModel.fromJson(i as Map<String, dynamic>))
              .toList()
          : const [],
      createdAt: json['createdAt']?.toString(),
    );
  }
}

/// Repositório para Plano de Contas Hierárquico e Contas a Pagar (/financial/chart-of-accounts e /financial/payables).
class PayablesAndAccountsRepository {
  final ApiClient _apiClient;

  PayablesAndAccountsRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Retorna a árvore completa do Plano de Contas (GET /financial/chart-of-accounts/tree).
  Future<List<ChartOfAccountModel>> getChartOfAccountsTree() async {
    try {
      final response = await _dio.get('/financial/chart-of-accounts/tree');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => ChartOfAccountModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Lista todas as contas contábeis ativas em formato plano (GET /financial/chart-of-accounts).
  Future<List<ChartOfAccountModel>> getChartOfAccountsFlat() async {
    try {
      final response = await _dio.get('/financial/chart-of-accounts');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => ChartOfAccountModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Cria uma nova conta no Plano de Contas (POST /financial/chart-of-accounts).
  Future<ChartOfAccountModel?> createChartOfAccount({
    String? parentId,
    required String code,
    required String name,
    required String accountType,
    required String dreCategory,
    bool acceptsEntries = true,
    bool active = true,
  }) async {
    final response = await _dio.post(
      '/financial/chart-of-accounts',
      data: {
        if (parentId != null && parentId.isNotEmpty) 'parentId': parentId,
        'code': code,
        'name': name,
        'accountType': accountType,
        'dreCategory': dreCategory,
        'acceptsEntries': acceptsEntries,
        'active': active,
      },
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return ChartOfAccountModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Atualiza uma conta contábil existente (PUT /financial/chart-of-accounts/{id}).
  Future<ChartOfAccountModel?> updateChartOfAccount({
    required String id,
    String? parentId,
    required String code,
    required String name,
    required String accountType,
    required String dreCategory,
    bool acceptsEntries = true,
    bool active = true,
  }) async {
    final response = await _dio.put(
      '/financial/chart-of-accounts/$id',
      data: {
        if (parentId != null && parentId.isNotEmpty) 'parentId': parentId,
        'code': code,
        'name': name,
        'accountType': accountType,
        'dreCategory': dreCategory,
        'acceptsEntries': acceptsEntries,
        'active': active,
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return ChartOfAccountModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Lista todas as contas a pagar e despesas lançadas (GET /financial/payables).
  Future<List<PayableInvoiceModel>> getAllPayableInvoices() async {
    try {
      final response = await _dio.get('/financial/payables');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => PayableInvoiceModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Lista parcelas de despesas pendentes de pagamento (GET /financial/payables/installments/pending).
  Future<List<ExpenseInstallmentModel>> getPendingExpenseInstallments() async {
    try {
      final response = await _dio.get('/financial/payables/installments/pending');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => ExpenseInstallmentModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Lança nova conta a pagar e gera cronograma de parcelas (POST /financial/payables).
  Future<PayableInvoiceModel?> createPayableInvoice({
    required String supplierName,
    String? supplierDocument,
    required String chartOfAccountId,
    required String description,
    String? invoiceNumber,
    required double totalAmount,
    required String issueDate,
    required int installmentsCount,
    required String firstDueDate,
    String? projectId,
  }) async {
    final response = await _dio.post(
      '/financial/payables',
      data: {
        'supplierName': supplierName,
        if (supplierDocument != null && supplierDocument.isNotEmpty)
          'supplierDocument': supplierDocument,
        'chartOfAccountId': chartOfAccountId,
        'description': description,
        if (invoiceNumber != null && invoiceNumber.isNotEmpty)
          'invoiceNumber': invoiceNumber,
        'totalAmount': totalAmount,
        'issueDate': issueDate,
        'installmentsCount': installmentsCount,
        'firstDueDate': firstDueDate,
        if (projectId != null && projectId.isNotEmpty) 'projectId': projectId,
      },
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return PayableInvoiceModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Registra baixa de pagamento de uma parcela de despesa (POST /financial/payables/installments/{id}/pay).
  Future<ExpenseInstallmentModel?> payExpenseInstallment({
    required String installmentId,
    double? paidAmount,
    String paymentMethod = 'TRANSFERENCIA_BANCARIA',
    String? receiptUrl,
  }) async {
    final response = await _dio.post(
      '/financial/payables/installments/$installmentId/pay',
      queryParameters: {
        'paidAmount': ?paidAmount,
        'paymentMethod': paymentMethod,
        if (receiptUrl != null && receiptUrl.isNotEmpty) 'receiptUrl': receiptUrl,
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return ExpenseInstallmentModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }
}

final payablesAndAccountsRepositoryProvider =
    Provider<PayablesAndAccountsRepository>((ref) {
  final api = ref.watch(apiClientProvider);
  return PayablesAndAccountsRepository(api);
});

final chartOfAccountsTreeProvider =
    FutureProvider.autoDispose<List<ChartOfAccountModel>>((ref) async {
  final repo = ref.watch(payablesAndAccountsRepositoryProvider);
  return repo.getChartOfAccountsTree();
});

final chartOfAccountsFlatProvider =
    FutureProvider.autoDispose<List<ChartOfAccountModel>>((ref) async {
  final repo = ref.watch(payablesAndAccountsRepositoryProvider);
  return repo.getChartOfAccountsFlat();
});

final allPayableInvoicesProvider =
    FutureProvider.autoDispose<List<PayableInvoiceModel>>((ref) async {
  final repo = ref.watch(payablesAndAccountsRepositoryProvider);
  return repo.getAllPayableInvoices();
});

final pendingExpenseInstallmentsProvider =
    FutureProvider.autoDispose<List<ExpenseInstallmentModel>>((ref) async {
  final repo = ref.watch(payablesAndAccountsRepositoryProvider);
  return repo.getPendingExpenseInstallments();
});
