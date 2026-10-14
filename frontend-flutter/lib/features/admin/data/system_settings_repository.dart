import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Modelo de Empresa / Filial (`/companies`).
class CompanyModel {
  final String id;
  final String name;
  final String? document;
  final String? address;
  final String? phone;
  final String? email;
  final String? website;
  final bool active;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const CompanyModel({
    required this.id,
    required this.name,
    this.document,
    this.address,
    this.phone,
    this.email,
    this.website,
    this.active = true,
    this.createdAt,
    this.updatedAt,
  });

  factory CompanyModel.fromJson(Map<String, dynamic> json) {
    return CompanyModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      document: json['document']?.toString(),
      address: json['address']?.toString(),
      phone: json['phone']?.toString(),
      email: json['email']?.toString(),
      website: json['website']?.toString(),
      active: json['active'] != false,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }
}

/// Payload para criação/atualização de Empresa (`/companies`).
class CompanySavePayload {
  final String name;
  final String? document;
  final String? address;
  final String? phone;
  final String? email;
  final String? website;
  final bool active;

  const CompanySavePayload({
    required this.name,
    this.document,
    this.address,
    this.phone,
    this.email,
    this.website,
    this.active = true,
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name.trim(),
      if (document != null && document!.trim().isNotEmpty)
        'document': document!.trim(),
      if (address != null && address!.trim().isNotEmpty)
        'address': address!.trim(),
      if (phone != null && phone!.trim().isNotEmpty) 'phone': phone!.trim(),
      if (email != null && email!.trim().isNotEmpty) 'email': email!.trim(),
      if (website != null && website!.trim().isNotEmpty)
        'website': website!.trim(),
      'active': active,
    };
  }
}

/// Modelo de Configurações Institucionais e do Site Vitrine (`/site-settings`).
class SiteSettingsModel {
  final String id;
  final String siteTitle;
  final String? siteDescription;
  final String? primaryColor;
  final String? secondaryColor;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const SiteSettingsModel({
    required this.id,
    required this.siteTitle,
    this.siteDescription,
    this.primaryColor,
    this.secondaryColor,
    this.createdAt,
    this.updatedAt,
  });

  factory SiteSettingsModel.fromJson(Map<String, dynamic> json) {
    return SiteSettingsModel(
      id: json['id']?.toString() ?? '',
      siteTitle: json['siteTitle']?.toString() ?? '',
      siteDescription: json['siteDescription']?.toString(),
      primaryColor: json['primaryColor']?.toString(),
      secondaryColor: json['secondaryColor']?.toString(),
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }
}

/// Payload para atualização de `/site-settings`.
class SiteSettingsSavePayload {
  final String siteTitle;
  final String? siteDescription;
  final String? primaryColor;
  final String? secondaryColor;

  const SiteSettingsSavePayload({
    required this.siteTitle,
    this.siteDescription,
    this.primaryColor,
    this.secondaryColor,
  });

  Map<String, dynamic> toJson() {
    return {
      'siteTitle': siteTitle.trim(),
      if (siteDescription != null && siteDescription!.trim().isNotEmpty)
        'siteDescription': siteDescription!.trim(),
      if (primaryColor != null && primaryColor!.trim().isNotEmpty)
        'primaryColor': primaryColor!.trim(),
      if (secondaryColor != null && secondaryColor!.trim().isNotEmpty)
        'secondaryColor': secondaryColor!.trim(),
    };
  }
}

/// Modelo de Configuração de Armazenamento S3 / Local (`/storage/config`).
class StorageConfigModel {
  final String? id;
  final String? companyId;
  final String storageType;
  final String provider;
  final String? endpointUrl;
  final String bucketName;
  final String region;
  final String? accessKey;
  final String? maskedSecretKey;
  final bool pathStyleAccess;
  final bool isActive;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const StorageConfigModel({
    this.id,
    this.companyId,
    required this.storageType,
    required this.provider,
    this.endpointUrl,
    required this.bucketName,
    required this.region,
    this.accessKey,
    this.maskedSecretKey,
    this.pathStyleAccess = true,
    this.isActive = true,
    this.createdAt,
    this.updatedAt,
  });

  factory StorageConfigModel.fromJson(Map<String, dynamic> json) {
    return StorageConfigModel(
      id: json['id']?.toString(),
      companyId: json['companyId']?.toString(),
      storageType: json['storageType']?.toString() ?? 'S3',
      provider: json['provider']?.toString() ?? 'SEAWEEDFS_LOCAL',
      endpointUrl: json['endpointUrl']?.toString(),
      bucketName: json['bucketName']?.toString() ?? 'isperp-files',
      region: json['region']?.toString() ?? 'us-east-1',
      accessKey: json['accessKey']?.toString(),
      maskedSecretKey: json['maskedSecretKey']?.toString(),
      pathStyleAccess: json['pathStyleAccess'] != false,
      isActive: json['isActive'] != false,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }
}

/// Payload para salvar ou testar configuração de Storage (`/storage/config`).
class StorageConfigSavePayload {
  final String? companyId;
  final String storageType;
  final String provider;
  final String? endpointUrl;
  final String bucketName;
  final String region;
  final String? accessKey;
  final String? secretKey;
  final bool pathStyleAccess;
  final bool isActive;

  const StorageConfigSavePayload({
    this.companyId,
    required this.storageType,
    required this.provider,
    this.endpointUrl,
    required this.bucketName,
    required this.region,
    this.accessKey,
    this.secretKey,
    this.pathStyleAccess = true,
    this.isActive = true,
  });

  Map<String, dynamic> toJson() {
    return {
      if (companyId != null && companyId!.isNotEmpty) 'companyId': companyId,
      'storageType': storageType,
      'provider': provider,
      if (endpointUrl != null && endpointUrl!.trim().isNotEmpty)
        'endpointUrl': endpointUrl!.trim(),
      'bucketName': bucketName.trim(),
      'region': region.trim(),
      if (accessKey != null && accessKey!.trim().isNotEmpty)
        'accessKey': accessKey!.trim(),
      if (secretKey != null && secretKey!.trim().isNotEmpty)
        'secretKey': secretKey!.trim(),
      'pathStyleAccess': pathStyleAccess,
      'isActive': isActive,
    };
  }
}

/// Resposta do teste de conectividade S3 (`POST /storage/config/test`).
class StorageConnectionTestResult {
  final bool success;
  final String message;
  final String? details;
  final int latencyMs;

  const StorageConnectionTestResult({
    required this.success,
    required this.message,
    this.details,
    required this.latencyMs,
  });

  factory StorageConnectionTestResult.fromJson(Map<String, dynamic> json) {
    return StorageConnectionTestResult(
      success: json['success'] == true,
      message: json['message']?.toString() ?? '',
      details: json['details']?.toString(),
      latencyMs: (json['latencyMs'] as num?)?.toInt() ?? 0,
    );
  }
}

/// Modelo de Configuração de Notificação WhatsApp/SMS (`/notifications/configs`).
class NotificationConfigModel {
  final String id;
  final String? companyId;
  final String providerType;
  final String name;
  final String? apiUrl;
  final String? apiToken;
  final String? accountSid;
  final String? authToken;
  final String? fromPhoneNumber;
  final bool active;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const NotificationConfigModel({
    required this.id,
    this.companyId,
    required this.providerType,
    required this.name,
    this.apiUrl,
    this.apiToken,
    this.accountSid,
    this.authToken,
    this.fromPhoneNumber,
    this.active = true,
    this.createdAt,
    this.updatedAt,
  });

  factory NotificationConfigModel.fromJson(Map<String, dynamic> json) {
    return NotificationConfigModel(
      id: json['id']?.toString() ?? '',
      companyId: json['companyId']?.toString(),
      providerType: json['providerType']?.toString() ?? 'EVOLUTION_API',
      name: json['name']?.toString() ?? '',
      apiUrl: json['apiUrl']?.toString(),
      apiToken: json['apiToken']?.toString(),
      accountSid: json['accountSid']?.toString(),
      authToken: json['authToken']?.toString(),
      fromPhoneNumber: json['fromPhoneNumber']?.toString(),
      active: json['active'] != false,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }
}

/// Payload para salvar configuração de Notificação (`/notifications/configs`).
class NotificationConfigSavePayload {
  final String? companyId;
  final String providerType;
  final String name;
  final String? apiUrl;
  final String? apiToken;
  final String? accountSid;
  final String? authToken;
  final String? fromPhoneNumber;
  final bool active;

  const NotificationConfigSavePayload({
    this.companyId,
    required this.providerType,
    required this.name,
    this.apiUrl,
    this.apiToken,
    this.accountSid,
    this.authToken,
    this.fromPhoneNumber,
    this.active = true,
  });

  Map<String, dynamic> toJson() {
    return {
      if (companyId != null && companyId!.isNotEmpty) 'companyId': companyId,
      'providerType': providerType,
      'name': name.trim(),
      if (apiUrl != null && apiUrl!.trim().isNotEmpty)
        'apiUrl': apiUrl!.trim(),
      if (apiToken != null && apiToken!.trim().isNotEmpty)
        'apiToken': apiToken!.trim(),
      if (accountSid != null && accountSid!.trim().isNotEmpty)
        'accountSid': accountSid!.trim(),
      if (authToken != null && authToken!.trim().isNotEmpty)
        'authToken': authToken!.trim(),
      if (fromPhoneNumber != null && fromPhoneNumber!.trim().isNotEmpty)
        'fromPhoneNumber': fromPhoneNumber!.trim(),
      'active': active,
    };
  }
}

/// Repositório Unificado de Configurações do Sistema (`/companies`, `/site-settings`, `/storage/config`, `/notifications/configs`).
class SystemSettingsRepository {
  final ApiClient _apiClient;

  SystemSettingsRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  // ===========================================================================
  // EMPRESAS & FILIAIS (/companies)
  // ===========================================================================

  Future<List<CompanyModel>> getAllCompanies() async {
    final response = await _dio.get('/companies');
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(CompanyModel.fromJson)
          .toList();
    }
    return [];
  }

  Future<CompanyModel?> getPrimaryCompany() async {
    try {
      final response = await _dio.get('/companies/primary');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return CompanyModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<CompanyModel> createCompany(CompanySavePayload payload) async {
    final response = await _dio.post('/companies', data: payload.toJson());
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return CompanyModel.fromJson(response.data as Map<String, dynamic>);
    }
    throw Exception('Falha ao cadastrar empresa/filial');
  }

  Future<CompanyModel> updateCompany(
    String id,
    CompanySavePayload payload,
  ) async {
    final response = await _dio.put('/companies/$id', data: payload.toJson());
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return CompanyModel.fromJson(response.data as Map<String, dynamic>);
    }
    throw Exception('Falha ao atualizar empresa/filial');
  }

  Future<void> deleteCompany(String id) async {
    await _dio.delete('/companies/$id');
  }

  // ===========================================================================
  // SITE VITRINE & IDENTIDADE (/site-settings)
  // ===========================================================================

  Future<SiteSettingsModel?> getSiteSettings() async {
    try {
      final response = await _dio.get('/site-settings');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return SiteSettingsModel.fromJson(
          response.data as Map<String, dynamic>,
        );
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<SiteSettingsModel> updateSiteSettings(
    SiteSettingsSavePayload payload,
  ) async {
    final response = await _dio.put('/site-settings', data: payload.toJson());
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return SiteSettingsModel.fromJson(response.data as Map<String, dynamic>);
    }
    throw Exception('Falha ao atualizar configurações do site vitrine');
  }

  // ===========================================================================
  // STORAGE S3 / SEAWEEDFS (/storage/config)
  // ===========================================================================

  Future<StorageConfigModel?> getStorageConfig() async {
    try {
      final response = await _dio.get('/storage/config');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return StorageConfigModel.fromJson(
          response.data as Map<String, dynamic>,
        );
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<StorageConfigModel> saveStorageConfig(
    StorageConfigSavePayload payload,
  ) async {
    final response = await _dio.put('/storage/config', data: payload.toJson());
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return StorageConfigModel.fromJson(response.data as Map<String, dynamic>);
    }
    throw Exception('Falha ao salvar configuração de armazenamento');
  }

  Future<StorageConnectionTestResult> testStorageConnection(
    StorageConfigSavePayload payload,
  ) async {
    final response = await _dio.post(
      '/storage/config/test',
      data: payload.toJson(),
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return StorageConnectionTestResult.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao testar conectividade do armazenamento');
  }

  // ===========================================================================
  // NOTIFICAÇÕES WHATSAPP / SMS (/notifications/configs)
  // ===========================================================================

  Future<List<NotificationConfigModel>> getNotificationConfigs() async {
    final response = await _dio.get('/notifications/configs');
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(NotificationConfigModel.fromJson)
          .toList();
    }
    return [];
  }

  Future<NotificationConfigModel> createNotificationConfig(
    NotificationConfigSavePayload payload,
  ) async {
    final response = await _dio.post(
      '/notifications/configs',
      data: payload.toJson(),
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return NotificationConfigModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao criar canal de notificação');
  }

  Future<NotificationConfigModel> updateNotificationConfig(
    String id,
    NotificationConfigSavePayload payload,
  ) async {
    final response = await _dio.put(
      '/notifications/configs/$id',
      data: payload.toJson(),
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return NotificationConfigModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao atualizar canal de notificação');
  }
}

final systemSettingsRepositoryProvider = Provider<SystemSettingsRepository>((
  ref,
) {
  final apiClient = ref.watch(apiClientProvider);
  return SystemSettingsRepository(apiClient);
});

final companiesListProvider = FutureProvider.autoDispose<List<CompanyModel>>((
  ref,
) async {
  return ref.watch(systemSettingsRepositoryProvider).getAllCompanies();
});

final siteSettingsProvider = FutureProvider.autoDispose<SiteSettingsModel?>((
  ref,
) async {
  return ref.watch(systemSettingsRepositoryProvider).getSiteSettings();
});

final storageConfigProvider = FutureProvider.autoDispose<StorageConfigModel?>((
  ref,
) async {
  return ref.watch(systemSettingsRepositoryProvider).getStorageConfig();
});

final notificationConfigsListProvider =
    FutureProvider.autoDispose<List<NotificationConfigModel>>((ref) async {
      return ref
          .watch(systemSettingsRepositoryProvider)
          .getNotificationConfigs();
    });
