import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/app_providers.dart';

// ============================================================================
// 1. EMPRESA FISCAL, CERTIFICADO DIGITAL A1 E GATEWAY FISCAL
// ============================================================================

class FiscalCompanyModel {
  final String id;
  final String cnpj;
  final String razaoSocial;
  final String? nomeFantasia;
  final String inscricaoEstadual;
  final String? inscricaoMunicipal;
  final String cnaePrincipal;
  final String regimeTributario;
  final double aliquotaIcms;
  final double aliquotaFust;
  final double aliquotaFunttel;
  final double aliquotaPis;
  final double aliquotaCofins;
  final String logradouro;
  final String numero;
  final String? complemento;
  final String bairro;
  final String cidade;
  final String uf;
  final String cep;
  final String codigoIbge;
  final String? telefone;
  final String? emailFiscal;
  final String nfcomAmbiente;
  final String nfcomSerie;
  final int nfcomProximoNumero;
  final bool hasCertificate;
  final String? certificateExpiresAt;
  final String? accountingName;
  final String? accountingEmails;
  final int accountingSendDay;
  final bool accountingAutoSend;
  final String? accountingLastSentAt;
  final bool fiscalConfirmed;
  final String? fiscalConfirmedAt;
  final bool isActive;

  const FiscalCompanyModel({
    required this.id,
    required this.cnpj,
    required this.razaoSocial,
    this.nomeFantasia,
    required this.inscricaoEstadual,
    this.inscricaoMunicipal,
    required this.cnaePrincipal,
    required this.regimeTributario,
    required this.aliquotaIcms,
    required this.aliquotaFust,
    required this.aliquotaFunttel,
    required this.aliquotaPis,
    required this.aliquotaCofins,
    required this.logradouro,
    required this.numero,
    this.complemento,
    required this.bairro,
    required this.cidade,
    required this.uf,
    required this.cep,
    required this.codigoIbge,
    this.telefone,
    this.emailFiscal,
    required this.nfcomAmbiente,
    required this.nfcomSerie,
    required this.nfcomProximoNumero,
    required this.hasCertificate,
    this.certificateExpiresAt,
    this.accountingName,
    this.accountingEmails,
    required this.accountingSendDay,
    required this.accountingAutoSend,
    this.accountingLastSentAt,
    required this.fiscalConfirmed,
    this.fiscalConfirmedAt,
    required this.isActive,
  });

  factory FiscalCompanyModel.fromJson(Map<String, dynamic> json) {
    return FiscalCompanyModel(
      id: (json['id'] ?? '').toString(),
      cnpj: (json['cnpj'] ?? '').toString(),
      razaoSocial: (json['razaoSocial'] ?? '').toString(),
      nomeFantasia: json['nomeFantasia']?.toString(),
      inscricaoEstadual: (json['inscricaoEstadual'] ?? '').toString(),
      inscricaoMunicipal: json['inscricaoMunicipal']?.toString(),
      cnaePrincipal: (json['cnaePrincipal'] ?? '6110-8/03').toString(),
      regimeTributario: (json['regimeTributario'] ?? 'SIMPLES_NACIONAL').toString(),
      aliquotaIcms: (json['aliquotaIcms'] as num?)?.toDouble() ?? 0.0,
      aliquotaFust: (json['aliquotaFust'] as num?)?.toDouble() ?? 0.0,
      aliquotaFunttel: (json['aliquotaFunttel'] as num?)?.toDouble() ?? 0.0,
      aliquotaPis: (json['aliquotaPis'] as num?)?.toDouble() ?? 0.0,
      aliquotaCofins: (json['aliquotaCofins'] as num?)?.toDouble() ?? 0.0,
      logradouro: (json['logradouro'] ?? '').toString(),
      numero: (json['numero'] ?? '').toString(),
      complemento: json['complemento']?.toString(),
      bairro: (json['bairro'] ?? '').toString(),
      cidade: (json['cidade'] ?? '').toString(),
      uf: (json['uf'] ?? '').toString(),
      cep: (json['cep'] ?? '').toString(),
      codigoIbge: (json['codigoIbge'] ?? '').toString(),
      telefone: json['telefone']?.toString(),
      emailFiscal: json['emailFiscal']?.toString(),
      nfcomAmbiente: (json['nfcomAmbiente'] ?? 'HOMOLOGACAO').toString(),
      nfcomSerie: (json['nfcomSerie'] ?? '1').toString(),
      nfcomProximoNumero: (json['nfcomProximoNumero'] as num?)?.toInt() ?? 1,
      hasCertificate: json['hasCertificate'] == true,
      certificateExpiresAt: json['certificateExpiresAt']?.toString(),
      accountingName: json['accountingName']?.toString(),
      accountingEmails: json['accountingEmails']?.toString(),
      accountingSendDay: (json['accountingSendDay'] as num?)?.toInt() ?? 5,
      accountingAutoSend: json['accountingAutoSend'] == true,
      accountingLastSentAt: json['accountingLastSentAt']?.toString(),
      fiscalConfirmed: json['fiscalConfirmed'] == true,
      fiscalConfirmedAt: json['fiscalConfirmedAt']?.toString(),
      isActive: json['isActive'] != false,
    );
  }
}

class CertificateUploadResultModel {
  final bool success;
  final String? subjectCnpj;
  final String? subjectName;
  final String? validUntil;
  final String? message;
  final String? errorMessage;

  const CertificateUploadResultModel({
    required this.success,
    this.subjectCnpj,
    this.subjectName,
    this.validUntil,
    this.message,
    this.errorMessage,
  });

  factory CertificateUploadResultModel.fromJson(Map<String, dynamic> json) {
    return CertificateUploadResultModel(
      success: json['success'] == true,
      subjectCnpj: json['subjectCnpj']?.toString(),
      subjectName: json['subjectName']?.toString(),
      validUntil: json['validUntil']?.toString(),
      message: json['message']?.toString(),
      errorMessage: json['errorMessage']?.toString(),
    );
  }
}

class FiscalGatewayConfigModel {
  final String? id;
  final String companyId;
  final String gatewayType;
  final String environment;
  final String baseUrl;
  final bool isActive;

  const FiscalGatewayConfigModel({
    this.id,
    required this.companyId,
    required this.gatewayType,
    required this.environment,
    required this.baseUrl,
    required this.isActive,
  });

  factory FiscalGatewayConfigModel.fromJson(Map<String, dynamic> json) {
    return FiscalGatewayConfigModel(
      id: json['id']?.toString(),
      companyId: (json['companyId'] ?? '').toString(),
      gatewayType: (json['gatewayType'] ?? 'XINGUBIT_PAY').toString(),
      environment: (json['environment'] ?? 'HOMOLOGACAO').toString(),
      baseUrl: (json['baseUrl'] ?? '').toString(),
      isActive: json['isActive'] != false,
    );
  }
}

// ============================================================================
// 2. NFCOM MODELO 62, CONVÊNIO 115 E CONTABILIDADE
// ============================================================================

class NfcomRecordModel {
  final String id;
  final String companyId;
  final String? invoiceId;
  final String? contractId;
  final String customerId;
  final String? chaveAcesso;
  final int numero;
  final String serie;
  final String modelo;
  final String tipoEmissao;
  final String ambiente;
  final String status;
  final String? protocoloAutorizacao;
  final String? dataAutorizacao;
  final String? digestValue;
  final double valorTotal;
  final double valorIcms;
  final double valorFust;
  final double valorFunttel;
  final String? xmlAutorizado;
  final String? danfePdfUrl;
  final String? motivoCancelamento;
  final String? dataCancelamento;
  final String? createdAt;

  const NfcomRecordModel({
    required this.id,
    required this.companyId,
    this.invoiceId,
    this.contractId,
    required this.customerId,
    this.chaveAcesso,
    required this.numero,
    required this.serie,
    required this.modelo,
    required this.tipoEmissao,
    required this.ambiente,
    required this.status,
    this.protocoloAutorizacao,
    this.dataAutorizacao,
    this.digestValue,
    required this.valorTotal,
    required this.valorIcms,
    required this.valorFust,
    required this.valorFunttel,
    this.xmlAutorizado,
    this.danfePdfUrl,
    this.motivoCancelamento,
    this.dataCancelamento,
    this.createdAt,
  });

  factory NfcomRecordModel.fromJson(Map<String, dynamic> json) {
    return NfcomRecordModel(
      id: (json['id'] ?? '').toString(),
      companyId: (json['companyId'] ?? '').toString(),
      invoiceId: json['invoiceId']?.toString(),
      contractId: json['contractId']?.toString(),
      customerId: (json['customerId'] ?? '').toString(),
      chaveAcesso: json['chaveAcesso']?.toString(),
      numero: (json['numero'] as num?)?.toInt() ?? 0,
      serie: (json['serie'] ?? '1').toString(),
      modelo: (json['modelo'] ?? '62').toString(),
      tipoEmissao: (json['tipoEmissao'] ?? 'NORMAL').toString(),
      ambiente: (json['ambiente'] ?? 'HOMOLOGACAO').toString(),
      status: (json['status'] ?? 'PENDENTE').toString(),
      protocoloAutorizacao: json['protocoloAutorizacao']?.toString(),
      dataAutorizacao: json['dataAutorizacao']?.toString(),
      digestValue: json['digestValue']?.toString(),
      valorTotal: (json['valorTotal'] as num?)?.toDouble() ?? 0.0,
      valorIcms: (json['valorIcms'] as num?)?.toDouble() ?? 0.0,
      valorFust: (json['valorFust'] as num?)?.toDouble() ?? 0.0,
      valorFunttel: (json['valorFunttel'] as num?)?.toDouble() ?? 0.0,
      xmlAutorizado: json['xmlAutorizado']?.toString(),
      danfePdfUrl: json['danfePdfUrl']?.toString(),
      motivoCancelamento: json['motivoCancelamento']?.toString(),
      dataCancelamento: json['dataCancelamento']?.toString(),
      createdAt: json['createdAt']?.toString(),
    );
  }
}

class NfcomRecordsPageModel {
  final List<NfcomRecordModel> content;
  final int totalElements;
  final int totalPages;
  final int size;
  final int number;

  const NfcomRecordsPageModel({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.size,
    required this.number,
  });

  factory NfcomRecordsPageModel.fromJson(Map<String, dynamic> json) {
    final raw = (json['content'] as List<dynamic>? ?? [])
        .map((e) => NfcomRecordModel.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList();
    return NfcomRecordsPageModel(
      content: raw,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? raw.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 1,
      size: (json['size'] as num?)?.toInt() ?? 15,
      number: (json['number'] as num?)?.toInt() ?? 0,
    );
  }
}

class NfcomCancelResultModel {
  final bool success;
  final String? chaveAcesso;
  final String? protocoloCancelamento;
  final String? dataCancelamento;
  final String? errorMessage;

  const NfcomCancelResultModel({
    required this.success,
    this.chaveAcesso,
    this.protocoloCancelamento,
    this.dataCancelamento,
    this.errorMessage,
  });

  factory NfcomCancelResultModel.fromJson(Map<String, dynamic> json) {
    return NfcomCancelResultModel(
      success: json['success'] == true,
      chaveAcesso: json['chaveAcesso']?.toString(),
      protocoloCancelamento: json['protocoloCancelamento']?.toString(),
      dataCancelamento: json['dataCancelamento']?.toString(),
      errorMessage: json['errorMessage']?.toString(),
    );
  }
}

class SendAccountingReportResultModel {
  final bool success;
  final String message;
  final String? sentAt;

  const SendAccountingReportResultModel({
    required this.success,
    required this.message,
    this.sentAt,
  });

  factory SendAccountingReportResultModel.fromJson(Map<String, dynamic> json) {
    return SendAccountingReportResultModel(
      success: json['success'] == true,
      message: (json['message'] ?? '').toString(),
      sentAt: json['sentAt']?.toString(),
    );
  }
}

// ============================================================================
// 3. TRANSIÇÃO DE REGIME TRIBUTÁRIO
// ============================================================================

class FiscalRegimeTransitionModel {
  final String? id;
  final String companyId;
  final String previousRegime;
  final String newRegime;
  final String effectiveDate;
  final double? aliquotaIcms;
  final double? aliquotaPis;
  final double? aliquotaCofins;
  final double? aliquotaFust;
  final double? aliquotaFunttel;
  final String status;
  final String? notes;
  final String? appliedAt;
  final String createdAt;

  const FiscalRegimeTransitionModel({
    this.id,
    required this.companyId,
    required this.previousRegime,
    required this.newRegime,
    required this.effectiveDate,
    this.aliquotaIcms,
    this.aliquotaPis,
    this.aliquotaCofins,
    this.aliquotaFust,
    this.aliquotaFunttel,
    required this.status,
    this.notes,
    this.appliedAt,
    required this.createdAt,
  });

  factory FiscalRegimeTransitionModel.fromJson(Map<String, dynamic> json) {
    return FiscalRegimeTransitionModel(
      id: json['id']?.toString(),
      companyId: (json['companyId'] ?? '').toString(),
      previousRegime: (json['previousRegime'] ?? '').toString(),
      newRegime: (json['newRegime'] ?? '').toString(),
      effectiveDate: (json['effectiveDate'] ?? '').toString(),
      aliquotaIcms: (json['aliquotaIcms'] as num?)?.toDouble(),
      aliquotaPis: (json['aliquotaPis'] as num?)?.toDouble(),
      aliquotaCofins: (json['aliquotaCofins'] as num?)?.toDouble(),
      aliquotaFust: (json['aliquotaFust'] as num?)?.toDouble(),
      aliquotaFunttel: (json['aliquotaFunttel'] as num?)?.toDouble(),
      status: (json['status'] ?? 'SCHEDULED').toString(),
      notes: json['notes']?.toString(),
      appliedAt: json['appliedAt']?.toString(),
      createdAt: (json['createdAt'] ?? '').toString(),
    );
  }
}

// ============================================================================
// REPOSITÓRIO HTTP E PROVIDERS RIVERPOD
// ============================================================================

class FiscalRepository {
  final Dio _dio;

  FiscalRepository(this._dio);

  Future<FiscalCompanyModel?> getActiveFiscalCompany() async {
    try {
      final response = await _dio.get('/fiscal/company');
      if (response.data == null) return null;
      return FiscalCompanyModel.fromJson(
        Map<String, dynamic>.from(response.data as Map),
      );
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<FiscalCompanyModel> saveFiscalCompany({
    String? id,
    required String cnpj,
    required String razaoSocial,
    String? nomeFantasia,
    required String inscricaoEstadual,
    String? inscricaoMunicipal,
    String cnaePrincipal = '6110-8/03',
    required String regimeTributario,
    double? aliquotaIcms,
    double? aliquotaFust,
    double? aliquotaFunttel,
    double? aliquotaPis,
    double? aliquotaCofins,
    required String logradouro,
    required String numero,
    String? complemento,
    required String bairro,
    required String cidade,
    required String uf,
    required String cep,
    required String codigoIbge,
    String? telefone,
    String? emailFiscal,
    String nfcomAmbiente = 'HOMOLOGACAO',
    String nfcomSerie = '1',
    int? nfcomProximoNumero,
    String? accountingName,
    String? accountingEmails,
    int? accountingSendDay,
    bool? accountingAutoSend,
    bool? fiscalConfirmed,
  }) async {
    final response = await _dio.post(
      '/fiscal/company',
      data: {
        'id': ?id,
        'cnpj': cnpj,
        'razaoSocial': razaoSocial,
        'nomeFantasia': ?nomeFantasia,
        'inscricaoEstadual': inscricaoEstadual,
        'inscricaoMunicipal': ?inscricaoMunicipal,
        'cnaePrincipal': cnaePrincipal,
        'regimeTributario': regimeTributario,
        'aliquotaIcms': ?aliquotaIcms,
        'aliquotaFust': ?aliquotaFust,
        'aliquotaFunttel': ?aliquotaFunttel,
        'aliquotaPis': ?aliquotaPis,
        'aliquotaCofins': ?aliquotaCofins,
        'logradouro': logradouro,
        'numero': numero,
        'complemento': ?complemento,
        'bairro': bairro,
        'cidade': cidade,
        'uf': uf,
        'cep': cep,
        'codigoIbge': codigoIbge,
        'telefone': ?telefone,
        'emailFiscal': ?emailFiscal,
        'nfcomAmbiente': nfcomAmbiente,
        'nfcomSerie': nfcomSerie,
        'nfcomProximoNumero': ?nfcomProximoNumero,
        'accountingName': ?accountingName,
        'accountingEmails': ?accountingEmails,
        'accountingSendDay': ?accountingSendDay,
        'accountingAutoSend': ?accountingAutoSend,
        'fiscalConfirmed': ?fiscalConfirmed,
      },
    );
    return FiscalCompanyModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<CertificateUploadResultModel> uploadFiscalCertificate({
    required String companyId,
    required List<int> fileBytes,
    required String filename,
    required String password,
  }) async {
    final formData = FormData.fromMap({
      'file': MultipartFile.fromBytes(fileBytes, filename: filename),
      'password': password,
    });
    final response = await _dio.post(
      '/fiscal/company/$companyId/certificate',
      data: formData,
    );
    return CertificateUploadResultModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<FiscalGatewayConfigModel> getActiveFiscalConfig() async {
    final response = await _dio.get('/fiscal/configs');
    return FiscalGatewayConfigModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<NfcomRecordModel> emitNfcom(String invoiceId) async {
    final response = await _dio.post('/fiscal/invoices/$invoiceId/emit');
    return NfcomRecordModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<NfcomRecordsPageModel> listNfcomRecords({
    int page = 0,
    int size = 15,
    String? sort,
  }) async {
    final response = await _dio.get(
      '/fiscal/records',
      queryParameters: {
        'page': page,
        'size': size,
        'sort': ?sort,
      },
    );
    return NfcomRecordsPageModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<NfcomCancelResultModel> cancelNfcom({
    required String recordId,
    required String reason,
  }) async {
    final response = await _dio.post(
      '/fiscal/records/$recordId/cancel',
      data: {'reason': reason},
    );
    return NfcomCancelResultModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<List<int>> exportConvenio115({int? year, int? month}) async {
    final response = await _dio.get<List<int>>(
      '/fiscal/convenio115/export',
      queryParameters: {
        'year': ?year,
        'month': ?month,
      },
      options: Options(responseType: ResponseType.bytes),
    );
    return response.data ?? const [];
  }

  Future<SendAccountingReportResultModel> sendAccountingReport({
    int? year,
    int? month,
  }) async {
    final response = await _dio.post(
      '/fiscal/convenio115/send-accounting',
      queryParameters: {
        'year': ?year,
        'month': ?month,
      },
    );
    return SendAccountingReportResultModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<List<FiscalRegimeTransitionModel>> getTransitionHistory({
    String? companyId,
  }) async {
    final response = await _dio.get(
      '/fiscal/regimes/transitions',
      queryParameters: {
        'companyId': ?companyId,
      },
    );
    final raw = response.data as List<dynamic>? ?? [];
    return raw
        .map((e) => FiscalRegimeTransitionModel.fromJson(
              Map<String, dynamic>.from(e as Map),
            ))
        .toList();
  }

  Future<FiscalRegimeTransitionModel> scheduleOrApplyTransition({
    String? companyId,
    required String newRegime,
    required String effectiveDate,
    double? aliquotaIcms,
    double? aliquotaPis,
    double? aliquotaCofins,
    double? aliquotaFust,
    double? aliquotaFunttel,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/fiscal/regimes/transitions',
      data: {
        'companyId': ?companyId,
        'newRegime': newRegime,
        'effectiveDate': effectiveDate,
        'aliquotaIcms': ?aliquotaIcms,
        'aliquotaPis': ?aliquotaPis,
        'aliquotaCofins': ?aliquotaCofins,
        'aliquotaFust': ?aliquotaFust,
        'aliquotaFunttel': ?aliquotaFunttel,
        'notes': ?notes,
      },
    );
    return FiscalRegimeTransitionModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<FiscalRegimeTransitionModel> cancelTransition(String id) async {
    final response = await _dio.delete('/fiscal/regimes/transitions/$id');
    return FiscalRegimeTransitionModel.fromJson(
      Map<String, dynamic>.from(response.data as Map),
    );
  }

  Future<int> triggerProcessPendingTransitions() async {
    final response =
        await _dio.post('/fiscal/regimes/transitions/process-pending');
    if (response.data is num) {
      return (response.data as num).toInt();
    }
    return int.tryParse(response.data.toString()) ?? 0;
  }
}

final fiscalRepositoryProvider = Provider<FiscalRepository>((ref) {
  final dio = ref.watch(apiClientProvider).dio;
  return FiscalRepository(dio);
});

final activeFiscalCompanyProvider =
    FutureProvider<FiscalCompanyModel?>((ref) async {
  return ref.watch(fiscalRepositoryProvider).getActiveFiscalCompany();
});

final activeFiscalGatewayConfigProvider =
    FutureProvider<FiscalGatewayConfigModel>((ref) async {
  return ref.watch(fiscalRepositoryProvider).getActiveFiscalConfig();
});

final nfcomRecordsPageProvider =
    FutureProvider.family<NfcomRecordsPageModel, int>((ref, page) async {
  return ref.watch(fiscalRepositoryProvider).listNfcomRecords(
        page: page,
        size: 15,
      );
});

final fiscalRegimeTransitionsProvider =
    FutureProvider<List<FiscalRegimeTransitionModel>>((ref) async {
  return ref.watch(fiscalRepositoryProvider).getTransitionHistory();
});
