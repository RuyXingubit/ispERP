import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'sales_models.dart';

/// Repositório de integração com as APIs de Venda, CEP e Viabilidade FTTH.
class SalesRepository {
  final ApiClient _apiClient;

  SalesRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Consulta CEP real via GeoCep API.
  Future<CepLookupModel?> lookupCep(String rawCep) async {
    final cleanCep = CpfUtils.clean(rawCep);
    if (cleanCep.length != 8) return null;

    try {
      final response = await _dio.get('/geocep/cep/$cleanCep');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return CepLookupModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Consulta viabilidade óptica FTTH real contra caixas CTO e portas livres.
  Future<FtthFeasibilityModel?> checkFeasibility(
    double latitude,
    double longitude, {
    double maxDistanceMeters = 200.0,
  }) async {
    try {
      final response = await _dio.post(
        '/ftth/feasibility',
        data: {
          'latitude': latitude,
          'longitude': longitude,
          'maxDistanceMeters': maxDistanceMeters,
        },
      );

      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return FtthFeasibilityModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Busca catálogo de planos comerciais ativos no backend.
  Future<List<CommercialPlan>> getActivePlans() async {
    try {
      final response = await _dio.get('/plans/active');
      if (response.statusCode == 200 && response.data is List) {
        final list = response.data as List;
        return list
            .map((item) => CommercialPlan.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Submete a nova venda no backend disparando o ZeroTouchOnboarding.
  Future<SaleResult?> submitSale(CreateSalePayload payload) async {
    try {
      final response = await _dio.post(
        '/sales',
        data: payload.toJson(),
      );

      if ((response.statusCode == 200 || response.statusCode == 201) &&
          response.data is Map<String, dynamic>) {
        return SaleResult.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      if (e is DioException && e.response?.data != null) {
        throw Exception(e.response?.data['message'] ?? 'Erro ao cadastrar venda');
      }
      rethrow;
    }
  }
}

/// Provider do repositório de vendas.
final salesRepositoryProvider = Provider<SalesRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return SalesRepository(apiClient);
});
