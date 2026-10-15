import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Item individual da composição financeira do plano (SCM, SVA, Locação de Equipamento).
class PlanCompositionItemModel {
  final String? id;
  final String name;
  final String category;
  final double amount;
  final String? fiscalCode;

  const PlanCompositionItemModel({
    this.id,
    required this.name,
    required this.category,
    required this.amount,
    this.fiscalCode,
  });

  factory PlanCompositionItemModel.fromJson(Map<String, dynamic> json) {
    return PlanCompositionItemModel(
      id: json['id']?.toString(),
      name: json['name']?.toString() ?? '',
      category: json['category']?.toString() ?? 'SCM',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      fiscalCode: json['fiscalCode']?.toString(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null && id!.isNotEmpty) 'id': id,
      'name': name.trim(),
      'category': category,
      'amount': amount,
      if (fiscalCode != null && fiscalCode!.trim().isNotEmpty)
        'fiscalCode': fiscalCode!.trim(),
    };
  }

  bool get isScm => category.toUpperCase() == 'SCM';

  String get categoryLabel {
    switch (category.toUpperCase()) {
      case 'SCM':
        return 'SCM (Conexão — Base NFCom Mod. 62)';
      case 'EQUIPMENT_RENTAL':
        return 'Locação / Comodato de Equipamento';
      case 'SVA_DIGITAL_CONTENT':
        return 'SVA — Conteúdo Digital / E-books';
      case 'SVA_STREAMING_APP':
        return 'SVA — Aplicativo / Streaming';
      case 'SVA_OTHER':
        return 'SVA — Outros Serviços de Valor Adicionado';
      default:
        return category;
    }
  }
}

/// Modelo completo de Plano de Acesso à Internet (GET/POST/PUT /plans).
class AdminPlanModel {
  final String id;
  final String name;
  final int downloadSpeed;
  final int uploadSpeed;
  final double price;
  final double scmAmount;
  final double svaAmount;
  final List<PlanCompositionItemModel> compositionItems;
  final String? description;
  final String? svaIncluded;
  final int suspensionDays;
  final bool alwaysIssueNfcom;
  final bool active;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const AdminPlanModel({
    required this.id,
    required this.name,
    required this.downloadSpeed,
    required this.uploadSpeed,
    required this.price,
    this.scmAmount = 0.0,
    this.svaAmount = 0.0,
    this.compositionItems = const [],
    this.description,
    this.svaIncluded,
    this.suspensionDays = 5,
    this.alwaysIssueNfcom = false,
    this.active = true,
    this.createdAt,
    this.updatedAt,
  });

  factory AdminPlanModel.fromJson(Map<String, dynamic> json) {
    final priceVal = (json['price'] as num?)?.toDouble() ?? 0.0;
    final itemsRaw = (json['compositionItems'] as List<dynamic>? ?? [])
        .map(
          (e) => PlanCompositionItemModel.fromJson(
            Map<String, dynamic>.from(e as Map),
          ),
        )
        .toList();
    final scmVal =
        (json['scmAmount'] as num?)?.toDouble() ??
        (itemsRaw.isEmpty
            ? priceVal
            : itemsRaw
                  .where((i) => i.isScm)
                  .fold<double>(0.0, (sum, i) => sum + i.amount));
    final svaVal =
        (json['svaAmount'] as num?)?.toDouble() ??
        (itemsRaw.isEmpty
            ? 0.0
            : itemsRaw
                  .where((i) => !i.isScm)
                  .fold<double>(0.0, (sum, i) => sum + i.amount));

    return AdminPlanModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      downloadSpeed: (json['downloadSpeed'] as num?)?.toInt() ?? 0,
      uploadSpeed: (json['uploadSpeed'] as num?)?.toInt() ?? 0,
      price: priceVal,
      scmAmount: scmVal,
      svaAmount: svaVal,
      compositionItems: itemsRaw,
      description: json['description']?.toString(),
      svaIncluded: json['svaIncluded']?.toString(),
      suspensionDays: (json['suspensionDays'] as num?)?.toInt() ?? 5,
      alwaysIssueNfcom: json['alwaysIssueNfcom'] == true,
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

/// Payload para criação e atualização de Plano (POST /plans e PUT /plans/{id}).
class PlanSavePayload {
  final String name;
  final int downloadSpeed;
  final int uploadSpeed;
  final double price;
  final String? description;
  final String? svaIncluded;
  final int suspensionDays;
  final bool alwaysIssueNfcom;
  final bool active;
  final List<PlanCompositionItemModel> compositionItems;

  const PlanSavePayload({
    required this.name,
    required this.downloadSpeed,
    required this.uploadSpeed,
    required this.price,
    this.description,
    this.svaIncluded,
    this.suspensionDays = 5,
    this.alwaysIssueNfcom = false,
    this.active = true,
    this.compositionItems = const [],
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name.trim(),
      'downloadSpeed': downloadSpeed,
      'uploadSpeed': uploadSpeed,
      'price': price,
      if (description != null && description!.trim().isNotEmpty)
        'description': description!.trim(),
      if (svaIncluded != null && svaIncluded!.trim().isNotEmpty)
        'svaIncluded': svaIncluded!.trim(),
      'suspensionDays': suspensionDays,
      'alwaysIssueNfcom': alwaysIssueNfcom,
      'active': active,
      if (compositionItems.isNotEmpty)
        'compositionItems': compositionItems.map((i) => i.toJson()).toList(),
    };
  }
}

/// Modelo de Template de Contrato (GET/POST/PUT /contracts/templates).
class ContractTemplateModel {
  final String id;
  final String? companyId;
  final String name;
  final String documentType;
  final int version;
  final bool isActive;
  final String contentMarkdown;
  final String consentClause;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const ContractTemplateModel({
    required this.id,
    this.companyId,
    required this.name,
    required this.documentType,
    this.version = 1,
    this.isActive = true,
    required this.contentMarkdown,
    required this.consentClause,
    this.createdAt,
    this.updatedAt,
  });

  factory ContractTemplateModel.fromJson(Map<String, dynamic> json) {
    return ContractTemplateModel(
      id: json['id']?.toString() ?? '',
      companyId: json['companyId']?.toString(),
      name: json['name']?.toString() ?? '',
      documentType: json['documentType']?.toString() ?? 'SERVICE_AGREEMENT',
      version: (json['version'] as num?)?.toInt() ?? 1,
      isActive: json['isActive'] != false,
      contentMarkdown: json['contentMarkdown']?.toString() ?? '',
      consentClause: json['consentClause']?.toString() ?? '',
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }

  String get documentTypeLabel {
    switch (documentType.toUpperCase()) {
      case 'SERVICE_AGREEMENT':
        return 'Contrato de Prestação SCM/SVA';
      case 'LOYALTY_TERM':
        return 'Termo de Fidelidade / Permanência';
      case 'EQUIPMENT_COMODATO':
        return 'Termo de Comodato de Equipamentos';
      case 'CUSTOM_TERM':
        return 'Termo / Aditivo Personalizado';
      default:
        return documentType;
    }
  }
}

/// Payload para criação e atualização de Template de Contrato.
class ContractTemplateSavePayload {
  final String? companyId;
  final String name;
  final String documentType;
  final int version;
  final bool isActive;
  final String contentMarkdown;
  final String consentClause;

  const ContractTemplateSavePayload({
    this.companyId,
    required this.name,
    required this.documentType,
    this.version = 1,
    this.isActive = true,
    required this.contentMarkdown,
    required this.consentClause,
  });

  Map<String, dynamic> toJson() {
    return {
      if (companyId != null && companyId!.isNotEmpty) 'companyId': companyId,
      'name': name.trim(),
      'documentType': documentType,
      'version': version,
      'isActive': isActive,
      'contentMarkdown': contentMarkdown,
      'consentClause': consentClause.trim(),
    };
  }
}

/// Variável dinâmica disponível para interpolação em minutas contratuais.
class ContractTemplateVariableModel {
  final String tag;
  final String label;
  final String category;
  final String? example;
  final String? description;

  const ContractTemplateVariableModel({
    required this.tag,
    required this.label,
    required this.category,
    this.example,
    this.description,
  });

  factory ContractTemplateVariableModel.fromJson(Map<String, dynamic> json) {
    return ContractTemplateVariableModel(
      tag: json['tag']?.toString() ?? '',
      label: json['label']?.toString() ?? '',
      category: json['category']?.toString() ?? 'GERAL',
      example: json['example']?.toString(),
      description: json['description']?.toString(),
    );
  }
}

/// Repositório de Catálogo de Planos (`/plans`) e Templates de Contrato (`/contracts/templates`).
class CatalogRepository {
  final ApiClient _apiClient;

  CatalogRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  // ===========================================================================
  // PLANOS DE ACESSO (/plans)
  // ===========================================================================

  Future<List<AdminPlanModel>> getAllPlans() async {
    final response = await _dio.get('/plans');
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(AdminPlanModel.fromJson)
          .toList();
    }
    return [];
  }

  Future<AdminPlanModel> createPlan(PlanSavePayload payload) async {
    final response = await _dio.post('/plans', data: payload.toJson());
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return AdminPlanModel.fromJson(response.data as Map<String, dynamic>);
    }
    throw Exception('Falha ao criar plano de acesso');
  }

  Future<AdminPlanModel> updatePlan(String id, PlanSavePayload payload) async {
    final response = await _dio.put('/plans/$id', data: payload.toJson());
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return AdminPlanModel.fromJson(response.data as Map<String, dynamic>);
    }
    throw Exception('Falha ao atualizar plano de acesso');
  }

  Future<void> deletePlan(String id) async {
    await _dio.delete('/plans/$id');
  }

  // ===========================================================================
  // TEMPLATES DE CONTRATO (/contracts/templates)
  // ===========================================================================

  Future<List<ContractTemplateModel>> getContractTemplates({
    String? companyId,
  }) async {
    final response = await _dio.get(
      '/contracts/templates',
      queryParameters: {
        if (companyId != null && companyId.isNotEmpty) 'companyId': companyId,
      },
    );
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(ContractTemplateModel.fromJson)
          .toList();
    }
    return [];
  }

  Future<List<ContractTemplateVariableModel>> getTemplateVariables() async {
    final response = await _dio.get('/contracts/templates/variables');
    if (response.statusCode == 200 && response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(ContractTemplateVariableModel.fromJson)
          .toList();
    }
    return [];
  }

  Future<ContractTemplateModel> createContractTemplate(
    ContractTemplateSavePayload payload,
  ) async {
    final response = await _dio.post(
      '/contracts/templates',
      data: payload.toJson(),
    );
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return ContractTemplateModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao criar minuta contratual');
  }

  Future<ContractTemplateModel> updateContractTemplate(
    String id,
    ContractTemplateSavePayload payload,
  ) async {
    final response = await _dio.put(
      '/contracts/templates/$id',
      data: payload.toJson(),
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return ContractTemplateModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao atualizar minuta contratual');
  }

  Future<void> deleteContractTemplate(String id) async {
    await _dio.delete('/contracts/templates/$id');
  }

  Future<ContractTemplateModel> cloneContractTemplate(String id) async {
    final response = await _dio.post('/contracts/templates/$id/clone');
    if ((response.statusCode == 200 || response.statusCode == 201) &&
        response.data is Map<String, dynamic>) {
      return ContractTemplateModel.fromJson(
        response.data as Map<String, dynamic>,
      );
    }
    throw Exception('Falha ao clonar minuta contratual');
  }

  Future<String> previewContractTemplate(String content) async {
    final response = await _dio.post(
      '/contracts/templates/preview',
      data: {'content': content},
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      final map = response.data as Map<String, dynamic>;
      return map['rendered']?.toString() ?? content;
    }
    return content;
  }
}

final catalogRepositoryProvider = Provider<CatalogRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return CatalogRepository(apiClient);
});

final adminPlansListProvider = FutureProvider.autoDispose<List<AdminPlanModel>>(
  (ref) async {
    return ref.watch(catalogRepositoryProvider).getAllPlans();
  },
);

final contractTemplatesListProvider =
    FutureProvider.autoDispose<List<ContractTemplateModel>>((ref) async {
      return ref.watch(catalogRepositoryProvider).getContractTemplates();
    });

final contractTemplateVariablesProvider =
    FutureProvider.autoDispose<List<ContractTemplateVariableModel>>((ref) async {
      return ref.watch(catalogRepositoryProvider).getTemplateVariables();
    });
