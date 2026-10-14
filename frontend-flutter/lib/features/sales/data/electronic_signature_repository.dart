import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'electronic_signature_models.dart';

/// Repositório para Assinatura Eletrônica Forense de Contratos (PIX Simbólico + MP 2.200-2).
class ElectronicSignatureRepository {
  final ApiClient _apiClient;

  ElectronicSignatureRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Cria uma nova sessão de assinatura eletrônica via PIX Simbólico para o contrato.
  Future<SignatureSessionModel> createSignatureSession({
    required String contractId,
    String? templateId,
    double symbolicAmount = 1.00,
  }) async {
    final res = await _dio.post(
      '/contracts/signatures',
      data: {
        'contractId': contractId,
        'templateId': ?templateId,
        'symbolicAmount': symbolicAmount,
      },
    );
    return SignatureSessionModel.fromJson(res.data as Map<String, dynamic>);
  }

  /// Lista o histórico de sessões de assinatura de um contrato.
  Future<List<SignatureSessionModel>> listContractSignatures(
    String contractId,
  ) async {
    try {
      final res = await _dio.get('/contracts/$contractId/signatures');
      if (res.statusCode == 200 && res.data is List) {
        return (res.data as List)
            .whereType<Map<String, dynamic>>()
            .map(SignatureSessionModel.fromJson)
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Consulta em tempo real o status de uma sessão de assinatura pelo token público.
  Future<SignatureSessionModel> getSignatureStatus(String token) async {
    final res = await _dio.get('/public/signatures/$token/status');
    return SignatureSessionModel.fromJson(res.data as Map<String, dynamic>);
  }

  /// Seleciona um método alternativo de assinatura (Fallback MP 2.200-2) caso o titular não utilize o PIX.
  Future<SignatureSessionModel> selectFallbackMethod({
    required String token,
    required FallbackMethodEnum fallbackMethod,
    String? justification,
  }) async {
    final cleanJustification = justification?.trim();
    final res = await _dio.post(
      '/public/signatures/$token/fallback',
      data: {
        'fallbackMethod': fallbackMethod.apiValue,
        if (cleanJustification != null && cleanJustification.isNotEmpty)
          'justification': cleanJustification,
      },
    );
    return SignatureSessionModel.fromJson(res.data as Map<String, dynamic>);
  }

  /// Localiza o contrato associado a uma venda (`saleId`) ou lista contratos recentes.
  Future<Map<String, dynamic>?> findContractForSale(String saleId) async {
    try {
      final res = await _dio.get('/contracts');
      if (res.statusCode == 200 && res.data is List) {
        final list = (res.data as List).whereType<Map<String, dynamic>>().toList();
        for (final c in list) {
          if (c['saleId']?.toString() == saleId) {
            return c;
          }
        }
        if (list.isNotEmpty) {
          return list.first;
        }
      }
      return null;
    } catch (_) {
      return null;
    }
  }
}

final electronicSignatureRepositoryProvider =
    Provider<ElectronicSignatureRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ElectronicSignatureRepository(apiClient);
});
