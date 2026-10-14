import 'package:flutter/foundation.dart';

/// Estados possíveis da sessão de assinatura eletrônica forense.
enum SignatureStatusEnum {
  pending('PENDING', 'Aguardando PIX Simbólico'),
  signed('SIGNED', 'Assinado com Validade Forense'),
  rejectedDivergentDocument(
    'REJECTED_DIVERGENT_DOCUMENT',
    'Rejeitado: CPF Pagador Divergente',
  ),
  expired('EXPIRED', 'Sessão Expirada'),
  canceled('CANCELED', 'Cancelado');

  final String apiValue;
  final String label;

  const SignatureStatusEnum(this.apiValue, this.label);

  static SignatureStatusEnum fromString(String? raw) {
    final val = (raw ?? 'PENDING').toUpperCase();
    for (final s in SignatureStatusEnum.values) {
      if (s.apiValue == val) return s;
    }
    return SignatureStatusEnum.pending;
  }
}

/// Métodos de assinatura eletrônica (Principal via PIX Simbólico ou Fallbacks MP 2.200-2).
enum FallbackMethodEnum {
  pix('PIX', 'PIX Simbólico (Conciliação BACEN)'),
  govBr('GOV_BR', 'Assinatura Gov.br (Prata / Ouro)'),
  emailOtp('EMAIL_OTP', 'Token OTP via E-mail / IP Auditado'),
  physicalNotary('PHYSICAL_NOTARY', 'Assinatura Física Presencial / Cartório');

  final String apiValue;
  final String label;

  const FallbackMethodEnum(this.apiValue, this.label);

  static FallbackMethodEnum? fromString(String? raw) {
    if (raw == null || raw.isEmpty) return null;
    final val = raw.toUpperCase();
    for (final m in FallbackMethodEnum.values) {
      if (m.apiValue == val) return m;
    }
    return null;
  }
}

/// Sessão de Assinatura Eletrônica Forense (PIX Simbólico R$ 1,00 + Hash SHA-256 + MP 2.200-2).
@immutable
class SignatureSessionModel {
  final String id;
  final String contractId;
  final String? templateId;
  final String token;
  final String signatureUrl;
  final SignatureStatusEnum status;
  final double symbolicAmount;
  final String? pixTxid;
  final String? pixCopyPaste;
  final String? pixQrCodeBase64;
  final String? pixEndToEndId;
  final String? documentSha256Hash;
  final String? payerName;
  final String? payerCpfCnpj;
  final String? payerBankName;
  final String? rejectionReason;
  final String? signedPdfUrl;
  final FallbackMethodEnum? fallbackMethod;
  final double? onboardingCreditAmount;
  final String? forensicCertificatePdfUrl;
  final String? expiresAt;
  final String? signedAt;
  final String? createdAt;

  const SignatureSessionModel({
    required this.id,
    required this.contractId,
    this.templateId,
    required this.token,
    required this.signatureUrl,
    required this.status,
    required this.symbolicAmount,
    this.pixTxid,
    this.pixCopyPaste,
    this.pixQrCodeBase64,
    this.pixEndToEndId,
    this.documentSha256Hash,
    this.payerName,
    this.payerCpfCnpj,
    this.payerBankName,
    this.rejectionReason,
    this.signedPdfUrl,
    this.fallbackMethod,
    this.onboardingCreditAmount,
    this.forensicCertificatePdfUrl,
    this.expiresAt,
    this.signedAt,
    this.createdAt,
  });

  factory SignatureSessionModel.fromJson(Map<String, dynamic> json) {
    return SignatureSessionModel(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      templateId: json['templateId']?.toString(),
      token: json['token']?.toString() ?? '',
      signatureUrl: json['signatureUrl']?.toString() ?? '',
      status: SignatureStatusEnum.fromString(json['status']?.toString()),
      symbolicAmount: (json['symbolicAmount'] as num?)?.toDouble() ?? 1.0,
      pixTxid: json['pixTxid']?.toString(),
      pixCopyPaste: json['pixCopyPaste']?.toString(),
      pixQrCodeBase64: json['pixQrCodeBase64']?.toString(),
      pixEndToEndId: json['pixEndToEndId']?.toString(),
      documentSha256Hash: json['documentSha256Hash']?.toString(),
      payerName: json['payerName']?.toString(),
      payerCpfCnpj: json['payerCpfCnpj']?.toString(),
      payerBankName: json['payerBankName']?.toString(),
      rejectionReason: json['rejectionReason']?.toString(),
      signedPdfUrl: json['signedPdfUrl']?.toString(),
      fallbackMethod:
          FallbackMethodEnum.fromString(json['fallbackMethod']?.toString()),
      onboardingCreditAmount:
          (json['onboardingCreditAmount'] as num?)?.toDouble(),
      forensicCertificatePdfUrl: json['forensicCertificatePdfUrl']?.toString(),
      expiresAt: json['expiresAt']?.toString(),
      signedAt: json['signedAt']?.toString(),
      createdAt: json['createdAt']?.toString(),
    );
  }

  bool get isSigned => status == SignatureStatusEnum.signed;
  bool get isRejected =>
      status == SignatureStatusEnum.rejectedDivergentDocument;
}
