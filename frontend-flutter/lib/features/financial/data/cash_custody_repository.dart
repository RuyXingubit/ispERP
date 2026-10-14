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

/// Modelo de Transferência Física de Espécie entre Colaboradores (CashTransferResponseDto).
class CashTransferModel {
  final String id;
  final String senderUserId;
  final String senderUserName;
  final String receiverUserId;
  final String receiverUserName;
  final double amount;
  final String? reason;
  final String status;
  final String? requestedAt;
  final String? respondedAt;
  final String? notes;

  const CashTransferModel({
    required this.id,
    required this.senderUserId,
    required this.senderUserName,
    required this.receiverUserId,
    required this.receiverUserName,
    required this.amount,
    this.reason,
    required this.status,
    this.requestedAt,
    this.respondedAt,
    this.notes,
  });

  factory CashTransferModel.fromJson(Map<String, dynamic> json) {
    return CashTransferModel(
      id: json['id']?.toString() ?? '',
      senderUserId: json['senderUserId']?.toString() ?? '',
      senderUserName: json['senderUserName']?.toString() ?? 'Remetente',
      receiverUserId: json['receiverUserId']?.toString() ?? '',
      receiverUserName: json['receiverUserName']?.toString() ?? 'Destinatário',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      reason: json['reason']?.toString(),
      status: json['status']?.toString() ?? 'PENDING_ACCEPTANCE',
      requestedAt: json['requestedAt']?.toString(),
      respondedAt: json['respondedAt']?.toString(),
      notes: json['notes']?.toString(),
    );
  }
}

/// Modelo de Depósito Bancário para Conciliação de Custódia (BankDepositResponseDto).
class BankDepositModel {
  final String id;
  final String depositorUserId;
  final String depositorUserName;
  final String? depositorCpf;
  final double amount;
  final String bankName;
  final String? bankAgency;
  final String? bankAccount;
  final String receiptFileUrl;
  final String status;
  final String? depositDate;
  final String? auditedByUserId;
  final String? auditedByUserName;

  const BankDepositModel({
    required this.id,
    required this.depositorUserId,
    required this.depositorUserName,
    this.depositorCpf,
    required this.amount,
    required this.bankName,
    this.bankAgency,
    this.bankAccount,
    required this.receiptFileUrl,
    required this.status,
    this.depositDate,
    this.auditedByUserId,
    this.auditedByUserName,
  });

  factory BankDepositModel.fromJson(Map<String, dynamic> json) {
    return BankDepositModel(
      id: json['id']?.toString() ?? '',
      depositorUserId: json['depositorUserId']?.toString() ?? '',
      depositorUserName: json['depositorUserName']?.toString() ?? 'Colaborador',
      depositorCpf: json['depositorCpf']?.toString(),
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      bankName: json['bankName']?.toString() ?? '',
      bankAgency: json['bankAgency']?.toString(),
      bankAccount: json['bankAccount']?.toString(),
      receiptFileUrl: json['receiptFileUrl']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING_AUDIT',
      depositDate: json['depositDate']?.toString(),
      auditedByUserId: json['auditedByUserId']?.toString(),
      auditedByUserName: json['auditedByUserName']?.toString(),
    );
  }
}

/// Modelo de Taxa de Ordem de Serviço & Alçada de Isenção (WorkOrderFeeDto).
class WorkOrderFeeModel {
  final String workOrderId;
  final String protocol;
  final String customerId;
  final String customerName;
  final String serviceType;
  final double standardFeeAmount;
  final String feeStatus;
  final String? waiverReason;
  final String? waiverRequestedByUserId;
  final String? waiverRequestedByName;
  final String? waiverAuditedByUserId;
  final String? waiverAuditedByName;
  final String? waiverAuditedAt;

  const WorkOrderFeeModel({
    required this.workOrderId,
    required this.protocol,
    required this.customerId,
    required this.customerName,
    required this.serviceType,
    required this.standardFeeAmount,
    required this.feeStatus,
    this.waiverReason,
    this.waiverRequestedByUserId,
    this.waiverRequestedByName,
    this.waiverAuditedByUserId,
    this.waiverAuditedByName,
    this.waiverAuditedAt,
  });

  factory WorkOrderFeeModel.fromJson(Map<String, dynamic> json) {
    return WorkOrderFeeModel(
      workOrderId: json['workOrderId']?.toString() ?? '',
      protocol: json['protocol']?.toString() ?? '',
      customerId: json['customerId']?.toString() ?? '',
      customerName: json['customerName']?.toString() ?? 'Assinante',
      serviceType: json['serviceType']?.toString() ?? 'VISITA_TECNICA',
      standardFeeAmount: (json['standardFeeAmount'] as num?)?.toDouble() ?? 0.0,
      feeStatus: json['feeStatus']?.toString() ?? 'PENDING_WAIVER_APPROVAL',
      waiverReason: json['waiverReason']?.toString(),
      waiverRequestedByUserId: json['waiverRequestedByUserId']?.toString(),
      waiverRequestedByName: json['waiverRequestedByName']?.toString(),
      waiverAuditedByUserId: json['waiverAuditedByUserId']?.toString(),
      waiverAuditedByName: json['waiverAuditedByName']?.toString(),
      waiverAuditedAt: json['waiverAuditedAt']?.toString(),
    );
  }
}

/// Repositório para Custódia de Caixa, Repasses Dual-Acceptance, Depósitos,
/// Alçadas de Taxa de O.S. e Ações Financeiras (/financial/custody e /financial/work-orders).
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

  /// Consulta o saldo sob custódia de um colaborador específico (GET /financial/custody/cash/user/{userId}).
  Future<CashCustodyModel?> getCashCustodyByUserId(String userId) async {
    try {
      final response = await _dio.get('/financial/custody/cash/user/$userId');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return CashCustodyModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Registra recebimento de fatura em dinheiro vivo e debita na custódia do CPF (POST /financial/custody/cash/settle).
  Future<CashCustodyModel?> recordCashSettlement({
    required String userId,
    required String invoiceId,
    required double amount,
    String? receiptNumber,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/financial/custody/cash/settle',
      options: Options(headers: {'X-User-Id': userId}),
      data: {
        'invoiceId': invoiceId,
        'amount': amount,
        if (receiptNumber != null && receiptNumber.isNotEmpty) 'receiptNumber': receiptNumber,
        if (notes != null && notes.isNotEmpty) 'notes': notes,
      },
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return CashCustodyModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Solicita transferência de valores para outro colaborador (POST /financial/custody/cash/transfer/request).
  Future<CashTransferModel?> requestCashTransfer({
    required String senderUserId,
    required String receiverUserId,
    required double amount,
    String? reason,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/financial/custody/cash/transfer/request',
      options: Options(headers: {'X-User-Id': senderUserId}),
      data: {
        'receiverUserId': receiverUserId,
        'amount': amount,
        if (reason != null && reason.isNotEmpty) 'reason': reason,
        if (notes != null && notes.isNotEmpty) 'notes': notes,
      },
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return CashTransferModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Lista transferências de dinheiro pendentes de aceite para o colaborador (GET /financial/custody/cash/transfer/pending).
  Future<List<CashTransferModel>> getPendingCashTransfers(String userId) async {
    try {
      final response = await _dio.get(
        '/financial/custody/cash/transfer/pending',
        options: Options(headers: {'X-User-Id': userId}),
      );
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => CashTransferModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Aceita ou rejeita transferência de custódia recebida (POST /financial/custody/cash/transfer/{id}/respond?accept=...).
  Future<CashTransferModel?> respondCashTransfer({
    required String userId,
    required String transferId,
    required bool accept,
  }) async {
    final response = await _dio.post(
      '/financial/custody/cash/transfer/$transferId/respond',
      queryParameters: {'accept': accept},
      options: Options(headers: {'X-User-Id': userId}),
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return CashTransferModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
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
    String? bankAgency,
    String? bankAccount,
    required String receiptFileUrl,
  }) async {
    final response = await _dio.post(
      '/financial/custody/cash/deposit',
      options: Options(headers: {'X-User-Id': userId}),
      data: {
        'amount': amount,
        'bankName': bankName,
        if (bankAgency != null && bankAgency.isNotEmpty) 'bankAgency': bankAgency,
        if (bankAccount != null && bankAccount.isNotEmpty) 'bankAccount': bankAccount,
        'receiptFileUrl': receiptFileUrl,
      },
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return BankDepositModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Audita e concilia depósito bancário no extrato (POST /financial/custody/cash/deposit/{id}/audit).
  Future<BankDepositModel?> auditBankDeposit({
    required String auditorUserId,
    required String depositId,
    required bool approved,
    String? notes,
    String? rejectionReason,
  }) async {
    final response = await _dio.post(
      '/financial/custody/cash/deposit/$depositId/audit',
      options: Options(headers: {'X-User-Id': auditorUserId}),
      data: {
        'approved': approved,
        if (notes != null && notes.isNotEmpty) 'notes': notes,
        if (rejectionReason != null && rejectionReason.isNotEmpty)
          'rejectionReason': rejectionReason,
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return BankDepositModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Atribui taxa padrão de serviço a uma Ordem de Serviço (POST /financial/work-orders/{id}/assign-fee).
  Future<WorkOrderFeeModel?> assignStandardWorkOrderFee({
    required String workOrderId,
    required double amount,
  }) async {
    final response = await _dio.post(
      '/financial/work-orders/$workOrderId/assign-fee',
      queryParameters: {'amount': amount},
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return WorkOrderFeeModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Solicita isenção de cobrança de taxa de O.S. (POST /financial/work-orders/waiver/request).
  Future<WorkOrderFeeModel?> requestWorkOrderFeeWaiver({
    required String userId,
    required String workOrderId,
    required String waiverReason,
  }) async {
    final response = await _dio.post(
      '/financial/work-orders/waiver/request',
      options: Options(headers: {'X-User-Id': userId}),
      data: {
        'workOrderId': workOrderId,
        'waiverReason': waiverReason,
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return WorkOrderFeeModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Lista pedidos de isenção de taxa de O.S. pendentes de aprovação (GET /financial/work-orders/waiver/pending).
  Future<List<WorkOrderFeeModel>> getPendingWorkOrderFeeWaivers() async {
    try {
      final response = await _dio.get('/financial/work-orders/waiver/pending');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => WorkOrderFeeModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Aprova ou rejeita pedido de isenção de taxa de O.S. (POST /financial/work-orders/{id}/waiver/audit).
  Future<WorkOrderFeeModel?> auditWorkOrderFeeWaiver({
    required String auditorUserId,
    required String workOrderId,
    required bool approved,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/financial/work-orders/$workOrderId/waiver/audit',
      options: Options(headers: {'X-User-Id': auditorUserId}),
      data: {
        'approved': approved,
        if (notes != null && notes.isNotEmpty) 'notes': notes,
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return WorkOrderFeeModel.fromJson(response.data as Map<String, dynamic>);
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

final pendingWorkOrderFeeWaiversProvider = FutureProvider.autoDispose<List<WorkOrderFeeModel>>((ref) async {
  final repo = ref.watch(cashCustodyRepositoryProvider);
  return repo.getPendingWorkOrderFeeWaivers();
});

final pendingCashTransfersProvider =
    FutureProvider.autoDispose.family<List<CashTransferModel>, String>((ref, userId) async {
  if (userId.isEmpty) return [];
  final repo = ref.watch(cashCustodyRepositoryProvider);
  return repo.getPendingCashTransfers(userId);
});
