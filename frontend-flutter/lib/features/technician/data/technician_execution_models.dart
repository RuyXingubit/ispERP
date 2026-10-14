import 'package:flutter/foundation.dart';

/// ONU descoberta via Auto-Find nas portas PON da OLT aguardando autorização.
@immutable
class OltUnprovisionedOnuModel {
  final String networkDeviceId;
  final String oltName;
  final int slotNumber;
  final int portNumber;
  final String? ponName;
  final String onuSerial;
  final String? onuMac;
  final double? rxPowerDbm;
  final String? detectedAt;

  const OltUnprovisionedOnuModel({
    required this.networkDeviceId,
    required this.oltName,
    required this.slotNumber,
    required this.portNumber,
    this.ponName,
    required this.onuSerial,
    this.onuMac,
    this.rxPowerDbm,
    this.detectedAt,
  });

  factory OltUnprovisionedOnuModel.fromJson(Map<String, dynamic> json) {
    return OltUnprovisionedOnuModel(
      networkDeviceId: json['networkDeviceId']?.toString() ?? '',
      oltName: json['oltName']?.toString() ?? 'OLT Principal',
      slotNumber: (json['slotNumber'] as num?)?.toInt() ?? 1,
      portNumber: (json['portNumber'] as num?)?.toInt() ?? 1,
      ponName: json['ponName']?.toString(),
      onuSerial: json['onuSerial']?.toString() ?? '',
      onuMac: json['onuMac']?.toString(),
      rxPowerDbm: (json['rxPowerDbm'] as num?)?.toDouble(),
      detectedAt: json['detectedAt']?.toString(),
    );
  }

  String get displayPon => ponName != null && ponName!.isNotEmpty
      ? ponName!
      : 'PON $slotNumber/$portNumber';

  /// Indica se o sinal óptico está dentro da faixa saudável (>= -25.0 dBm).
  bool get isSignalOptimal => rxPowerDbm != null && rxPowerDbm! >= -25.0;

  /// Indica se o sinal óptico está em faixa crítica (< -27.0 dBm).
  bool get isSignalCritical => rxPowerDbm != null && rxPowerDbm! < -27.0;
}

/// Resposta do provisionamento 1-clique da ONU na OLT.
@immutable
class ProvisionOnuResult {
  final bool success;
  final String message;

  const ProvisionOnuResult({
    required this.success,
    required this.message,
  });

  factory ProvisionOnuResult.fromJson(Map<String, dynamic> json) {
    return ProvisionOnuResult(
      success: json['success'] == true,
      message: json['message']?.toString() ?? 'ONU provisionada com sucesso.',
    );
  }
}

/// Status em tempo real da sessão PPPoE no FreeRADIUS durante execução em campo.
@immutable
class RadiusSessionStatusModel {
  final String workOrderId;
  final bool authenticated;
  final String? username;
  final String? framedIpAddress;
  final String? macAddress;
  final String? nasIpAddress;
  final int? uptimeSeconds;

  const RadiusSessionStatusModel({
    required this.workOrderId,
    required this.authenticated,
    this.username,
    this.framedIpAddress,
    this.macAddress,
    this.nasIpAddress,
    this.uptimeSeconds,
  });

  factory RadiusSessionStatusModel.fromJson(Map<String, dynamic> json) {
    return RadiusSessionStatusModel(
      workOrderId: json['workOrderId']?.toString() ?? '',
      authenticated: json['authenticated'] == true,
      username: json['username']?.toString(),
      framedIpAddress: json['framedIpAddress']?.toString(),
      macAddress: json['macAddress']?.toString(),
      nasIpAddress: json['nasIpAddress']?.toString(),
      uptimeSeconds: (json['uptimeSeconds'] as num?)?.toInt(),
    );
  }

  String get formattedUptime {
    final secs = uptimeSeconds ?? 0;
    if (secs <= 0) return '0s';
    final minutes = secs ~/ 60;
    final remainingSecs = secs % 60;
    if (minutes < 60) {
      return '${minutes}m ${remainingSecs}s';
    }
    final hours = minutes ~/ 60;
    final remainingMins = minutes % 60;
    return '${hours}h ${remainingMins}m';
  }
}

/// Payload para encerramento completo da O.S. pelo técnico em campo.
@immutable
class TechnicianExecutionCompletePayload {
  final String onuSerial;
  final String? onuMac;
  final int? vlanId;
  final String? pppoeUsername;
  final String? pppoePassword;
  final String? wifiSsid;
  final String? wifiPassword;
  final double? fiberSignalDbm;
  final String? installationPhotoUrl;
  final String? customerSignatureName;
  final String? notes;
  final String? warehouseId;

  const TechnicianExecutionCompletePayload({
    required this.onuSerial,
    this.onuMac,
    this.vlanId,
    this.pppoeUsername,
    this.pppoePassword,
    this.wifiSsid,
    this.wifiPassword,
    this.fiberSignalDbm,
    this.installationPhotoUrl,
    this.customerSignatureName,
    this.notes,
    this.warehouseId,
  });

  Map<String, dynamic> toJson() {
    return {
      'onuSerial': onuSerial,
      'onuMac': ?onuMac,
      'vlanId': ?vlanId,
      'pppoeUsername': ?pppoeUsername,
      'pppoePassword': ?pppoePassword,
      'wifiSsid': ?wifiSsid,
      'wifiPassword': ?wifiPassword,
      'fiberSignalDbm': ?fiberSignalDbm,
      'installationPhotoUrl': ?installationPhotoUrl,
      'customerSignatureName': ?customerSignatureName,
      'notes': ?notes,
      'warehouseId': ?warehouseId,
    };
  }
}

/// Payload para concluir recolha de equipamento (O.S. RETIRADA) devolvendo ao almoxarifado.
@immutable
class CompleteRemovalPayload {
  final String warehouseId;
  final String? photoUrl;
  final String? notes;

  const CompleteRemovalPayload({
    required this.warehouseId,
    this.photoUrl,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'warehouseId': warehouseId,
      'photoUrl': ?photoUrl,
      'notes': ?notes,
    };
  }
}

/// Payload para registrar tentativa frustrada de recolha de equipamento (O.S. RETIRADA -> INFRUTIFERA / SPC).
@immutable
class FailRemovalPayload {
  final String unsuccessReason;
  final String? photoUrl;
  final String? notes;

  const FailRemovalPayload({
    required this.unsuccessReason,
    this.photoUrl,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'unsuccessReason': unsuccessReason,
      'photoUrl': ?photoUrl,
      'notes': ?notes,
    };
  }
}

/// Resultado de cobrança jurídica / negativação gerada após tentativa infrutífera de recolha.
@immutable
class LegalCollectionResultModel {
  final String id;
  final String customerId;
  final String contractId;
  final String? workOrderId;
  final double totalDebtInvoices;
  final double equipmentIndemnityAmount;
  final double totalClaimAmount;
  final String status;
  final String? evidenceNotes;

  const LegalCollectionResultModel({
    required this.id,
    required this.customerId,
    required this.contractId,
    this.workOrderId,
    required this.totalDebtInvoices,
    required this.equipmentIndemnityAmount,
    required this.totalClaimAmount,
    required this.status,
    this.evidenceNotes,
  });

  factory LegalCollectionResultModel.fromJson(Map<String, dynamic> json) {
    return LegalCollectionResultModel(
      id: json['id']?.toString() ?? '',
      customerId: json['customerId']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      workOrderId: json['workOrderId']?.toString(),
      totalDebtInvoices: (json['totalDebtInvoices'] as num?)?.toDouble() ?? 0.0,
      equipmentIndemnityAmount:
          (json['equipmentIndemnityAmount'] as num?)?.toDouble() ?? 0.0,
      totalClaimAmount: (json['totalClaimAmount'] as num?)?.toDouble() ?? 0.0,
      status: json['status']?.toString() ?? 'PENDING_BUREAU_SUBMISSION',
      evidenceNotes: json['evidenceNotes']?.toString(),
    );
  }
}
