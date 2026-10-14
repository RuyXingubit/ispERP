import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import '../../inventory/data/inventory_models.dart';
import 'technician_execution_models.dart';

/// Repositório para execução técnica de campo (OLT Auto-Find, Provisionamento, FreeRADIUS e Logística Reversa).
class TechnicianExecutionRepository {
  final ApiClient _apiClient;

  TechnicianExecutionRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Lista todas as Ordens de Serviço da fila técnica.
  Future<List<Map<String, dynamic>>> listWorkOrders() async {
    final res = await _dio.get('/work-orders');
    if (res.statusCode == 200 && res.data is List) {
      return List<Map<String, dynamic>>.from(res.data as List);
    }
    return [];
  }

  /// Inicia o deslocamento/execução de uma O.S. (EM_ANDAMENTO).
  Future<void> startWorkOrder(String workOrderId) async {
    await _dio.patch(
      '/work-orders/$workOrderId/status',
      queryParameters: {'status': 'EM_ANDAMENTO'},
    );
  }

  /// Auto-Find: Lista ONUs recém-conectadas nas portas PON da OLT aguardando autorização.
  Future<List<OltUnprovisionedOnuModel>> listUnprovisionedOnus(
    String workOrderId,
  ) async {
    final res = await _dio.get(
      '/technician/execution/$workOrderId/unprovisioned-onus',
    );
    if (res.statusCode == 200 && res.data is List) {
      return (res.data as List)
          .whereType<Map<String, dynamic>>()
          .map(OltUnprovisionedOnuModel.fromJson)
          .toList();
    }
    return [];
  }

  /// Provisiona a ONU em 1 clique na OLT vinculando VLAN e credenciais PPPoE.
  Future<ProvisionOnuResult> provisionOnu({
    required String workOrderId,
    required String onuSerial,
    int vlanId = 100,
    String? pppoeUsername,
    String? pppoePassword,
  }) async {
    final cleanUser = pppoeUsername?.trim();
    final cleanPass = pppoePassword?.trim();
    final res = await _dio.post(
      '/technician/execution/$workOrderId/provision',
      data: {
        'onuSerial': onuSerial.trim(),
        'vlanId': vlanId,
        if (cleanUser != null && cleanUser.isNotEmpty)
          'pppoeUsername': cleanUser,
        if (cleanPass != null && cleanPass.isNotEmpty)
          'pppoePassword': cleanPass,
      },
    );
    if (res.data is Map<String, dynamic>) {
      return ProvisionOnuResult.fromJson(res.data as Map<String, dynamic>);
    }
    return const ProvisionOnuResult(
      success: true,
      message: 'ONU provisionada com sucesso.',
    );
  }

  /// Valida em tempo real no FreeRADIUS se a ONU subiu sessão PPPoE autenticada.
  Future<RadiusSessionStatusModel> getRadiusStatus(String workOrderId) async {
    final res = await _dio.get(
      '/technician/execution/$workOrderId/radius-status',
    );
    if (res.data is Map<String, dynamic>) {
      return RadiusSessionStatusModel.fromJson(
        res.data as Map<String, dynamic>,
      );
    }
    return RadiusSessionStatusModel(
      workOrderId: workOrderId,
      authenticated: false,
    );
  }

  /// Conclui a execução da O.S. registrando Serial, MAC, Sinal Óptico e Assinatura.
  Future<Map<String, dynamic>> completeInstallation(
    String workOrderId,
    TechnicianExecutionCompletePayload payload,
  ) async {
    final res = await _dio.post(
      '/technician/execution/$workOrderId/complete',
      data: payload.toJson(),
    );
    if (res.data is Map<String, dynamic>) {
      return res.data as Map<String, dynamic>;
    }
    return {'id': workOrderId, 'status': 'COMPLETED'};
  }

  /// Conclui uma O.S. de RETIRADA devolvendo o equipamento recolhido ao almoxarifado.
  Future<Map<String, dynamic>> completeRemoval(
    String workOrderId,
    CompleteRemovalPayload payload,
  ) async {
    final res = await _dio.post(
      '/work-orders/$workOrderId/complete-removal',
      data: payload.toJson(),
    );
    if (res.data is Map<String, dynamic>) {
      return res.data as Map<String, dynamic>;
    }
    return {'id': workOrderId, 'status': 'COMPLETED'};
  }

  /// Registra tentativa infrutífera de recolha (O.S. RETIRADA), acionando cobrança jurídica / SPC.
  Future<LegalCollectionResultModel> failRemoval(
    String workOrderId,
    FailRemovalPayload payload,
  ) async {
    final res = await _dio.post(
      '/work-orders/$workOrderId/fail-removal',
      data: payload.toJson(),
    );
    if (res.data is Map<String, dynamic>) {
      return LegalCollectionResultModel.fromJson(
        res.data as Map<String, dynamic>,
      );
    }
    return LegalCollectionResultModel(
      id: '',
      customerId: '',
      contractId: '',
      workOrderId: workOrderId,
      totalDebtInvoices: 0,
      equipmentIndemnityAmount: 0,
      totalClaimAmount: 0,
      status: 'PENDING_BUREAU_SUBMISSION',
    );
  }

  /// Lista os almoxarifados disponíveis para devolução de equipamento recolhido em campo.
  Future<List<WarehouseModel>> listWarehouses() async {
    try {
      final res = await _dio.get('/warehouses');
      if (res.statusCode == 200 && res.data is List) {
        return (res.data as List)
            .whereType<Map<String, dynamic>>()
            .map(WarehouseModel.fromJson)
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }
}

final technicianExecutionRepositoryProvider =
    Provider<TechnicianExecutionRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return TechnicianExecutionRepository(apiClient);
});
