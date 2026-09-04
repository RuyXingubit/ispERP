import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/app_providers.dart';
import 'dashboard_bi_model.dart';

/// Provider que consome os indicadores reais consolidados do backend (GET /bi/metrics).
final dashboardBiProvider = FutureProvider.autoDispose<DashboardBiModel>((ref) async {
  final apiClient = ref.watch(apiClientProvider);

  final response = await apiClient.dio.get('/bi/metrics');

  if (response.data is Map<String, dynamic>) {
    return DashboardBiModel.fromJson(response.data as Map<String, dynamic>);
  }

  throw Exception('Formato de resposta inesperado do servidor');
});
