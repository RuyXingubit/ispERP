import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'attendance_models.dart';
import 'customer_search_model.dart';

/// Repositório de integração com as rotas operacionais do ispERP.
class AttendanceRepository {
  final ApiClient _apiClient;

  AttendanceRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Busca assinantes por CPF ou Nome contra a API real.
  Future<List<CustomerSearchModel>> searchCustomers(String query) async {
    final cleanQuery = query.trim();
    if (cleanQuery.isEmpty) return [];

    try {
      final response = await _dio.get(
        '/customers/search',
        queryParameters: {'q': cleanQuery},
      );

      if (response.statusCode == 200 && response.data is List) {
        final list = response.data as List;
        return list
            .map((item) => CustomerSearchModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Retorna os contratos do assinante.
  Future<List<ContractSummary>> getCustomerContracts(String customerId) async {
    try {
      final response = await _dio.get('/contracts/customer/$customerId');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((item) => ContractSummary.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Retorna as faturas do assinante (com destaque para pendentes).
  Future<List<PendingInvoice>> getCustomerInvoices(String customerId) async {
    try {
      final response = await _dio.get('/invoices/customer/$customerId');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((item) => PendingInvoice.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Busca a O.S. ativa associada ao contrato para a Linha do Tempo Viva.
  Future<LiveWorkOrder?> getContractWorkOrder(String contractId) async {
    try {
      final response = await _dio.get('/work-orders/contract/$contractId');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return LiveWorkOrder.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Efetua a baixa da fatura informando o valor e meio de pagamento.
  Future<bool> payInvoice({
    required String invoiceId,
    required double amount,
    required String paymentMethod,
  }) async {
    try {
      final response = await _dio.post(
        '/invoices/$invoiceId/pay',
        data: {
          'paidAmount': amount,
          'paymentMethod': paymentMethod,
        },
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}

/// Provider do repositório de atendimento.
final attendanceRepositoryProvider = Provider<AttendanceRepository>((ref) {
  final api = ref.watch(apiClientProvider);
  return AttendanceRepository(api);
});
