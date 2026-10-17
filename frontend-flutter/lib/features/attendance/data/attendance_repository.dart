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

  /// Executa Upgrade/Downgrade de plano com cálculo Pro-Rata e CoA imediato no FreeRADIUS (Onda I).
  Future<ContractLifecycleActionResultModel> changeContractPlan({
    required String contractId,
    required String newPlanId,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/contracts/$contractId/change-plan',
      data: {
        'newPlanId': newPlanId,
        if (notes != null && notes.trim().isNotEmpty) 'notes': notes.trim(),
      },
    );
    return ContractLifecycleActionResultModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  /// Executa Mudança de Endereço com geração automática de O.S. de Instalação e taxa/isenção (Onda I).
  Future<ContractLifecycleActionResultModel> changeContractAddress({
    required String contractId,
    required String newInstallationAddress,
    required String newCity,
    required String newState,
    required String newZipCode,
    String? newCtoId,
    int? newCtoPortNumber,
    double serviceFee = 0.0,
    bool requestWaiver = false,
    String? waiverReason,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/contracts/$contractId/change-address',
      data: {
        'newInstallationAddress': newInstallationAddress.trim(),
        'newCity': newCity.trim(),
        'newState': newState.trim(),
        'newZipCode': newZipCode.trim(),
        if (newCtoId != null && newCtoId.trim().isNotEmpty) 'newCtoId': newCtoId.trim(),
        'newCtoPortNumber': ?newCtoPortNumber,
        'serviceFee': serviceFee,
        'requestWaiver': requestWaiver,
        if (waiverReason != null && waiverReason.trim().isNotEmpty) 'waiverReason': waiverReason.trim(),
        if (notes != null && notes.trim().isNotEmpty) 'notes': notes.trim(),
      },
    );
    return ContractLifecycleActionResultModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  /// Executa Troca de Titularidade migrando faturas em aberto/vencidas e gerando Assinatura Forense (Onda I).
  Future<ContractLifecycleActionResultModel> transferContractOwnership({
    required String contractId,
    required String newCustomerId,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/contracts/$contractId/transfer-ownership',
      data: {
        'newCustomerId': newCustomerId.trim(),
        if (notes != null && notes.trim().isNotEmpty) 'notes': notes.trim(),
      },
    );
    return ContractLifecycleActionResultModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  /// Lista os planos ativos para Upgrade/Downgrade (Onda I).
  Future<List<PlanOptionModel>> getActivePlans() async {
    try {
      final response = await _dio.get('/plans/active');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((item) => PlanOptionModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Lista o histórico auditado de eventos pós-venda do contrato (Onda I).
  Future<List<ContractLifecycleEventModel>> getContractLifecycleEvents(String contractId) async {
    try {
      final response = await _dio.get('/contracts/$contractId/lifecycle-events');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((item) => ContractLifecycleEventModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Consulta se há incidente massivo de rede óptica FTTH ativo afetando a CTO/PON do contrato (Onda J).
  Future<ActiveNetworkIncidentModel?> getActiveIncidentForContract(String contractId) async {
    try {
      final response = await _dio.get('/ftth/monitoring/incidents/active/contract/$contractId');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        final data = response.data as Map<String, dynamic>;
        if (data['id'] != null && data['id'].toString().isNotEmpty) {
          return ActiveNetworkIncidentModel.fromJson(data);
        }
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Consulta o estado TR-069 / CWMP Dual-Band Wi-Fi e diagnóstico da ONU/CPE do contrato (Onda M).
  Future<Tr069CpeStatusModel> getTr069CpeStatus(String contractId) async {
    final response = await _dio.get('/ftth/tr069/contracts/$contractId');
    return Tr069CpeStatusModel.fromJson(response.data as Map<String, dynamic>);
  }

  /// Atualiza SSID, senha WPA2/WPA3, canais 2.4GHz/5GHz e Band Steering via TR-069 SetParameterValues (Onda M).
  Future<Tr069CpeStatusModel> updateTr069Wifi({
    required String contractId,
    required String ssid24ghz,
    required String ssid5ghz,
    String? password,
    String channel24ghz = 'AUTO',
    String channel5ghz = 'AUTO',
    bool bandSteeringEnabled = true,
  }) async {
    final response = await _dio.put(
      '/ftth/tr069/contracts/$contractId/wifi',
      data: {
        'ssid24ghz': ssid24ghz.trim(),
        'ssid5ghz': ssid5ghz.trim(),
        if (password != null && password.trim().isNotEmpty) 'password': password.trim(),
        'channel24ghz': channel24ghz,
        'channel5ghz': channel5ghz,
        'bandSteeringEnabled': bandSteeringEnabled,
      },
    );
    return Tr069CpeStatusModel.fromJson(response.data as Map<String, dynamic>);
  }

  /// Despacha comando TR-069 Reboot remoto para a ONU/CPE do contrato (Onda M).
  Future<Tr069CommandLogModel> rebootTr069Cpe(String contractId) async {
    final response = await _dio.post('/ftth/tr069/contracts/$contractId/reboot');
    return Tr069CommandLogModel.fromJson(response.data as Map<String, dynamic>);
  }
}

/// Provider do repositório de atendimento.
final attendanceRepositoryProvider = Provider<AttendanceRepository>((ref) {
  final api = ref.watch(apiClientProvider);
  return AttendanceRepository(api);
});

