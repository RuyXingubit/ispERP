import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'nas_model.dart';

final nasRepositoryProvider = Provider<NasRepository>((ref) {
  final client = ref.watch(apiClientProvider);
  return NasRepository(client);
});

class NasRepository {
  final ApiClient _apiClient;

  NasRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Retorna todos os concentradores NAS cadastrados no FreeRADIUS.
  Future<List<NasModel>> getAllNas() async {
    try {
      final response = await _dio.get('/nas');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => NasModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      rethrow;
    }
  }

  /// Cadastra um novo concentrador (ex: MikroTik CHR ou Huawei BNG).
  Future<NasModel> createNas(NasModel nas) async {
    try {
      final response = await _dio.post(
        '/nas',
        data: nas.toJson(),
      );
      return NasModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  /// Atualiza os parâmetros de conexão de um NAS existente.
  Future<NasModel> updateNas(String id, NasModel nas) async {
    try {
      final response = await _dio.put(
        '/nas/$id',
        data: nas.toJson(),
      );
      return NasModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  /// Remove um concentrador do FreeRADIUS.
  Future<void> deleteNas(String id) async {
    try {
      await _dio.delete('/nas/$id');
    } catch (e) {
      rethrow;
    }
  }
}
