import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'dispatch_models.dart';

/// Repositório de integração com a Torre de Controle de Despacho e Triagem FTTH.
class DispatchRepository {
  final ApiClient _apiClient;

  DispatchRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Lista todas as demandas de instalação e dimensionamento FTTH.
  Future<List<InstallationDemandModel>> listDemands() async {
    try {
      final response = await _dio.get('/dispatch/installations/demands');
      if (response.statusCode == 200 && response.data is List) {
        final list = response.data as List;
        return list
            .map((item) => InstallationDemandModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      rethrow;
    }
  }

  /// Obtém detalhes da demanda de uma O.S. específica.
  Future<InstallationDemandModel?> getDemand(String workOrderId) async {
    try {
      final response = await _dio.get('/dispatch/installations/demands/$workOrderId');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return InstallationDemandModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  /// Lista técnicos candidatos para a O.S., auditados e ordenados por kit e proximidade GPS.
  Future<List<TechnicianCandidateModel>> listCandidates(String workOrderId) async {
    try {
      final response = await _dio.get('/dispatch/installations/$workOrderId/candidates');
      if (response.statusCode == 200 && response.data is List) {
        final list = response.data as List;
        return list
            .map((item) => TechnicianCandidateModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  /// Despacha a O.S. para o técnico selecionado, alocando os materiais no veículo.
  Future<bool> dispatchWorkOrder(String workOrderId, String technicianId) async {
    try {
      final response = await _dio.post(
        '/dispatch/installations/$workOrderId/dispatch',
        queryParameters: {'technicianId': technicianId},
      );
      return response.statusCode == 200;
    } catch (e) {
      rethrow;
    }
  }
}

/// Provider do repositório de despacho técnico.
final dispatchRepositoryProvider = Provider<DispatchRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DispatchRepository(apiClient);
});
