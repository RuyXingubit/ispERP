import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'payment_gateway_model.dart';

final paymentGatewayRepositoryProvider = Provider<PaymentGatewayRepository>((ref) {
  final client = ref.watch(apiClientProvider);
  return PaymentGatewayRepository(client);
});

class PaymentGatewayRepository {
  final ApiClient _apiClient;

  PaymentGatewayRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Retorna as configurações de todos os gateways de pagamento cadastrados.
  Future<List<PaymentGatewayModel>> getConfigs() async {
    try {
      final response = await _dio.get('/payment-gateways');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => PaymentGatewayModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      rethrow;
    }
  }

  /// Salva ou atualiza as credenciais de um gateway (ex: Xingubit Pay).
  Future<PaymentGatewayModel> saveConfig(PaymentGatewayModel config) async {
    try {
      final response = await _dio.post(
        '/payment-gateways',
        data: config.toJson(),
      );
      return PaymentGatewayModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }
}
