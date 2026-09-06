import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'audit_log_model.dart';

final auditLogRepositoryProvider = Provider<AuditLogRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AuditLogRepository(apiClient);
});

class AuditLogRepository {
  final ApiClient _apiClient;

  AuditLogRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Busca registros da trilha de auditoria forense com paginação e filtros dinâmicos.
  Future<AuditLogPage> getAuditLogs({
    String? userId,
    String? entityName,
    String? action,
    DateTime? startDate,
    DateTime? endDate,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final queryParameters = <String, dynamic>{
        'page': page,
        'size': size,
        if (userId != null && userId.isNotEmpty) 'userId': userId,
        if (entityName != null && entityName.isNotEmpty && entityName != 'ALL')
          'entityName': entityName,
        if (action != null && action.isNotEmpty) 'action': action,
        if (startDate != null) 'startDate': startDate.toIso8601String(),
        if (endDate != null) 'endDate': endDate.toIso8601String(),
      };

      final response = await _dio.get(
        '/api/admin/audit-logs',
        queryParameters: queryParameters,
      );

      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return AuditLogPage.fromJson(response.data as Map<String, dynamic>);
      }
      return AuditLogPage.empty();
    } catch (e) {
      rethrow;
    }
  }
}
