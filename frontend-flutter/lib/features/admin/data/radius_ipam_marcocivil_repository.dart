import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Modelo de Sessão RADIUS (`RadiusSessionResponse`).
class RadiusSessionModel {
  final int radacctId;
  final String acctSessionId;
  final String username;
  final String nasIpAddress;
  final String? nasShortname;
  final String? acctStartTime;
  final String? acctUpdateTime;
  final String? acctStopTime;
  final int? acctSessionTime;
  final int acctInputOctets;
  final int acctOutputOctets;
  final String? callingStationId;
  final String? framedIpAddress;
  final String? framedIpv6Prefix;
  final String? delegatedIpv6Prefix;
  final bool isOnline;
  final String? customerName;
  final String? customerCpfCnpj;

  const RadiusSessionModel({
    required this.radacctId,
    required this.acctSessionId,
    required this.username,
    required this.nasIpAddress,
    this.nasShortname,
    this.acctStartTime,
    this.acctUpdateTime,
    this.acctStopTime,
    this.acctSessionTime,
    required this.acctInputOctets,
    required this.acctOutputOctets,
    this.callingStationId,
    this.framedIpAddress,
    this.framedIpv6Prefix,
    this.delegatedIpv6Prefix,
    required this.isOnline,
    this.customerName,
    this.customerCpfCnpj,
  });

  factory RadiusSessionModel.fromJson(Map<String, dynamic> json) {
    return RadiusSessionModel(
      radacctId: (json['radacctId'] as num?)?.toInt() ?? 0,
      acctSessionId: json['acctSessionId']?.toString() ?? '',
      username: json['username']?.toString() ?? '',
      nasIpAddress: json['nasIpAddress']?.toString() ?? '',
      nasShortname: json['nasShortname']?.toString(),
      acctStartTime: json['acctStartTime']?.toString(),
      acctUpdateTime: json['acctUpdateTime']?.toString(),
      acctStopTime: json['acctStopTime']?.toString(),
      acctSessionTime: (json['acctSessionTime'] as num?)?.toInt(),
      acctInputOctets: (json['acctInputOctets'] as num?)?.toInt() ?? 0,
      acctOutputOctets: (json['acctOutputOctets'] as num?)?.toInt() ?? 0,
      callingStationId: json['callingStationId']?.toString(),
      framedIpAddress: json['framedIpAddress']?.toString(),
      framedIpv6Prefix: json['framedIpv6Prefix']?.toString(),
      delegatedIpv6Prefix: json['delegatedIpv6Prefix']?.toString(),
      isOnline: json['isOnline'] as bool? ?? false,
      customerName: json['customerName']?.toString(),
      customerCpfCnpj: json['customerCpfCnpj']?.toString(),
    );
  }
}

/// Resultado de comando PoD (`RadiusDisconnectResponse`).
class RadiusDisconnectResultModel {
  final String username;
  final bool success;
  final String message;

  const RadiusDisconnectResultModel({
    required this.username,
    required this.success,
    required this.message,
  });

  factory RadiusDisconnectResultModel.fromJson(Map<String, dynamic> json) {
    return RadiusDisconnectResultModel(
      username: json['username']?.toString() ?? '',
      success: json['success'] as bool? ?? false,
      message: json['message']?.toString() ?? '',
    );
  }
}

/// Resumo do Ciclo de Vida RADIUS (`RadiusLifecycleSummaryResponse`).
class RadiusLifecycleSummaryModel {
  final int totalPppoeUsers;
  final int totalActiveUsers;
  final int totalBlockedUsers;
  final int totalTrustUnblocked;
  final int todayAutoBlocksCount;
  final int todayUnblocksCount;
  final int toleranceDays;
  final bool autoBlockEnabled;

  const RadiusLifecycleSummaryModel({
    required this.totalPppoeUsers,
    required this.totalActiveUsers,
    required this.totalBlockedUsers,
    required this.totalTrustUnblocked,
    required this.todayAutoBlocksCount,
    required this.todayUnblocksCount,
    required this.toleranceDays,
    required this.autoBlockEnabled,
  });

  factory RadiusLifecycleSummaryModel.fromJson(Map<String, dynamic> json) {
    return RadiusLifecycleSummaryModel(
      totalPppoeUsers: (json['totalPppoeUsers'] as num?)?.toInt() ?? 0,
      totalActiveUsers: (json['totalActiveUsers'] as num?)?.toInt() ?? 0,
      totalBlockedUsers: (json['totalBlockedUsers'] as num?)?.toInt() ?? 0,
      totalTrustUnblocked: (json['totalTrustUnblocked'] as num?)?.toInt() ?? 0,
      todayAutoBlocksCount: (json['todayAutoBlocksCount'] as num?)?.toInt() ?? 0,
      todayUnblocksCount: (json['todayUnblocksCount'] as num?)?.toInt() ?? 0,
      toleranceDays: (json['toleranceDays'] as num?)?.toInt() ?? 5,
      autoBlockEnabled: json['autoBlockEnabled'] as bool? ?? true,
    );
  }
}

/// Configuração de Política de Auto-Corte RADIUS (`RadiusPolicyConfigResponse`).
class RadiusPolicyConfigModel {
  final String id;
  final bool autoBlockEnabled;
  final int toleranceDays;
  final String blockMode; // CAPTIVE_PORTAL, BANDWIDTH_REDUCTION, COMPLETE_DISCONNECT
  final int reducedDownloadKbps;
  final int reducedUploadKbps;
  final bool unblockOnPayment;
  final bool sendPodOnBlock;
  final bool sendPodOnUnblock;
  final int blockStartHour;
  final int blockEndHour;
  final bool allowBlockOnFriday;
  final bool protectEveOfHolidays;

  const RadiusPolicyConfigModel({
    required this.id,
    required this.autoBlockEnabled,
    required this.toleranceDays,
    required this.blockMode,
    required this.reducedDownloadKbps,
    required this.reducedUploadKbps,
    required this.unblockOnPayment,
    required this.sendPodOnBlock,
    required this.sendPodOnUnblock,
    required this.blockStartHour,
    required this.blockEndHour,
    required this.allowBlockOnFriday,
    required this.protectEveOfHolidays,
  });

  factory RadiusPolicyConfigModel.fromJson(Map<String, dynamic> json) {
    return RadiusPolicyConfigModel(
      id: json['id']?.toString() ?? '',
      autoBlockEnabled: json['autoBlockEnabled'] as bool? ?? true,
      toleranceDays: (json['toleranceDays'] as num?)?.toInt() ?? 5,
      blockMode: json['blockMode']?.toString() ?? 'CAPTIVE_PORTAL',
      reducedDownloadKbps: (json['reducedDownloadKbps'] as num?)?.toInt() ?? 1024,
      reducedUploadKbps: (json['reducedUploadKbps'] as num?)?.toInt() ?? 512,
      unblockOnPayment: json['unblockOnPayment'] as bool? ?? true,
      sendPodOnBlock: json['sendPodOnBlock'] as bool? ?? true,
      sendPodOnUnblock: json['sendPodOnUnblock'] as bool? ?? true,
      blockStartHour: (json['blockStartHour'] as num?)?.toInt() ?? 8,
      blockEndHour: (json['blockEndHour'] as num?)?.toInt() ?? 18,
      allowBlockOnFriday: json['allowBlockOnFriday'] as bool? ?? false,
      protectEveOfHolidays: json['protectEveOfHolidays'] as bool? ?? true,
    );
  }
}

/// Resultado de Ação Manual RADIUS (`RadiusManualActionResponse`).
class RadiusManualActionResultModel {
  final String contractId;
  final String username;
  final String actionApplied;
  final bool success;
  final String message;
  final String? podResult;

  const RadiusManualActionResultModel({
    required this.contractId,
    required this.username,
    required this.actionApplied,
    required this.success,
    required this.message,
    this.podResult,
  });

  factory RadiusManualActionResultModel.fromJson(Map<String, dynamic> json) {
    return RadiusManualActionResultModel(
      contractId: json['contractId']?.toString() ?? '',
      username: json['username']?.toString() ?? '',
      actionApplied: json['actionApplied']?.toString() ?? '',
      success: json['success'] as bool? ?? false,
      message: json['message']?.toString() ?? '',
      podResult: json['podResult']?.toString(),
    );
  }
}

/// Log de Ciclo de Vida RADIUS (`RadiusLifecycleLogResponse`).
class RadiusLifecycleLogModel {
  final String id;
  final String contractId;
  final String customerId;
  final String? customerName;
  final String username;
  final String actionType;
  final String? reason;
  final String? nasIp;
  final bool success;
  final String? details;
  final String createdAt;

  const RadiusLifecycleLogModel({
    required this.id,
    required this.contractId,
    required this.customerId,
    this.customerName,
    required this.username,
    required this.actionType,
    this.reason,
    this.nasIp,
    required this.success,
    this.details,
    required this.createdAt,
  });

  factory RadiusLifecycleLogModel.fromJson(Map<String, dynamic> json) {
    return RadiusLifecycleLogModel(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      customerId: json['customerId']?.toString() ?? '',
      customerName: json['customerName']?.toString(),
      username: json['username']?.toString() ?? '',
      actionType: json['actionType']?.toString() ?? '',
      reason: json['reason']?.toString(),
      nasIp: json['nasIp']?.toString(),
      success: json['success'] as bool? ?? true,
      details: json['details']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

/// Modelo de ASN IPAM (`IpamAsnResponse`).
class IpamAsnModel {
  final String id;
  final int asn;
  final String name;
  final String rir;
  final String? description;

  const IpamAsnModel({
    required this.id,
    required this.asn,
    required this.name,
    required this.rir,
    this.description,
  });

  factory IpamAsnModel.fromJson(Map<String, dynamic> json) {
    return IpamAsnModel(
      id: json['id']?.toString() ?? '',
      asn: (json['asn'] as num?)?.toInt() ?? 0,
      name: json['name']?.toString() ?? '',
      rir: json['rir']?.toString() ?? 'REGISTRO_BR',
      description: json['description']?.toString(),
    );
  }
}

/// Modelo de VRF IPAM (`IpamVrfResponse`).
class IpamVrfModel {
  final String id;
  final String name;
  final String? rd;
  final String? description;
  final bool isDefault;

  const IpamVrfModel({
    required this.id,
    required this.name,
    this.rd,
    this.description,
    required this.isDefault,
  });

  factory IpamVrfModel.fromJson(Map<String, dynamic> json) {
    return IpamVrfModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      rd: json['rd']?.toString(),
      description: json['description']?.toString(),
      isDefault: json['isDefault'] as bool? ?? false,
    );
  }
}

/// Modelo de Sub-rede IPAM (`IpamSubnetResponse`).
class IpamSubnetModel {
  final String id;
  final String? parentId;
  final String? vrfId;
  final String? vrfName;
  final String? asnId;
  final int? asnNumber;
  final String cidr;
  final String ipVersion; // IPV4, IPV6
  final String networkAddress;
  final String? broadcastAddress;
  final int prefixLength;
  final int totalHosts;
  final int allocatedHosts;
  final double utilizationPercentage;
  final bool isPool;
  final String? poolName;
  final String status; // ACTIVE, RESERVED, DEPRECATED
  final String category; // CUSTOMER_ACCESS, CGNAT, MANAGEMENT, INFRASTRUCTURE, PTP, LOOPBACK
  final String? description;

  const IpamSubnetModel({
    required this.id,
    this.parentId,
    this.vrfId,
    this.vrfName,
    this.asnId,
    this.asnNumber,
    required this.cidr,
    required this.ipVersion,
    required this.networkAddress,
    this.broadcastAddress,
    required this.prefixLength,
    required this.totalHosts,
    required this.allocatedHosts,
    required this.utilizationPercentage,
    required this.isPool,
    this.poolName,
    required this.status,
    required this.category,
    this.description,
  });

  factory IpamSubnetModel.fromJson(Map<String, dynamic> json) {
    return IpamSubnetModel(
      id: json['id']?.toString() ?? '',
      parentId: json['parentId']?.toString(),
      vrfId: json['vrfId']?.toString(),
      vrfName: json['vrfName']?.toString(),
      asnId: json['asnId']?.toString(),
      asnNumber: (json['asnNumber'] as num?)?.toInt(),
      cidr: json['cidr']?.toString() ?? '',
      ipVersion: json['ipVersion']?.toString() ?? 'IPV4',
      networkAddress: json['networkAddress']?.toString() ?? '',
      broadcastAddress: json['broadcastAddress']?.toString(),
      prefixLength: (json['prefixLength'] as num?)?.toInt() ?? 24,
      totalHosts: (json['totalHosts'] as num?)?.toInt() ?? 0,
      allocatedHosts: (json['allocatedHosts'] as num?)?.toInt() ?? 0,
      utilizationPercentage:
          (json['utilizationPercentage'] as num?)?.toDouble() ?? 0.0,
      isPool: json['isPool'] as bool? ?? false,
      poolName: json['poolName']?.toString(),
      status: json['status']?.toString() ?? 'ACTIVE',
      category: json['category']?.toString() ?? 'CUSTOMER_ACCESS',
      description: json['description']?.toString(),
    );
  }
}

/// Cálculo matemático de CIDR (`SubnetCalculationResult`).
class SubnetCalculationResultModel {
  final String cidr;
  final String ipVersion;
  final String networkAddress;
  final String? broadcastAddress;
  final String netmask;
  final String? wildcardMask;
  final String firstUsableIp;
  final String lastUsableIp;
  final int prefixLength;
  final int totalHosts;
  final int usableHosts;

  const SubnetCalculationResultModel({
    required this.cidr,
    required this.ipVersion,
    required this.networkAddress,
    this.broadcastAddress,
    required this.netmask,
    this.wildcardMask,
    required this.firstUsableIp,
    required this.lastUsableIp,
    required this.prefixLength,
    required this.totalHosts,
    required this.usableHosts,
  });

  factory SubnetCalculationResultModel.fromJson(Map<String, dynamic> json) {
    return SubnetCalculationResultModel(
      cidr: json['cidr']?.toString() ?? '',
      ipVersion: json['ipVersion']?.toString() ?? 'IPV4',
      networkAddress: json['networkAddress']?.toString() ?? '',
      broadcastAddress: json['broadcastAddress']?.toString(),
      netmask: json['netmask']?.toString() ?? '',
      wildcardMask: json['wildcardMask']?.toString(),
      firstUsableIp: json['firstUsableIp']?.toString() ?? '',
      lastUsableIp: json['lastUsableIp']?.toString() ?? '',
      prefixLength: (json['prefixLength'] as num?)?.toInt() ?? 24,
      totalHosts: (json['totalHosts'] as num?)?.toInt() ?? 0,
      usableHosts: (json['usableHosts'] as num?)?.toInt() ?? 0,
    );
  }
}

/// Resultado de Split VLSM (`IpamSplitResponse`).
class IpamSplitResponseModel {
  final String parentSubnetId;
  final String parentCidr;
  final int targetPrefixLength;
  final int totalSubnetsGenerated;
  final List<SubnetCalculationResultModel> generatedSubnets;
  final List<IpamSubnetModel> persistedSubnets;

  const IpamSplitResponseModel({
    required this.parentSubnetId,
    required this.parentCidr,
    required this.targetPrefixLength,
    required this.totalSubnetsGenerated,
    required this.generatedSubnets,
    required this.persistedSubnets,
  });

  factory IpamSplitResponseModel.fromJson(Map<String, dynamic> json) {
    final genRaw = json['generatedSubnets'];
    final persRaw = json['persistedSubnets'];
    return IpamSplitResponseModel(
      parentSubnetId: json['parentSubnetId']?.toString() ?? '',
      parentCidr: json['parentCidr']?.toString() ?? '',
      targetPrefixLength: (json['targetPrefixLength'] as num?)?.toInt() ?? 24,
      totalSubnetsGenerated:
          (json['totalSubnetsGenerated'] as num?)?.toInt() ?? 0,
      generatedSubnets: genRaw is List
          ? genRaw
              .map((e) => SubnetCalculationResultModel.fromJson(
                  Map<String, dynamic>.from(e as Map)))
              .toList()
          : <SubnetCalculationResultModel>[],
      persistedSubnets: persRaw is List
          ? persRaw
              .map((e) =>
                  IpamSubnetModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <IpamSubnetModel>[],
    );
  }
}

/// Endereço IP individual no IPAM (`IpamIpAddressResponse`).
class IpamIpAddressModel {
  final String id;
  final String subnetId;
  final String subnetCidr;
  final String ipAddress;
  final String status; // AVAILABLE, ALLOCATED, RESERVED, DHCP_POOL
  final String? assignedToType;
  final String? assignedToId;
  final String? assignedToLabel;
  final String? dnsName;
  final String? description;

  const IpamIpAddressModel({
    required this.id,
    required this.subnetId,
    required this.subnetCidr,
    required this.ipAddress,
    required this.status,
    this.assignedToType,
    this.assignedToId,
    this.assignedToLabel,
    this.dnsName,
    this.description,
  });

  factory IpamIpAddressModel.fromJson(Map<String, dynamic> json) {
    return IpamIpAddressModel(
      id: json['id']?.toString() ?? '',
      subnetId: json['subnetId']?.toString() ?? '',
      subnetCidr: json['subnetCidr']?.toString() ?? '',
      ipAddress: json['ipAddress']?.toString() ?? '',
      status: json['status']?.toString() ?? 'ALLOCATED',
      assignedToType: json['assignedToType']?.toString(),
      assignedToId: json['assignedToId']?.toString(),
      assignedToLabel: json['assignedToLabel']?.toString(),
      dnsName: json['dnsName']?.toString(),
      description: json['description']?.toString(),
    );
  }
}

/// Mapeamento CGNAT (`CgnatMappingResponse`).
class CgnatMappingModel {
  final String id;
  final String? nasId;
  final String? nasName;
  final String vendorType;
  final String publicIp;
  final int portStart;
  final int portEnd;
  final String privateIpStart;
  final String privateIpEnd;
  final String protocol;
  final String? notes;

  const CgnatMappingModel({
    required this.id,
    this.nasId,
    this.nasName,
    required this.vendorType,
    required this.publicIp,
    required this.portStart,
    required this.portEnd,
    required this.privateIpStart,
    required this.privateIpEnd,
    required this.protocol,
    this.notes,
  });

  factory CgnatMappingModel.fromJson(Map<String, dynamic> json) {
    return CgnatMappingModel(
      id: json['id']?.toString() ?? '',
      nasId: json['nasId']?.toString(),
      nasName: json['nasName']?.toString(),
      vendorType: json['vendorType']?.toString() ?? 'MIKROTIK',
      publicIp: json['publicIp']?.toString() ?? '',
      portStart: (json['portStart'] as num?)?.toInt() ?? 1024,
      portEnd: (json['portEnd'] as num?)?.toInt() ?? 2047,
      privateIpStart: json['privateIpStart']?.toString() ?? '',
      privateIpEnd: json['privateIpEnd']?.toString() ?? '',
      protocol: json['protocol']?.toString() ?? 'BOTH',
      notes: json['notes']?.toString(),
    );
  }
}

/// Resultado de Importação de Script CGNAT (`CgnatScriptImportResponse`).
class CgnatScriptImportResultModel {
  final int totalParsed;
  final int totalSaved;
  final List<CgnatMappingModel> importedMappings;
  final List<String> warnings;

  const CgnatScriptImportResultModel({
    required this.totalParsed,
    required this.totalSaved,
    required this.importedMappings,
    required this.warnings,
  });

  factory CgnatScriptImportResultModel.fromJson(Map<String, dynamic> json) {
    final mapRaw = json['importedMappings'];
    final warnRaw = json['warnings'];
    return CgnatScriptImportResultModel(
      totalParsed: (json['totalParsed'] as num?)?.toInt() ?? 0,
      totalSaved: (json['totalSaved'] as num?)?.toInt() ?? 0,
      importedMappings: mapRaw is List
          ? mapRaw
              .map((e) =>
                  CgnatMappingModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <CgnatMappingModel>[],
      warnings: warnRaw is List
          ? warnRaw.map((e) => e.toString()).toList()
          : <String>[],
    );
  }
}

/// Resultado de Busca Forense Marco Civil (`MarcoCivilSearchResult`).
class MarcoCivilSearchResultModel {
  final bool matched;
  final String queriedIp;
  final int? queriedPort;
  final String queriedTimestamp;
  final bool usedCgnat;
  final String? resolvedPrivateIp;
  final String? cgnatRuleSummary;
  final int? radacctId;
  final String? username;
  final String? callingStationId;
  final String? nasIpAddress;
  final String? sessionStartTime;
  final String? sessionStopTime;
  final String? contractId;
  final String? contractNumber;
  final String? customerId;
  final String? customerName;
  final String? customerCpfCnpj;
  final String? customerPhone;
  final String? customerEmail;
  final String? installationAddress;
  final String? planName;

  const MarcoCivilSearchResultModel({
    required this.matched,
    required this.queriedIp,
    this.queriedPort,
    required this.queriedTimestamp,
    required this.usedCgnat,
    this.resolvedPrivateIp,
    this.cgnatRuleSummary,
    this.radacctId,
    this.username,
    this.callingStationId,
    this.nasIpAddress,
    this.sessionStartTime,
    this.sessionStopTime,
    this.contractId,
    this.contractNumber,
    this.customerId,
    this.customerName,
    this.customerCpfCnpj,
    this.customerPhone,
    this.customerEmail,
    this.installationAddress,
    this.planName,
  });

  factory MarcoCivilSearchResultModel.fromJson(Map<String, dynamic> json) {
    return MarcoCivilSearchResultModel(
      matched: json['matched'] as bool? ?? false,
      queriedIp: json['queriedIp']?.toString() ?? '',
      queriedPort: (json['queriedPort'] as num?)?.toInt(),
      queriedTimestamp: json['queriedTimestamp']?.toString() ?? '',
      usedCgnat: json['usedCgnat'] as bool? ?? false,
      resolvedPrivateIp: json['resolvedPrivateIp']?.toString(),
      cgnatRuleSummary: json['cgnatRuleSummary']?.toString(),
      radacctId: (json['radacctId'] as num?)?.toInt(),
      username: json['username']?.toString(),
      callingStationId: json['callingStationId']?.toString(),
      nasIpAddress: json['nasIpAddress']?.toString(),
      sessionStartTime: json['sessionStartTime']?.toString(),
      sessionStopTime: json['sessionStopTime']?.toString(),
      contractId: json['contractId']?.toString(),
      contractNumber: json['contractNumber']?.toString(),
      customerId: json['customerId']?.toString(),
      customerName: json['customerName']?.toString(),
      customerCpfCnpj: json['customerCpfCnpj']?.toString(),
      customerPhone: json['customerPhone']?.toString(),
      customerEmail: json['customerEmail']?.toString(),
      installationAddress: json['installationAddress']?.toString(),
      planName: json['planName']?.toString(),
    );
  }
}

/// Laudo Pericial Oficial Marco Civil (`MarcoCivilReportResponse`).
class MarcoCivilReportModel {
  final String id;
  final String validationToken;
  final String sha256Hash;
  final String publicValidationUrl;
  final String qrCodePayload;
  final String? courtOrderNumber;
  final String? requesterAuthority;
  final String queriedIp;
  final int? queriedPort;
  final String queriedTimestamp;
  final String? matchedContractId;
  final String? matchedCustomerName;
  final String? matchedCpfCnpj;
  final String? matchedCallingStationId;
  final String? reportPdfUrl;
  final String createdAt;

  const MarcoCivilReportModel({
    required this.id,
    required this.validationToken,
    required this.sha256Hash,
    required this.publicValidationUrl,
    required this.qrCodePayload,
    this.courtOrderNumber,
    this.requesterAuthority,
    required this.queriedIp,
    this.queriedPort,
    required this.queriedTimestamp,
    this.matchedContractId,
    this.matchedCustomerName,
    this.matchedCpfCnpj,
    this.matchedCallingStationId,
    this.reportPdfUrl,
    required this.createdAt,
  });

  factory MarcoCivilReportModel.fromJson(Map<String, dynamic> json) {
    return MarcoCivilReportModel(
      id: json['id']?.toString() ?? '',
      validationToken: json['validationToken']?.toString() ?? '',
      sha256Hash: json['sha256Hash']?.toString() ?? '',
      publicValidationUrl: json['publicValidationUrl']?.toString() ?? '',
      qrCodePayload: json['qrCodePayload']?.toString() ?? '',
      courtOrderNumber: json['courtOrderNumber']?.toString(),
      requesterAuthority: json['requesterAuthority']?.toString(),
      queriedIp: json['queriedIp']?.toString() ?? '',
      queriedPort: (json['queriedPort'] as num?)?.toInt(),
      queriedTimestamp: json['queriedTimestamp']?.toString() ?? '',
      matchedContractId: json['matchedContractId']?.toString(),
      matchedCustomerName: json['matchedCustomerName']?.toString(),
      matchedCpfCnpj: json['matchedCpfCnpj']?.toString(),
      matchedCallingStationId: json['matchedCallingStationId']?.toString(),
      reportPdfUrl: json['reportPdfUrl']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

/// Resposta de Validação Pública de Laudo Marco Civil (`PublicValidationResponse`).
class PublicValidationResultModel {
  final bool valid;
  final String validationToken;
  final String sha256Hash;
  final String? courtOrderNumber;
  final String? requesterAuthority;
  final String queriedIp;
  final int? queriedPort;
  final String queriedTimestamp;
  final String? customerNameMasked;
  final String? customerCpfCnpjMasked;
  final String? callingStationId;
  final String reportIssuedAt;
  final String statusMessage;

  const PublicValidationResultModel({
    required this.valid,
    required this.validationToken,
    required this.sha256Hash,
    this.courtOrderNumber,
    this.requesterAuthority,
    required this.queriedIp,
    this.queriedPort,
    required this.queriedTimestamp,
    this.customerNameMasked,
    this.customerCpfCnpjMasked,
    this.callingStationId,
    required this.reportIssuedAt,
    required this.statusMessage,
  });

  factory PublicValidationResultModel.fromJson(Map<String, dynamic> json) {
    return PublicValidationResultModel(
      valid: json['valid'] as bool? ?? false,
      validationToken: json['validationToken']?.toString() ?? '',
      sha256Hash: json['sha256Hash']?.toString() ?? '',
      courtOrderNumber: json['courtOrderNumber']?.toString(),
      requesterAuthority: json['requesterAuthority']?.toString(),
      queriedIp: json['queriedIp']?.toString() ?? '',
      queriedPort: (json['queriedPort'] as num?)?.toInt(),
      queriedTimestamp: json['queriedTimestamp']?.toString() ?? '',
      customerNameMasked: json['customerNameMasked']?.toString(),
      customerCpfCnpjMasked: json['customerCpfCnpjMasked']?.toString(),
      callingStationId: json['callingStationId']?.toString(),
      reportIssuedAt: json['reportIssuedAt']?.toString() ?? '',
      statusMessage: json['statusMessage']?.toString() ?? '',
    );
  }
}

/// Repositório de RADIUS Sessões/Lifecycle, IPAM, CGNAT & Marco Civil (Onda B — Etapa 2).
class RadiusIpamMarcoCivilRepository {
  final ApiClient _apiClient;

  RadiusIpamMarcoCivilRepository(this._apiClient);

  // ==========================================
  // 1. Sessões RADIUS & Desconexão PoD
  // ==========================================

  Future<List<RadiusSessionModel>> getActiveSessions() async {
    try {
      final response = await _apiClient.dio.get('/radius/sessions/active');
      final list = response.data as List? ?? [];
      return list
          .map((e) =>
              RadiusSessionModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar sessões RADIUS ativas.'));
    }
  }

  Future<List<RadiusSessionModel>> getSessionHistory(String username) async {
    try {
      final response =
          await _apiClient.dio.get('/radius/sessions/history/$username');
      final list = response.data as List? ?? [];
      return list
          .map((e) =>
              RadiusSessionModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao consultar histórico da sessão.'));
    }
  }

  Future<RadiusDisconnectResultModel> disconnectSession({
    required String username,
    String? nasIpAddress,
    String? acctSessionId,
    String? framedIpAddress,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/radius/disconnect',
        data: {
          'username': username,
          'nasIpAddress': ?nasIpAddress,
          'acctSessionId': ?acctSessionId,
          'framedIpAddress': ?framedIpAddress,
        },
      );
      return RadiusDisconnectResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao enviar pacote PoD ao BNG.'));
    }
  }

  // ==========================================
  // 2. Ciclo de Vida RADIUS & Auto-Corte
  // ==========================================

  Future<RadiusLifecycleSummaryModel> getLifecycleSummary() async {
    try {
      final response = await _apiClient.dio.get('/radius/lifecycle/summary');
      return RadiusLifecycleSummaryModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao carregar resumo RADIUS Lifecycle.'));
    }
  }

  Future<RadiusPolicyConfigModel> getLifecyclePolicy() async {
    try {
      final response = await _apiClient.dio.get('/radius/lifecycle/policy');
      return RadiusPolicyConfigModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao carregar política de auto-corte.'));
    }
  }

  Future<RadiusPolicyConfigModel> updateLifecyclePolicy({
    required bool autoBlockEnabled,
    required int toleranceDays,
    required String blockMode,
    int reducedDownloadKbps = 1024,
    int reducedUploadKbps = 512,
    bool unblockOnPayment = true,
    bool sendPodOnBlock = true,
    bool sendPodOnUnblock = true,
    int blockStartHour = 8,
    int blockEndHour = 18,
    bool allowBlockOnFriday = false,
    bool protectEveOfHolidays = true,
  }) async {
    try {
      final response = await _apiClient.dio.put(
        '/radius/lifecycle/policy',
        data: {
          'autoBlockEnabled': autoBlockEnabled,
          'toleranceDays': toleranceDays,
          'blockMode': blockMode,
          'reducedDownloadKbps': reducedDownloadKbps,
          'reducedUploadKbps': reducedUploadKbps,
          'unblockOnPayment': unblockOnPayment,
          'sendPodOnBlock': sendPodOnBlock,
          'sendPodOnUnblock': sendPodOnUnblock,
          'blockStartHour': blockStartHour,
          'blockEndHour': blockEndHour,
          'allowBlockOnFriday': allowBlockOnFriday,
          'protectEveOfHolidays': protectEveOfHolidays,
        },
      );
      return RadiusPolicyConfigModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao atualizar política de auto-corte.'));
    }
  }

  Future<String> runAutoBlockNow() async {
    try {
      final response = await _apiClient.dio.post('/radius/lifecycle/run-now');
      final data = response.data;
      if (data is Map && data['message'] != null) {
        return data['message'].toString();
      }
      return 'Ciclo de auto-corte executado.';
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao disparar motor de auto-corte.'));
    }
  }

  Future<RadiusManualActionResultModel> executeManualAction({
    required String contractId,
    required String action,
    String? reason,
    String? vendorType,
    bool sendPod = true,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/radius/lifecycle/manual-action',
        data: {
          'contractId': contractId,
          'action': action,
          'reason': ?reason,
          'vendorType': ?vendorType,
          'sendPod': sendPod,
        },
      );
      return RadiusManualActionResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao executar ação manual RADIUS.'));
    }
  }

  Future<List<RadiusLifecycleLogModel>> getLifecycleLogs({
    int page = 0,
    int size = 25,
  }) async {
    try {
      final response = await _apiClient.dio.get(
        '/radius/lifecycle/logs',
        queryParameters: {'page': page, 'size': size},
      );
      final data = response.data;
      final list = data is Map ? (data['content'] as List? ?? []) : (data as List? ?? []);
      return list
          .map((e) => RadiusLifecycleLogModel.fromJson(
              Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao carregar logs do ciclo de vida RADIUS.'));
    }
  }

  // ==========================================
  // 3. IPAM (ASNs, VRFs, Sub-redes, Split VLSM & Inventário de IPs)
  // ==========================================

  Future<List<IpamAsnModel>> getAllAsns() async {
    try {
      final response = await _apiClient.dio.get('/ipam/asns');
      final list = response.data as List? ?? [];
      return list
          .map((e) => IpamAsnModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar ASNs do IPAM.'));
    }
  }

  Future<IpamAsnModel> createAsn({
    required int asn,
    required String name,
    String rir = 'REGISTRO_BR',
    String? description,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ipam/asns',
        data: {
          'asn': asn,
          'name': name,
          'rir': rir,
          'description': ?description,
        },
      );
      return IpamAsnModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar ASN.'));
    }
  }

  Future<List<IpamVrfModel>> getAllVrfs() async {
    try {
      final response = await _apiClient.dio.get('/ipam/vrfs');
      final list = response.data as List? ?? [];
      return list
          .map((e) => IpamVrfModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar VRFs do IPAM.'));
    }
  }

  Future<IpamVrfModel> createVrf({
    required String name,
    String? rd,
    String? description,
    bool isDefault = false,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ipam/vrfs',
        data: {
          'name': name,
          'rd': ?rd,
          'description': ?description,
          'isDefault': isDefault,
        },
      );
      return IpamVrfModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar VRF.'));
    }
  }

  Future<List<IpamSubnetModel>> getAllSubnets() async {
    try {
      final response = await _apiClient.dio.get('/ipam/subnets');
      final list = response.data as List? ?? [];
      return list
          .map((e) =>
              IpamSubnetModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar sub-redes IPAM.'));
    }
  }

  Future<IpamSubnetModel> createSubnet({
    required String cidr,
    String category = 'CUSTOMER_ACCESS',
    String status = 'ACTIVE',
    bool isPool = false,
    String? poolName,
    String? vrfId,
    String? asnId,
    String? description,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ipam/subnets',
        data: {
          'cidr': cidr,
          'category': category,
          'status': status,
          'isPool': isPool,
          'poolName': ?poolName,
          'vrfId': ?vrfId,
          'asnId': ?asnId,
          'description': ?description,
        },
      );
      return IpamSubnetModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar sub-rede IPAM.'));
    }
  }

  Future<SubnetCalculationResultModel> calculateCidr(String cidr) async {
    try {
      final response = await _apiClient.dio.get(
        '/ipam/calculate',
        queryParameters: {'cidr': cidr},
      );
      return SubnetCalculationResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao calcular CIDR.'));
    }
  }

  Future<IpamSplitResponseModel> splitSubnet({
    required String subnetId,
    required int targetPrefixLength,
    bool createSubnets = true,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ipam/subnets/split',
        data: {
          'subnetId': subnetId,
          'targetPrefixLength': targetPrefixLength,
          'createSubnets': createSubnets,
        },
      );
      return IpamSplitResponseModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao dividir sub-rede (Split VLSM).'));
    }
  }

  Future<List<IpamIpAddressModel>> getIpsBySubnet(String subnetId) async {
    try {
      final response =
          await _apiClient.dio.get('/ipam/subnets/$subnetId/ips');
      final list = response.data as List? ?? [];
      return list
          .map((e) =>
              IpamIpAddressModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar IPs da sub-rede.'));
    }
  }

  Future<String?> getNextAvailableIp(String subnetId) async {
    try {
      final response =
          await _apiClient.dio.get('/ipam/subnets/$subnetId/next-available');
      final data = response.data;
      if (data is Map) {
        return data['nextAvailableIp']?.toString();
      }
      return null;
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao localizar próximo IP livre.'));
    }
  }

  Future<IpamIpAddressModel> registerIp({
    required String subnetId,
    required String ipAddress,
    String status = 'ALLOCATED',
    String? assignedToType,
    String? dnsName,
    String? description,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ipam/ips',
        data: {
          'subnetId': subnetId,
          'ipAddress': ipAddress,
          'status': status,
          'assignedToType': ?assignedToType,
          'dnsName': ?dnsName,
          'description': ?description,
        },
      );
      return IpamIpAddressModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao registrar IP no IPAM.'));
    }
  }

  // ==========================================
  // 4. CGNAT Multi-Vendor & Marco Civil (Lei 12.965/2014)
  // ==========================================

  Future<List<CgnatMappingModel>> getAllCgnatMappings() async {
    try {
      final response = await _apiClient.dio.get('/cgnat/mappings');
      final list = response.data as List? ?? [];
      return list
          .map((e) =>
              CgnatMappingModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar mapeamentos CGNAT.'));
    }
  }

  Future<CgnatMappingModel> createCgnatMapping({
    String? nasId,
    String vendorType = 'MIKROTIK',
    required String publicIp,
    required int portStart,
    required int portEnd,
    required String privateIpStart,
    required String privateIpEnd,
    String protocol = 'BOTH',
    String? notes,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/cgnat/mappings',
        data: {
          'nasId': ?nasId,
          'vendorType': vendorType,
          'publicIp': publicIp,
          'portStart': portStart,
          'portEnd': portEnd,
          'privateIpStart': privateIpStart,
          'privateIpEnd': privateIpEnd,
          'protocol': protocol,
          'notes': ?notes,
        },
      );
      return CgnatMappingModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao criar regra CGNAT.'));
    }
  }

  Future<void> deleteCgnatMapping(String id) async {
    try {
      await _apiClient.dio.delete('/cgnat/mappings/$id');
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao remover mapeamento CGNAT.'));
    }
  }

  Future<CgnatScriptImportResultModel> importCgnatScript({
    String? nasId,
    String vendorType = 'MIKROTIK',
    required String scriptContent,
    bool replaceExisting = false,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/cgnat/import-script',
        data: {
          'nasId': ?nasId,
          'vendorType': vendorType,
          'scriptContent': scriptContent,
          'replaceExisting': replaceExisting,
        },
      );
      return CgnatScriptImportResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao importar script de CGNAT.'));
    }
  }

  Future<MarcoCivilSearchResultModel> searchSubscriber({
    required String ip,
    int? port,
    required String timestamp,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/marco-civil/search',
        data: {
          'ip': ip,
          'port': ?port,
          'timestamp': timestamp,
        },
      );
      return MarcoCivilSearchResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha na busca forense Marco Civil.'));
    }
  }

  Future<MarcoCivilReportModel> generateReport({
    String? courtOrderNumber,
    String? requesterAuthority,
    required String queriedIp,
    int? queriedPort,
    required String queriedTimestamp,
    String? matchedContractId,
    String? notes,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/marco-civil/reports',
        data: {
          'courtOrderNumber': ?courtOrderNumber,
          'requesterAuthority': ?requesterAuthority,
          'queriedIp': queriedIp,
          'queriedPort': ?queriedPort,
          'queriedTimestamp': queriedTimestamp,
          'matchedContractId': ?matchedContractId,
          'notes': ?notes,
        },
      );
      return MarcoCivilReportModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao emitir laudo pericial Marco Civil.'));
    }
  }

  Future<PublicValidationResultModel> validateReportToken(String token) async {
    try {
      final response =
          await _apiClient.dio.get('/public/marco-civil/validate/$token');
      return PublicValidationResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao validar token público do laudo.'));
    }
  }

  String _extractError(DioException e, String fallback) {
    final data = e.response?.data;
    if (data is Map) {
      return data['detail']?.toString() ??
          data['message']?.toString() ??
          fallback;
    }
    return fallback;
  }
}

final radiusIpamMarcoCivilRepositoryProvider =
    Provider<RadiusIpamMarcoCivilRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return RadiusIpamMarcoCivilRepository(apiClient);
});

final radiusActiveSessionsProvider =
    FutureProvider.autoDispose<List<RadiusSessionModel>>((ref) async {
  return ref.watch(radiusIpamMarcoCivilRepositoryProvider).getActiveSessions();
});

final radiusLifecycleSummaryProvider =
    FutureProvider.autoDispose<RadiusLifecycleSummaryModel>((ref) async {
  return ref
      .watch(radiusIpamMarcoCivilRepositoryProvider)
      .getLifecycleSummary();
});

final radiusLifecyclePolicyProvider =
    FutureProvider.autoDispose<RadiusPolicyConfigModel>((ref) async {
  return ref.watch(radiusIpamMarcoCivilRepositoryProvider).getLifecyclePolicy();
});

final radiusLifecycleLogsProvider =
    FutureProvider.autoDispose<List<RadiusLifecycleLogModel>>((ref) async {
  return ref.watch(radiusIpamMarcoCivilRepositoryProvider).getLifecycleLogs();
});

final ipamAsnsProvider =
    FutureProvider.autoDispose<List<IpamAsnModel>>((ref) async {
  return ref.watch(radiusIpamMarcoCivilRepositoryProvider).getAllAsns();
});

final ipamVrfsProvider =
    FutureProvider.autoDispose<List<IpamVrfModel>>((ref) async {
  return ref.watch(radiusIpamMarcoCivilRepositoryProvider).getAllVrfs();
});

final ipamSubnetsProvider =
    FutureProvider.autoDispose<List<IpamSubnetModel>>((ref) async {
  return ref.watch(radiusIpamMarcoCivilRepositoryProvider).getAllSubnets();
});

final cgnatMappingsProvider =
    FutureProvider.autoDispose<List<CgnatMappingModel>>((ref) async {
  return ref
      .watch(radiusIpamMarcoCivilRepositoryProvider)
      .getAllCgnatMappings();
});
