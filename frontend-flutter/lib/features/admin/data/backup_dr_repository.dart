import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Visão geral consolidada do sistema de Disaster Recovery (`/financial/backup/overview`).
class BackupOverviewModel {
  final String securityMode;
  final bool hasActivePolicy;
  final String cronExpression;
  final int retentionDays;
  final int activeDestinationsCount;
  final int totalBackupsCount;
  final String? lastBackupStatus;
  final DateTime? lastBackupAt;
  final int? lastBackupSizeBytes;
  final double? lastBackupCompressionRatio;
  final String? lastBackupSha256;
  final String? lastBackupFileName;
  final bool isDryRunVerified;
  final DateTime? lastDryRunVerifiedAt;
  final bool rescueKitDownloaded;

  const BackupOverviewModel({
    required this.securityMode,
    required this.hasActivePolicy,
    required this.cronExpression,
    required this.retentionDays,
    required this.activeDestinationsCount,
    required this.totalBackupsCount,
    this.lastBackupStatus,
    this.lastBackupAt,
    this.lastBackupSizeBytes,
    this.lastBackupCompressionRatio,
    this.lastBackupSha256,
    this.lastBackupFileName,
    required this.isDryRunVerified,
    this.lastDryRunVerifiedAt,
    required this.rescueKitDownloaded,
  });

  factory BackupOverviewModel.fromJson(Map<String, dynamic> json) {
    return BackupOverviewModel(
      securityMode: json['securityMode']?.toString() ?? 'MANAGED_RESCUE',
      hasActivePolicy: json['hasActivePolicy'] == true,
      cronExpression: json['cronExpression']?.toString() ?? '0 0 3 * * *',
      retentionDays: (json['retentionDays'] as num?)?.toInt() ?? 30,
      activeDestinationsCount:
          (json['activeDestinationsCount'] as num?)?.toInt() ?? 0,
      totalBackupsCount: (json['totalBackupsCount'] as num?)?.toInt() ?? 0,
      lastBackupStatus: json['lastBackupStatus']?.toString(),
      lastBackupAt: json['lastBackupAt'] != null
          ? DateTime.tryParse(json['lastBackupAt'].toString())
          : null,
      lastBackupSizeBytes: (json['lastBackupSizeBytes'] as num?)?.toInt(),
      lastBackupCompressionRatio:
          (json['lastBackupCompressionRatio'] as num?)?.toDouble(),
      lastBackupSha256: json['lastBackupSha256']?.toString(),
      lastBackupFileName: json['lastBackupFileName']?.toString(),
      isDryRunVerified: json['isDryRunVerified'] == true,
      lastDryRunVerifiedAt: json['lastDryRunVerifiedAt'] != null
          ? DateTime.tryParse(json['lastDryRunVerifiedAt'].toString())
          : null,
      rescueKitDownloaded: json['rescueKitDownloaded'] == true,
    );
  }
}

/// Payload para configurar a política de Backup e Criptografia AES-256-GCM (`/financial/backup/policies`).
class BackupPolicySavePayload {
  final String securityMode;
  final String? customMasterKey;
  final String cronExpression;
  final int retentionDays;
  final String compressionAlgorithm;
  final bool autoDryRunEnabled;

  const BackupPolicySavePayload({
    required this.securityMode,
    this.customMasterKey,
    required this.cronExpression,
    required this.retentionDays,
    required this.compressionAlgorithm,
    this.autoDryRunEnabled = true,
  });

  Map<String, dynamic> toJson() {
    return {
      'securityMode': securityMode,
      if (customMasterKey != null && customMasterKey!.trim().isNotEmpty)
        'customMasterKey': customMasterKey!.trim(),
      'cronExpression': cronExpression.trim(),
      'retentionDays': retentionDays,
      'compressionAlgorithm': compressionAlgorithm,
      'autoDryRunEnabled': autoDryRunEnabled,
    };
  }
}

/// Resposta da configuração de política de Backup (`POST /financial/backup/policies`).
class BackupPolicyResultModel {
  final String id;
  final String securityMode;
  final String masterKeyHash;
  final String? generatedPlainMasterKey;
  final String cronExpression;
  final int retentionDays;
  final String compressionAlgorithm;
  final bool autoDryRunEnabled;
  final bool isActive;

  const BackupPolicyResultModel({
    required this.id,
    required this.securityMode,
    required this.masterKeyHash,
    this.generatedPlainMasterKey,
    required this.cronExpression,
    required this.retentionDays,
    required this.compressionAlgorithm,
    required this.autoDryRunEnabled,
    required this.isActive,
  });

  factory BackupPolicyResultModel.fromJson(Map<String, dynamic> json) {
    return BackupPolicyResultModel(
      id: json['id']?.toString() ?? '',
      securityMode: json['securityMode']?.toString() ?? 'MANAGED_RESCUE',
      masterKeyHash: json['masterKeyHash']?.toString() ?? '',
      generatedPlainMasterKey: json['generatedPlainMasterKey']?.toString(),
      cronExpression: json['cronExpression']?.toString() ?? '0 0 3 * * *',
      retentionDays: (json['retentionDays'] as num?)?.toInt() ?? 30,
      compressionAlgorithm: json['compressionAlgorithm']?.toString() ?? 'ZSTD',
      autoDryRunEnabled: json['autoDryRunEnabled'] != false,
      isActive: json['isActive'] != false,
    );
  }
}

/// Modelo de Destino de Armazenamento de Backup (`/financial/backup/destinations`).
class BackupDestinationModel {
  final String id;
  final String name;
  final String storageType;
  final String? endpointUrl;
  final String? bucketName;
  final String? region;
  final String? accessKey;
  final String? pathPrefix;
  final bool isActive;
  final bool isPrimary;
  final DateTime? lastTestedAt;
  final String? lastTestStatus;
  final String? lastTestError;
  final DateTime? createdAt;

  const BackupDestinationModel({
    required this.id,
    required this.name,
    required this.storageType,
    this.endpointUrl,
    this.bucketName,
    this.region,
    this.accessKey,
    this.pathPrefix,
    this.isActive = true,
    this.isPrimary = false,
    this.lastTestedAt,
    this.lastTestStatus,
    this.lastTestError,
    this.createdAt,
  });

  factory BackupDestinationModel.fromJson(Map<String, dynamic> json) {
    return BackupDestinationModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      storageType: json['storageType']?.toString() ?? 'S3_COMPATIBLE',
      endpointUrl: json['endpointUrl']?.toString(),
      bucketName: json['bucketName']?.toString(),
      region: json['region']?.toString(),
      accessKey: json['accessKey']?.toString(),
      pathPrefix: json['pathPrefix']?.toString(),
      isActive: json['isActive'] != false,
      isPrimary: json['isPrimary'] == true,
      lastTestedAt: json['lastTestedAt'] != null
          ? DateTime.tryParse(json['lastTestedAt'].toString())
          : null,
      lastTestStatus: json['lastTestStatus']?.toString(),
      lastTestError: json['lastTestError']?.toString(),
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
    );
  }
}

/// Payload para criar Destino de Backup (`POST /financial/backup/destinations`).
class BackupDestinationSavePayload {
  final String name;
  final String storageType;
  final String? endpointUrl;
  final String? bucketName;
  final String? region;
  final String? accessKey;
  final String? secretKey;
  final String? pathPrefix;
  final bool isPrimary;

  const BackupDestinationSavePayload({
    required this.name,
    required this.storageType,
    this.endpointUrl,
    this.bucketName,
    this.region,
    this.accessKey,
    this.secretKey,
    this.pathPrefix,
    this.isPrimary = false,
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name.trim(),
      'storageType': storageType,
      if (endpointUrl != null && endpointUrl!.trim().isNotEmpty)
        'endpointUrl': endpointUrl!.trim(),
      if (bucketName != null && bucketName!.trim().isNotEmpty)
        'bucketName': bucketName!.trim(),
      if (region != null && region!.trim().isNotEmpty) 'region': region!.trim(),
      if (accessKey != null && accessKey!.trim().isNotEmpty)
        'accessKey': accessKey!.trim(),
      if (secretKey != null && secretKey!.trim().isNotEmpty)
        'secretKey': secretKey!.trim(),
      if (pathPrefix != null && pathPrefix!.trim().isNotEmpty)
        'pathPrefix': pathPrefix!.trim(),
      'isPrimary': isPrimary,
    };
  }
}

/// Resultado do teste de conectividade do Destino de Backup (`POST /financial/backup/destinations/{id}/test`).
class BackupDestinationTestResult {
  final bool success;
  final String message;
  final String? detailedError;
  final int latencyMs;

  const BackupDestinationTestResult({
    required this.success,
    required this.message,
    this.detailedError,
    required this.latencyMs,
  });

  factory BackupDestinationTestResult.fromJson(Map<String, dynamic> json) {
    return BackupDestinationTestResult(
      success: json['success'] == true,
      message: json['message']?.toString() ?? '',
      detailedError: json['detailedError']?.toString(),
      latencyMs: (json['latencyMs'] as num?)?.toInt() ?? 0,
    );
  }
}

/// Registro de execução no Histórico de Backups (`/financial/backup/history`).
class BackupExecutionLogModel {
  final String id;
  final String? policyId;
  final String? destinationId;
  final String? destinationName;
  final String triggerType;
  final String status;
  final String fileName;
  final int? originalSizeBytes;
  final int? compressedSizeBytes;
  final double? compressionRatio;
  final String? sha256Hash;
  final int? durationSeconds;
  final String? errorMessage;
  final bool isDryRunVerified;
  final DateTime? dryRunVerifiedAt;
  final DateTime? startedAt;
  final DateTime? completedAt;

  const BackupExecutionLogModel({
    required this.id,
    this.policyId,
    this.destinationId,
    this.destinationName,
    required this.triggerType,
    required this.status,
    required this.fileName,
    this.originalSizeBytes,
    this.compressedSizeBytes,
    this.compressionRatio,
    this.sha256Hash,
    this.durationSeconds,
    this.errorMessage,
    this.isDryRunVerified = false,
    this.dryRunVerifiedAt,
    this.startedAt,
    this.completedAt,
  });

  factory BackupExecutionLogModel.fromJson(Map<String, dynamic> json) {
    return BackupExecutionLogModel(
      id: json['id']?.toString() ?? '',
      policyId: json['policyId']?.toString(),
      destinationId: json['destinationId']?.toString(),
      destinationName: json['destinationName']?.toString(),
      triggerType: json['triggerType']?.toString() ?? 'MANUAL',
      status: json['status']?.toString() ?? 'RUNNING',
      fileName: json['fileName']?.toString() ?? '',
      originalSizeBytes: (json['originalSizeBytes'] as num?)?.toInt(),
      compressedSizeBytes: (json['compressedSizeBytes'] as num?)?.toInt(),
      compressionRatio: (json['compressionRatio'] as num?)?.toDouble(),
      sha256Hash: json['sha256Hash']?.toString(),
      durationSeconds: (json['durationSeconds'] as num?)?.toInt(),
      errorMessage: json['errorMessage']?.toString(),
      isDryRunVerified: json['isDryRunVerified'] == true,
      dryRunVerifiedAt: json['dryRunVerifiedAt'] != null
          ? DateTime.tryParse(json['dryRunVerifiedAt'].toString())
          : null,
      startedAt: json['startedAt'] != null
          ? DateTime.tryParse(json['startedAt'].toString())
          : null,
      completedAt: json['completedAt'] != null
          ? DateTime.tryParse(json['completedAt'].toString())
          : null,
    );
  }
}

/// Repositório de Governança de Backup & Disaster Recovery (`/financial/backup/*`).
class BackupDrRepository {
  final ApiClient _apiClient;

  BackupDrRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  Future<BackupOverviewModel> getOverview() async {
    final response = await _dio.get('/financial/backup/overview');
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return BackupOverviewModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao obter visão geral de Disaster Recovery');
  }

  Future<BackupPolicyResultModel> configurePolicy(
    BackupPolicySavePayload payload,
  ) async {
    final response = await _dio.post(
      '/financial/backup/policies',
      data: payload.toJson(),
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return BackupPolicyResultModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao configurar política de backup');
  }

  Future<List<BackupDestinationModel>> getDestinations() async {
    final response = await _dio.get('/financial/backup/destinations');
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(BackupDestinationModel.fromJson)
          .toList();
    }
    return [];
  }

  Future<BackupDestinationModel> createDestination(
    BackupDestinationSavePayload payload,
  ) async {
    final response = await _dio.post(
      '/financial/backup/destinations',
      data: payload.toJson(),
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return BackupDestinationModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao cadastrar destino de backup');
  }

  Future<void> deleteDestination(String id) async {
    await _dio.delete('/financial/backup/destinations/$id');
  }

  Future<BackupDestinationTestResult> testDestination(String id) async {
    final response = await _dio.post('/financial/backup/destinations/$id/test');
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return BackupDestinationTestResult.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao testar conectividade do destino de backup');
  }

  Future<BackupExecutionLogModel> executeBackup({String? masterKey}) async {
    final response = await _dio.post(
      '/financial/backup/execute',
      data: {
        if (masterKey != null && masterKey.trim().isNotEmpty)
          'masterKey': masterKey.trim(),
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return BackupExecutionLogModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao disparar rotina de backup');
  }

  Future<List<BackupExecutionLogModel>> getHistory() async {
    final response = await _dio.get('/financial/backup/history');
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(BackupExecutionLogModel.fromJson)
          .toList();
    }
    return [];
  }

  Future<String> fetchEmergencyKitMarkdown() async {
    final response = await _dio.get(
      '/financial/backup/emergency-kit',
      options: Options(responseType: ResponseType.plain),
    );
    if (response.statusCode == 200 && response.data != null) {
      return response.data.toString();
    }
    throw Exception('Falha ao gerar o Kit de Resgate de Emergência');
  }
}

final backupDrRepositoryProvider = Provider<BackupDrRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return BackupDrRepository(apiClient);
});

final backupOverviewProvider = FutureProvider.autoDispose<BackupOverviewModel>((
  ref,
) async {
  return ref.watch(backupDrRepositoryProvider).getOverview();
});

final backupDestinationsProvider =
    FutureProvider.autoDispose<List<BackupDestinationModel>>((ref) async {
      return ref.watch(backupDrRepositoryProvider).getDestinations();
    });

final backupHistoryProvider =
    FutureProvider.autoDispose<List<BackupExecutionLogModel>>((ref) async {
      return ref.watch(backupDrRepositoryProvider).getHistory();
    });
