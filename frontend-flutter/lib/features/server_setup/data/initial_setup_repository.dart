import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

class InitialSetupData {
  final String companyName;
  final String companyCnpj;
  final String? companyPhone;
  final String? companyEmail;
  final String? companyAddress;
  final String? companyWebsite;
  final String adminName;
  final String adminEmail;
  final String adminPassword;

  InitialSetupData({
    required this.companyName,
    required this.companyCnpj,
    this.companyPhone,
    this.companyEmail,
    this.companyAddress,
    this.companyWebsite,
    required this.adminName,
    required this.adminEmail,
    required this.adminPassword,
  });

  Map<String, dynamic> toJson() {
    return {
      'companyName': companyName,
      'companyCnpj': companyCnpj,
      if (companyPhone != null && companyPhone!.isNotEmpty) 'companyPhone': companyPhone,
      if (companyEmail != null && companyEmail!.isNotEmpty) 'companyEmail': companyEmail,
      if (companyAddress != null && companyAddress!.isNotEmpty) 'companyAddress': companyAddress,
      if (companyWebsite != null && companyWebsite!.isNotEmpty) 'companyWebsite': companyWebsite,
      'adminName': adminName,
      'adminEmail': adminEmail,
      'adminPassword': adminPassword,
    };
  }
}

final initialSetupRepositoryProvider = Provider<InitialSetupRepository>((ref) {
  final client = ref.watch(apiClientProvider);
  return InitialSetupRepository(client);
});

class InitialSetupRepository {
  final ApiClient _apiClient;

  InitialSetupRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Verifica se a instância já concluiu o setup inicial ou se está zerada.
  Future<bool> isSetupCompleted() async {
    try {
      final response = await _dio.get('/initial-setup/status');
      if (response.statusCode == 200 && response.data is Map) {
        return response.data['isSetupCompleted'] == true;
      }
      return true; // Falha segura: assume completo se não conseguir responder
    } catch (_) {
      // Se der erro ou 404, assume completo para evitar bloqueios indevidos
      return true;
    }
  }

  /// Executa o setup de primeiro acesso cadastrando a empresa e o primeiro admin.
  Future<void> performSetup(InitialSetupData data) async {
    final response = await _dio.post(
      '/initial-setup',
      data: data.toJson(),
    );

    if (response.statusCode != 200 && response.statusCode != 201) {
      throw Exception(response.data?['message'] ?? 'Falha ao executar o setup inicial.');
    }
  }
}
