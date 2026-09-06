import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'user_model.dart';

final usersRepositoryProvider = Provider<UsersRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return UsersRepository(apiClient);
});

class UsersRepository {
  final ApiClient _apiClient;

  UsersRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Lista todos os colaboradores e usuários cadastrados no sistema.
  Future<List<UserModel>> listUsers() async {
    try {
      final response = await _dio.get('/users');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => UserModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      rethrow;
    }
  }

  /// Cadastra um novo colaborador no sistema.
  Future<UserModel> createUser({
    required String name,
    required String email,
    required String password,
    required String role,
    String? cpf,
    bool active = true,
  }) async {
    try {
      final response = await _dio.post(
        '/users',
        data: {
          'name': name,
          'email': email,
          'password': password,
          'role': role,
          if (cpf != null && cpf.isNotEmpty) 'cpf': cpf,
          'active': active,
        },
      );
      return UserModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  /// Atualiza dados de um colaborador.
  Future<UserModel> updateUser({
    required String id,
    String? name,
    String? email,
    String? role,
    String? cpf,
    bool? active,
    String? password,
  }) async {
    try {
      final response = await _dio.put(
        '/users/$id',
        data: {
          if (name != null) 'name': name,
          if (email != null) 'email': email,
          if (role != null) 'role': role,
          if (cpf != null) 'cpf': cpf,
          if (active != null) 'active': active,
          if (password != null && password.isNotEmpty) 'password': password,
        },
      );
      return UserModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  /// Suspende ou reativa um usuário diretamente.
  Future<UserModel> updateUserStatus({
    required String id,
    required bool active,
  }) async {
    try {
      final response = await _dio.patch(
        '/users/$id/status',
        queryParameters: {'active': active},
      );
      return UserModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  /// Altera o papel / role de acesso do colaborador.
  Future<UserModel> updateUserRole({
    required String id,
    required String role,
  }) async {
    try {
      final response = await _dio.patch(
        '/users/$id/role',
        queryParameters: {'role': role},
      );
      return UserModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  /// Redefine a senha de acesso de um usuário.
  Future<UserModel> resetPassword({
    required String id,
    required String newPassword,
  }) async {
    try {
      final response = await _dio.post(
        '/users/$id/reset-password',
        data: {'newPassword': newPassword},
      );
      return UserModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  /// Remove um usuário do sistema.
  Future<void> deleteUser(String id) async {
    try {
      await _dio.delete('/users/$id');
    } catch (e) {
      rethrow;
    }
  }
}
