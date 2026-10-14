import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Modelo de Custódia Pessoal de Caixa por CPF (CashCustodyDto).
class CashCustodyModel {
  final String id;
  final String userId;
  final String userName;
  final String userEmail;
  final String userRole;
  final String? cpf;
  final double currentBalance;
  final String? updatedAt;

  const CashCustodyModel({
    required this.id,
    required this.userId,
    required this.userName,
    required this.userEmail,
    required this.userRole,
    this.cpf,
    required this.currentBalance,
    this.updatedAt,
  });

  factory CashCustodyModel.fromJson(Map<String, dynamic> json) {
    return CashCustodyModel(
      id: json['id']?.toString() ?? '',
      userId: json['userId']?.toString() ?? '',
      userName: json['userName']?.toString() ?? 'Colaborador',
      userEmail: json['userEmail']?.toString() ?? '',
      userRole: json['userRole']?.toString() ?? 'SUPPORT',
      cpf: json['cpf']?.toString(),
      currentBalance: (json['currentBalance'] as num?)?.toDouble() ?? 0.0,
      updatedAt: json['updatedAt']?.toString(),
    );
  }
}

/// Modelo de Depósito Bancário para Conciliação de Custódia (BankDepositResponseDto).
class BankDepositModel {
  final String id;
  final String depositorUserId;
  final String depositorUserName;
  final double amount;
  final String bankName;
  final String receiptFileUrl;
  final String status;
  final String? depositDate;

  const BankDepositModel({
    required this.id,
    required this.depositorUserId,
    required this.depositorUserName,
    required this.amount,
    required this.bankName,
    required this.receiptFileUrl,
    required this.status,
    this.depositDate,
  });

  factory BankDepositModel.fromJson(Map<String, dynamic> json) {
    return BankDepositModel(
      id: json['id']?.toString() ?? '',
      depositorUserId: json['depositorUserId']?.toString() ?? '',
      depositorUserName: json['depositorUserName']?.toString() ?? 'Colaborador',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      bankName: json['bankName']?.toString() ?? '',
      receiptFileUrl: json['receiptFileUrl']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING_AUDIT',
      depositDate: json['depositDate']?.toString(),
    );
  }
}

/// Repositório para Custódia de Caixa, Depósitos e Ações Financeiras (/financial/custody e /radius/lifecycle/action).
class CashCustodyRepository {
  final ApiClient _apiClient;

  CashCustodyRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Lista todos os saldos sob custódia de colaboradores na empresa (GET /financial/custody/cash/all).
  Future<List<CashCustodyModel>> getAllCashCustodies() async {
    try {
      final response = await _dio.get('/financial/custody/cash/all');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => CashCustodyModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Lista depósitos bancários pendentes de conciliação (GET /financial/custody/cash/deposit/pending).
  Future<List<BankDepositModel>> getPendingBankDeposits() async {
    try {
      final response = await _dio.get('/financial/custody/cash/deposit/pending');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => BankDepositModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Submete comprovante de depósito bancário / prestação de contas (POST /financial/custody/cash/deposit).
  Future<BankDepositModel?> submitBankDeposit({
    required String userId,
    required double amount,
    required String bankName,
    required String receiptFileUrl,
  }) async {
    final response = await _dio.post(
      '/financial/custody/cash/deposit',
      options: Options(headers: {'X-User-Id': userId}),
      data: {
        'amount': amount,
        'bankName': bankName,
        'receiptFileUrl': receiptFileUrl,
      },
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return BankDepositModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Executa Desbloqueio em Confiança / desbloqueio administrativo no RADIUS (POST /radius/lifecycle/action).
  Future<bool> executeTrustUnblock({
    required String contractId,
    String? reason,
  }) async {
    try {
      final response = await _dio.post(
        '/radius/lifecycle/action',
        data: {
          'contractId': contractId,
          'action': 'UNBLOCK',
          'reason': reason ?? 'Desbloqueio em Confiança (48h) via Painel Financeiro',
          'sendPod': true,
        },
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}

final cashCustodyRepositoryProvider = Provider<CashCustodyRepository>((ref) {
  final api = ref.watch(apiClientProvider);
  return CashCustodyRepository(api);
});

final allCashCustodiesProvider = FutureProvider.autoDispose<List<CashCustodyModel>>((ref) async {
  final repo = ref.watch(cashCustodyRepositoryProvider);
  return repo.getAllCashCustodies();
});

final pendingBankDepositsProvider = FutureProvider.autoDispose<List<BankDepositModel>>((ref) async {
  final repo = ref.watch(cashCustodyRepositoryProvider);
  return repo.getPendingBankDeposits();
});
