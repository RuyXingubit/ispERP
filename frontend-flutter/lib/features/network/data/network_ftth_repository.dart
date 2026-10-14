import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Modelo de Dispositivo de Rede (OLT / BNG / Concentrador) — `NetworkDeviceResponse`.
class NetworkDeviceModel {
  final String id;
  final String name;
  final String deviceType;
  final String driverType;
  final String ipAddress;
  final int apiPort;
  final bool active;
  final String? createdAt;
  final String? updatedAt;

  const NetworkDeviceModel({
    required this.id,
    required this.name,
    required this.deviceType,
    required this.driverType,
    required this.ipAddress,
    required this.apiPort,
    required this.active,
    this.createdAt,
    this.updatedAt,
  });

  factory NetworkDeviceModel.fromJson(Map<String, dynamic> json) {
    return NetworkDeviceModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      deviceType: json['deviceType']?.toString() ?? 'OLT',
      driverType: json['driverType']?.toString() ?? 'SMARTOLT',
      ipAddress: json['ipAddress']?.toString() ?? '',
      apiPort: (json['apiPort'] as num?)?.toInt() ?? 443,
      active: json['active'] as bool? ?? true,
      createdAt: json['createdAt']?.toString(),
      updatedAt: json['updatedAt']?.toString(),
    );
  }
}

/// Modelo de Provisionamento de ONU em OLT — `OnuResponse`.
class OnuModel {
  final String id;
  final String contractId;
  final String customerId;
  final String? networkDeviceId;
  final String onuMac;
  final String onuSerial;
  final int vlanId;
  final String? pppoeUser;
  final int downloadSpeed;
  final int uploadSpeed;
  final String status; // PROVISIONED, BLOCKED, DEPROVISIONED
  final double? rxPowerDbm;
  final String? lastSyncAt;
  final String? createdAt;

  const OnuModel({
    required this.id,
    required this.contractId,
    required this.customerId,
    this.networkDeviceId,
    required this.onuMac,
    required this.onuSerial,
    required this.vlanId,
    this.pppoeUser,
    required this.downloadSpeed,
    required this.uploadSpeed,
    required this.status,
    this.rxPowerDbm,
    this.lastSyncAt,
    this.createdAt,
  });

  factory OnuModel.fromJson(Map<String, dynamic> json) {
    return OnuModel(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      customerId: json['customerId']?.toString() ?? '',
      networkDeviceId: json['networkDeviceId']?.toString(),
      onuMac: json['onuMac']?.toString() ?? '',
      onuSerial: json['onuSerial']?.toString() ?? '',
      vlanId: (json['vlanId'] as num?)?.toInt() ?? 100,
      pppoeUser: json['pppoeUser']?.toString(),
      downloadSpeed: (json['downloadSpeed'] as num?)?.toInt() ?? 0,
      uploadSpeed: (json['uploadSpeed'] as num?)?.toInt() ?? 0,
      status: json['status']?.toString() ?? 'PROVISIONED',
      rxPowerDbm: (json['rxPowerDbm'] as num?)?.toDouble(),
      lastSyncAt: json['lastSyncAt']?.toString(),
      createdAt: json['createdAt']?.toString(),
    );
  }
}

/// Modelo de Diagnóstico e Telemetria Óptica da ONU — `OnuStatusResponse`.
class OnuDiagnosticsModel {
  final String onuMac;
  final String onuSerial;
  final String status;
  final double? rxPowerDbm;
  final double? txPowerDbm;
  final String? oltName;
  final int? ponPort;
  final String? details;

  const OnuDiagnosticsModel({
    required this.onuMac,
    required this.onuSerial,
    required this.status,
    this.rxPowerDbm,
    this.txPowerDbm,
    this.oltName,
    this.ponPort,
    this.details,
  });

  factory OnuDiagnosticsModel.fromJson(Map<String, dynamic> json) {
    return OnuDiagnosticsModel(
      onuMac: json['onuMac']?.toString() ?? '',
      onuSerial: json['onuSerial']?.toString() ?? '',
      status: json['status']?.toString() ?? 'UNKNOWN',
      rxPowerDbm: (json['rxPowerDbm'] as num?)?.toDouble(),
      txPowerDbm: (json['txPowerDbm'] as num?)?.toDouble(),
      oltName: json['oltName']?.toString(),
      ponPort: (json['ponPort'] as num?)?.toInt(),
      details: json['details']?.toString(),
    );
  }
}

/// Modelo de Porta PON monitorada — `OltPonPortResponse`.
class OltPonPortModel {
  final String id;
  final String networkDeviceId;
  final String? oltName;
  final int slotNumber;
  final int portNumber;
  final String ponName;
  final String adminStatus;
  final String operStatus;
  final double txPowerDbm;
  final double temperatureCelsius;
  final int totalOnus;
  final int onlineOnus;
  final int losOnus;
  final int dyingGaspOnus;
  final int offlineOnus;
  final double healthPercentage;
  final String? connectedCableId;
  final String? connectedCableName;
  final String? lastPolledAt;

  const OltPonPortModel({
    required this.id,
    required this.networkDeviceId,
    this.oltName,
    required this.slotNumber,
    required this.portNumber,
    required this.ponName,
    required this.adminStatus,
    required this.operStatus,
    required this.txPowerDbm,
    required this.temperatureCelsius,
    required this.totalOnus,
    required this.onlineOnus,
    required this.losOnus,
    required this.dyingGaspOnus,
    required this.offlineOnus,
    required this.healthPercentage,
    this.connectedCableId,
    this.connectedCableName,
    this.lastPolledAt,
  });

  factory OltPonPortModel.fromJson(Map<String, dynamic> json) {
    return OltPonPortModel(
      id: json['id']?.toString() ?? '',
      networkDeviceId: json['networkDeviceId']?.toString() ?? '',
      oltName: json['oltName']?.toString(),
      slotNumber: (json['slotNumber'] as num?)?.toInt() ?? 0,
      portNumber: (json['portNumber'] as num?)?.toInt() ?? 1,
      ponName: json['ponName']?.toString() ?? '',
      adminStatus: json['adminStatus']?.toString() ?? 'UP',
      operStatus: json['operStatus']?.toString() ?? 'UP',
      txPowerDbm: (json['txPowerDbm'] as num?)?.toDouble() ?? 0.0,
      temperatureCelsius: (json['temperatureCelsius'] as num?)?.toDouble() ?? 0.0,
      totalOnus: (json['totalOnus'] as num?)?.toInt() ?? 0,
      onlineOnus: (json['onlineOnus'] as num?)?.toInt() ?? 0,
      losOnus: (json['losOnus'] as num?)?.toInt() ?? 0,
      dyingGaspOnus: (json['dyingGaspOnus'] as num?)?.toInt() ?? 0,
      offlineOnus: (json['offlineOnus'] as num?)?.toInt() ?? 0,
      healthPercentage: (json['healthPercentage'] as num?)?.toDouble() ?? 100.0,
      connectedCableId: json['connectedCableId']?.toString(),
      connectedCableName: json['connectedCableName']?.toString(),
      lastPolledAt: json['lastPolledAt']?.toString(),
    );
  }
}

/// Modelo de Incidente FTTH do NOC — `FtthIncidentResponse`.
class FtthIncidentModel {
  final String id;
  final String? networkDeviceId;
  final String? oltName;
  final String? oltPonPortId;
  final String? ponName;
  final String incidentType;
  final String incidentTypeDescription;
  final String severity; // CRITICAL, MAJOR, WARNING, INFO
  final String status; // ACTIVE, INVESTIGATING, DISPATCHED, RESOLVED
  final String title;
  final String? description;
  final int affectedCustomersCount;
  final List<String> affectedCtoNames;
  final String? affectedCableName;
  final double? estimatedCutLatitude;
  final double? estimatedCutLongitude;
  final String? estimatedCutDetails;
  final String? workOrderProtocol;
  final String detectedAt;
  final String? dispatchedAt;
  final String? resolvedAt;
  final String? rootCauseNotes;

  const FtthIncidentModel({
    required this.id,
    this.networkDeviceId,
    this.oltName,
    this.oltPonPortId,
    this.ponName,
    required this.incidentType,
    required this.incidentTypeDescription,
    required this.severity,
    required this.status,
    required this.title,
    this.description,
    required this.affectedCustomersCount,
    required this.affectedCtoNames,
    this.affectedCableName,
    this.estimatedCutLatitude,
    this.estimatedCutLongitude,
    this.estimatedCutDetails,
    this.workOrderProtocol,
    required this.detectedAt,
    this.dispatchedAt,
    this.resolvedAt,
    this.rootCauseNotes,
  });

  factory FtthIncidentModel.fromJson(Map<String, dynamic> json) {
    final ctoNamesRaw = json['affectedCtoNames'];
    final ctoNames = ctoNamesRaw is List
        ? ctoNamesRaw.map((e) => e.toString()).toList()
        : <String>[];
    return FtthIncidentModel(
      id: json['id']?.toString() ?? '',
      networkDeviceId: json['networkDeviceId']?.toString(),
      oltName: json['oltName']?.toString(),
      oltPonPortId: json['oltPonPortId']?.toString(),
      ponName: json['ponName']?.toString(),
      incidentType: json['incidentType']?.toString() ?? 'MASSIVE_LOS_PON',
      incidentTypeDescription: json['incidentTypeDescription']?.toString() ?? '',
      severity: json['severity']?.toString() ?? 'CRITICAL',
      status: json['status']?.toString() ?? 'ACTIVE',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString(),
      affectedCustomersCount: (json['affectedCustomersCount'] as num?)?.toInt() ?? 0,
      affectedCtoNames: ctoNames,
      affectedCableName: json['affectedCableName']?.toString(),
      estimatedCutLatitude: (json['estimatedCutLatitude'] as num?)?.toDouble(),
      estimatedCutLongitude: (json['estimatedCutLongitude'] as num?)?.toDouble(),
      estimatedCutDetails: json['estimatedCutDetails']?.toString(),
      workOrderProtocol: json['workOrderProtocol']?.toString(),
      detectedAt: json['detectedAt']?.toString() ?? '',
      dispatchedAt: json['dispatchedAt']?.toString(),
      resolvedAt: json['resolvedAt']?.toString(),
      rootCauseNotes: json['rootCauseNotes']?.toString(),
    );
  }
}

/// Resumo Executivo de Monitoramento do NOC — `NocMonitoringSummaryResponse`.
class NocMonitoringSummaryModel {
  final int totalOlts;
  final int totalPonPorts;
  final int activePonPorts;
  final int totalOnus;
  final int onlineOnus;
  final int losOnus;
  final int dyingGaspOnus;
  final int offlineOnus;
  final double globalHealthPercentage;
  final int activeIncidentsCount;
  final int criticalIncidentsCount;
  final List<FtthIncidentModel> activeIncidents;

  const NocMonitoringSummaryModel({
    required this.totalOlts,
    required this.totalPonPorts,
    required this.activePonPorts,
    required this.totalOnus,
    required this.onlineOnus,
    required this.losOnus,
    required this.dyingGaspOnus,
    required this.offlineOnus,
    required this.globalHealthPercentage,
    required this.activeIncidentsCount,
    required this.criticalIncidentsCount,
    required this.activeIncidents,
  });

  factory NocMonitoringSummaryModel.fromJson(Map<String, dynamic> json) {
    final incidentsRaw = json['activeIncidents'];
    final incidents = incidentsRaw is List
        ? incidentsRaw
            .map((e) => FtthIncidentModel.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList()
        : <FtthIncidentModel>[];
    return NocMonitoringSummaryModel(
      totalOlts: (json['totalOlts'] as num?)?.toInt() ?? 0,
      totalPonPorts: (json['totalPonPorts'] as num?)?.toInt() ?? 0,
      activePonPorts: (json['activePonPorts'] as num?)?.toInt() ?? 0,
      totalOnus: (json['totalOnus'] as num?)?.toInt() ?? 0,
      onlineOnus: (json['onlineOnus'] as num?)?.toInt() ?? 0,
      losOnus: (json['losOnus'] as num?)?.toInt() ?? 0,
      dyingGaspOnus: (json['dyingGaspOnus'] as num?)?.toInt() ?? 0,
      offlineOnus: (json['offlineOnus'] as num?)?.toInt() ?? 0,
      globalHealthPercentage: (json['globalHealthPercentage'] as num?)?.toDouble() ?? 100.0,
      activeIncidentsCount: (json['activeIncidentsCount'] as num?)?.toInt() ?? 0,
      criticalIncidentsCount: (json['criticalIncidentsCount'] as num?)?.toInt() ?? 0,
      activeIncidents: incidents,
    );
  }
}

/// Modelo de POP / Central Óptica — `FtthPopResponse`.
class FtthPopModel {
  final String id;
  final String name;
  final double? latitude;
  final double? longitude;
  final String? address;
  final String? description;

  const FtthPopModel({
    required this.id,
    required this.name,
    this.latitude,
    this.longitude,
    this.address,
    this.description,
  });

  factory FtthPopModel.fromJson(Map<String, dynamic> json) {
    return FtthPopModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      address: json['address']?.toString(),
      description: json['description']?.toString(),
    );
  }
}

/// Modelo de Poste Georreferenciado — `FtthPoleResponse`.
class FtthPoleModel {
  final String id;
  final String code;
  final double latitude;
  final double longitude;
  final String poleType;
  final int reservationMeters;
  final String? description;

  const FtthPoleModel({
    required this.id,
    required this.code,
    required this.latitude,
    required this.longitude,
    required this.poleType,
    required this.reservationMeters,
    this.description,
  });

  factory FtthPoleModel.fromJson(Map<String, dynamic> json) {
    return FtthPoleModel(
      id: json['id']?.toString() ?? '',
      code: json['code']?.toString() ?? '',
      latitude: (json['latitude'] as num?)?.toDouble() ?? 0.0,
      longitude: (json['longitude'] as num?)?.toDouble() ?? 0.0,
      poleType: json['poleType']?.toString() ?? 'CONCRETO',
      reservationMeters: (json['reservationMeters'] as num?)?.toInt() ?? 0,
      description: json['description']?.toString(),
    );
  }
}

/// Codificação de cor ABNT NBR 14106 / TIA-EIA-598 de uma fibra — `FiberColorInfo`.
class FiberColorInfoModel {
  final int fiberNumber;
  final int tubeNumber;
  final int fiberInTubeNumber;
  final String fiberColorName;
  final String fiberColorHex;
  final String tubeColorName;
  final String tubeColorHex;
  final String standard;

  const FiberColorInfoModel({
    required this.fiberNumber,
    required this.tubeNumber,
    required this.fiberInTubeNumber,
    required this.fiberColorName,
    required this.fiberColorHex,
    required this.tubeColorName,
    required this.tubeColorHex,
    required this.standard,
  });

  factory FiberColorInfoModel.fromJson(Map<String, dynamic> json) {
    return FiberColorInfoModel(
      fiberNumber: (json['fiberNumber'] as num?)?.toInt() ?? 1,
      tubeNumber: (json['tubeNumber'] as num?)?.toInt() ?? 1,
      fiberInTubeNumber: (json['fiberInTubeNumber'] as num?)?.toInt() ?? 1,
      fiberColorName: json['fiberColorName']?.toString() ?? '',
      fiberColorHex: json['fiberColorHex']?.toString() ?? '#00AA00',
      tubeColorName: json['tubeColorName']?.toString() ?? '',
      tubeColorHex: json['tubeColorHex']?.toString() ?? '#00AA00',
      standard: json['standard']?.toString() ?? 'ABNT_NBR_14106',
    );
  }
}

/// Modelo de Cabo Óptico — `FtthCableResponse`.
class FtthCableModel {
  final String id;
  final String name;
  final String cableType; // ALIMENTADOR, DISTRIBUICAO, DROP
  final int fiberCount;
  final int tubeCount;
  final String colorStandard; // ABNT_NBR_14106, TIA_EIA_598
  final double lengthMeters;
  final double attenuationDbPerKm;
  final String? sourcePopId;
  final String? sourcePoleId;
  final String? targetPoleId;
  final List<FiberColorInfoModel> fibers;

  const FtthCableModel({
    required this.id,
    required this.name,
    required this.cableType,
    required this.fiberCount,
    required this.tubeCount,
    required this.colorStandard,
    required this.lengthMeters,
    required this.attenuationDbPerKm,
    this.sourcePopId,
    this.sourcePoleId,
    this.targetPoleId,
    required this.fibers,
  });

  factory FtthCableModel.fromJson(Map<String, dynamic> json) {
    final fibersRaw = json['fibers'];
    final fibers = fibersRaw is List
        ? fibersRaw
            .map((e) => FiberColorInfoModel.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList()
        : <FiberColorInfoModel>[];
    return FtthCableModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      cableType: json['cableType']?.toString() ?? 'ALIMENTADOR',
      fiberCount: (json['fiberCount'] as num?)?.toInt() ?? 12,
      tubeCount: (json['tubeCount'] as num?)?.toInt() ?? 1,
      colorStandard: json['colorStandard']?.toString() ?? 'ABNT_NBR_14106',
      lengthMeters: (json['lengthMeters'] as num?)?.toDouble() ?? 0.0,
      attenuationDbPerKm: (json['attenuationDbPerKm'] as num?)?.toDouble() ?? 0.35,
      sourcePopId: json['sourcePopId']?.toString(),
      sourcePoleId: json['sourcePoleId']?.toString(),
      targetPoleId: json['targetPoleId']?.toString(),
      fibers: fibers,
    );
  }
}

/// Modelo de Caixa de Emenda Óptica (CEO) — `FtthClosureResponse`.
class FtthClosureModel {
  final String id;
  final String name;
  final String? poleId;
  final String? poleCode;
  final double? latitude;
  final double? longitude;
  final String closureType; // DOMO, RETANGULAR, SUBTERRANEA
  final int trayCount;
  final int capacityFusions;
  final int usedFusionsCount;
  final String status;

  const FtthClosureModel({
    required this.id,
    required this.name,
    this.poleId,
    this.poleCode,
    this.latitude,
    this.longitude,
    required this.closureType,
    required this.trayCount,
    required this.capacityFusions,
    required this.usedFusionsCount,
    required this.status,
  });

  factory FtthClosureModel.fromJson(Map<String, dynamic> json) {
    return FtthClosureModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      poleId: json['poleId']?.toString(),
      poleCode: json['poleCode']?.toString(),
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      closureType: json['closureType']?.toString() ?? 'DOMO',
      trayCount: (json['trayCount'] as num?)?.toInt() ?? 4,
      capacityFusions: (json['capacityFusions'] as num?)?.toInt() ?? 48,
      usedFusionsCount: (json['usedFusionsCount'] as num?)?.toInt() ?? 0,
      status: json['status']?.toString() ?? 'ATIVA',
    );
  }
}

/// Modelo de Splitter Óptico — `FtthSplitterResponse`.
class FtthSplitterModel {
  final String id;
  final String? closureId;
  final String name;
  final String splitterType;
  final String? inputCableId;
  final int? inputFiberNumber;
  final double attenuationDb;
  final int outputPorts;

  const FtthSplitterModel({
    required this.id,
    this.closureId,
    required this.name,
    required this.splitterType,
    this.inputCableId,
    this.inputFiberNumber,
    required this.attenuationDb,
    required this.outputPorts,
  });

  factory FtthSplitterModel.fromJson(Map<String, dynamic> json) {
    return FtthSplitterModel(
      id: json['id']?.toString() ?? '',
      closureId: json['closureId']?.toString(),
      name: json['name']?.toString() ?? '',
      splitterType: json['splitterType']?.toString() ?? 'BALANCED_1_8',
      inputCableId: json['inputCableId']?.toString(),
      inputFiberNumber: (json['inputFiberNumber'] as num?)?.toInt(),
      attenuationDb: (json['attenuationDb'] as num?)?.toDouble() ?? 10.5,
      outputPorts: (json['outputPorts'] as num?)?.toInt() ?? 8,
    );
  }
}

/// Modelo de Fusão Óptica — `FtthFusionResponse`.
class FtthFusionModel {
  final String id;
  final String closureId;
  final int trayNumber;
  final String sourceCableId;
  final String sourceCableName;
  final int sourceFiberNumber;
  final FiberColorInfoModel? sourceFiberColor;
  final String? targetCableId;
  final String? targetCableName;
  final int? targetFiberNumber;
  final FiberColorInfoModel? targetFiberColor;
  final String? targetSplitterId;
  final String? targetSplitterName;
  final String? targetCtoId;
  final String? targetCtoName;
  final double lossDb;
  final String? description;

  const FtthFusionModel({
    required this.id,
    required this.closureId,
    required this.trayNumber,
    required this.sourceCableId,
    required this.sourceCableName,
    required this.sourceFiberNumber,
    this.sourceFiberColor,
    this.targetCableId,
    this.targetCableName,
    this.targetFiberNumber,
    this.targetFiberColor,
    this.targetSplitterId,
    this.targetSplitterName,
    this.targetCtoId,
    this.targetCtoName,
    required this.lossDb,
    this.description,
  });

  factory FtthFusionModel.fromJson(Map<String, dynamic> json) {
    return FtthFusionModel(
      id: json['id']?.toString() ?? '',
      closureId: json['closureId']?.toString() ?? '',
      trayNumber: (json['trayNumber'] as num?)?.toInt() ?? 1,
      sourceCableId: json['sourceCableId']?.toString() ?? '',
      sourceCableName: json['sourceCableName']?.toString() ?? '',
      sourceFiberNumber: (json['sourceFiberNumber'] as num?)?.toInt() ?? 1,
      sourceFiberColor: json['sourceFiberColor'] is Map
          ? FiberColorInfoModel.fromJson(
              Map<String, dynamic>.from(json['sourceFiberColor'] as Map))
          : null,
      targetCableId: json['targetCableId']?.toString(),
      targetCableName: json['targetCableName']?.toString(),
      targetFiberNumber: (json['targetFiberNumber'] as num?)?.toInt(),
      targetFiberColor: json['targetFiberColor'] is Map
          ? FiberColorInfoModel.fromJson(
              Map<String, dynamic>.from(json['targetFiberColor'] as Map))
          : null,
      targetSplitterId: json['targetSplitterId']?.toString(),
      targetSplitterName: json['targetSplitterName']?.toString(),
      targetCtoId: json['targetCtoId']?.toString(),
      targetCtoName: json['targetCtoName']?.toString(),
      lossDb: (json['lossDb'] as num?)?.toDouble() ?? 0.05,
      description: json['description']?.toString(),
    );
  }
}

/// Modelo de Porta de CTO — `FtthCtoPortResponse`.
class FtthCtoPortModel {
  final String id;
  final String ctoId;
  final int portNumber;
  final String status; // LIVRE, OCUPADA, RESERVADA, DEFEITO
  final String? onuSerial;
  final String? onuMac;
  final String? customerName;
  final String? pppoeUser;
  final String? notes;

  const FtthCtoPortModel({
    required this.id,
    required this.ctoId,
    required this.portNumber,
    required this.status,
    this.onuSerial,
    this.onuMac,
    this.customerName,
    this.pppoeUser,
    this.notes,
  });

  factory FtthCtoPortModel.fromJson(Map<String, dynamic> json) {
    return FtthCtoPortModel(
      id: json['id']?.toString() ?? '',
      ctoId: json['ctoId']?.toString() ?? '',
      portNumber: (json['portNumber'] as num?)?.toInt() ?? 1,
      status: json['status']?.toString() ?? 'LIVRE',
      onuSerial: json['onuSerial']?.toString(),
      onuMac: json['onuMac']?.toString(),
      customerName: json['customerName']?.toString(),
      pppoeUser: json['pppoeUser']?.toString(),
      notes: json['notes']?.toString(),
    );
  }
}

/// Modelo de Caixa de Atendimento ao Cliente (CTO) — `FtthCtoResponse`.
class FtthCtoModel {
  final String id;
  final String name;
  final String? poleId;
  final String? poleCode;
  final String? closureId;
  final double latitude;
  final double longitude;
  final int totalPorts;
  final int freePortsCount;
  final int occupiedPortsCount;
  final double occupancyPercentage;
  final String splitterType;
  final String status;
  final String? description;
  final List<FtthCtoPortModel> ports;

  const FtthCtoModel({
    required this.id,
    required this.name,
    this.poleId,
    this.poleCode,
    this.closureId,
    required this.latitude,
    required this.longitude,
    required this.totalPorts,
    required this.freePortsCount,
    required this.occupiedPortsCount,
    required this.occupancyPercentage,
    required this.splitterType,
    required this.status,
    this.description,
    required this.ports,
  });

  factory FtthCtoModel.fromJson(Map<String, dynamic> json) {
    final portsRaw = json['ports'];
    final ports = portsRaw is List
        ? portsRaw
            .map((e) => FtthCtoPortModel.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList()
        : <FtthCtoPortModel>[];
    return FtthCtoModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      poleId: json['poleId']?.toString(),
      poleCode: json['poleCode']?.toString(),
      closureId: json['closureId']?.toString(),
      latitude: (json['latitude'] as num?)?.toDouble() ?? 0.0,
      longitude: (json['longitude'] as num?)?.toDouble() ?? 0.0,
      totalPorts: (json['totalPorts'] as num?)?.toInt() ?? 16,
      freePortsCount: (json['freePortsCount'] as num?)?.toInt() ?? 16,
      occupiedPortsCount: (json['occupiedPortsCount'] as num?)?.toInt() ?? 0,
      occupancyPercentage: (json['occupancyPercentage'] as num?)?.toDouble() ?? 0.0,
      splitterType: json['splitterType']?.toString() ?? 'BALANCED_1_16',
      status: json['status']?.toString() ?? 'ATIVA',
      description: json['description']?.toString(),
      ports: ports,
    );
  }
}

/// Diagrama Unifilar Interno da Caixa de Emenda (CEO) — `FtthClosureDiagramResponse`.
class FtthClosureDiagramModel {
  final FtthClosureModel closure;
  final List<FtthCableModel> cables;
  final List<FtthSplitterModel> splitters;
  final List<FtthFusionModel> fusions;
  final List<FtthCtoModel> connectedCtos;

  const FtthClosureDiagramModel({
    required this.closure,
    required this.cables,
    required this.splitters,
    required this.fusions,
    required this.connectedCtos,
  });

  factory FtthClosureDiagramModel.fromJson(Map<String, dynamic> json) {
    final cablesRaw = json['cables'];
    final splittersRaw = json['splitters'];
    final fusionsRaw = json['fusions'];
    final ctosRaw = json['connectedCtos'];
    return FtthClosureDiagramModel(
      closure: FtthClosureModel.fromJson(
          Map<String, dynamic>.from((json['closure'] as Map?) ?? {})),
      cables: cablesRaw is List
          ? cablesRaw
              .map((e) => FtthCableModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <FtthCableModel>[],
      splitters: splittersRaw is List
          ? splittersRaw
              .map((e) => FtthSplitterModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <FtthSplitterModel>[],
      fusions: fusionsRaw is List
          ? fusionsRaw
              .map((e) => FtthFusionModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <FtthFusionModel>[],
      connectedCtos: ctosRaw is List
          ? ctosRaw
              .map((e) => FtthCtoModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <FtthCtoModel>[],
    );
  }
}

/// Item de CTO Viável — `FeasibleCtoItem`.
class FeasibleCtoItemModel {
  final FtthCtoModel cto;
  final double distanceMeters;
  final int freePorts;
  final bool hasCapacity;

  const FeasibleCtoItemModel({
    required this.cto,
    required this.distanceMeters,
    required this.freePorts,
    required this.hasCapacity,
  });

  factory FeasibleCtoItemModel.fromJson(Map<String, dynamic> json) {
    return FeasibleCtoItemModel(
      cto: FtthCtoModel.fromJson(
          Map<String, dynamic>.from((json['cto'] as Map?) ?? {})),
      distanceMeters: (json['distanceMeters'] as num?)?.toDouble() ?? 0.0,
      freePorts: (json['freePorts'] as num?)?.toInt() ?? 0,
      hasCapacity: json['hasCapacity'] as bool? ?? false,
    );
  }
}

/// Resultado de Viabilidade Técnica FTTH — `FtthFeasibilityResponse`.
class FtthFeasibilityResultModel {
  final bool viable;
  final int viableCtosCount;
  final List<FeasibleCtoItemModel> nearbyCtos;

  const FtthFeasibilityResultModel({
    required this.viable,
    required this.viableCtosCount,
    required this.nearbyCtos,
  });

  factory FtthFeasibilityResultModel.fromJson(Map<String, dynamic> json) {
    final raw = json['nearbyCtos'];
    return FtthFeasibilityResultModel(
      viable: json['viable'] as bool? ?? false,
      viableCtosCount: (json['viableCtosCount'] as num?)?.toInt() ?? 0,
      nearbyCtos: raw is List
          ? raw
              .map((e) => FeasibleCtoItemModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <FeasibleCtoItemModel>[],
    );
  }
}

/// Nó do Trajeto Óptico (LightPath) — `LightPathNode`.
class LightPathNodeModel {
  final String elementType;
  final String name;
  final String? details;
  final double addedAttenuationDb;
  final double cumulativeAttenuationDb;

  const LightPathNodeModel({
    required this.elementType,
    required this.name,
    this.details,
    required this.addedAttenuationDb,
    required this.cumulativeAttenuationDb,
  });

  factory LightPathNodeModel.fromJson(Map<String, dynamic> json) {
    return LightPathNodeModel(
      elementType: json['elementType']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      details: json['details']?.toString(),
      addedAttenuationDb: (json['addedAttenuationDb'] as num?)?.toDouble() ?? 0.0,
      cumulativeAttenuationDb:
          (json['cumulativeAttenuationDb'] as num?)?.toDouble() ?? 0.0,
    );
  }
}

/// Resultado do Rastreamento Óptico (LightPath) — `LightPathTraceResult`.
class LightPathTraceResultModel {
  final bool reachedSource;
  final String? sourcePopName;
  final double totalAttenuationDb;
  final double estimatedRxPowerDbm;
  final List<LightPathNodeModel> nodes;

  const LightPathTraceResultModel({
    required this.reachedSource,
    this.sourcePopName,
    required this.totalAttenuationDb,
    required this.estimatedRxPowerDbm,
    required this.nodes,
  });

  factory LightPathTraceResultModel.fromJson(Map<String, dynamic> json) {
    final nodesRaw = json['nodes'];
    return LightPathTraceResultModel(
      reachedSource: json['reachedSource'] as bool? ?? false,
      sourcePopName: json['sourcePopName']?.toString(),
      totalAttenuationDb: (json['totalAttenuationDb'] as num?)?.toDouble() ?? 0.0,
      estimatedRxPowerDbm: (json['estimatedRxPowerDbm'] as num?)?.toDouble() ?? 0.0,
      nodes: nodesRaw is List
          ? nodesRaw
              .map((e) => LightPathNodeModel.fromJson(Map<String, dynamic>.from(e as Map)))
              .toList()
          : <LightPathNodeModel>[],
    );
  }
}

/// Repositório de Engenharia de Rede, OLTs, ONUs, Monitoramento NOC e Topologia FTTH (Onda B — Etapa 1).
class NetworkFtthRepository {
  final ApiClient _apiClient;

  NetworkFtthRepository(this._apiClient);

  // ==========================================
  // 1. Dispositivos de Rede (OLTs / BNGs) & ONUs
  // ==========================================

  Future<List<NetworkDeviceModel>> getAllDevices() async {
    try {
      final response = await _apiClient.dio.get('/network-devices');
      final list = response.data as List? ?? [];
      return list
          .map((e) => NetworkDeviceModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar OLTs e dispositivos de rede.'));
    }
  }

  Future<NetworkDeviceModel> createDevice({
    required String name,
    String deviceType = 'OLT',
    required String driverType,
    required String ipAddress,
    int apiPort = 443,
    String? apiToken,
    String? snmpCommunity,
    bool active = true,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/network-devices',
        data: {
          'name': name,
          'deviceType': deviceType,
          'driverType': driverType,
          'ipAddress': ipAddress,
          'apiPort': apiPort,
          if (apiToken != null && apiToken.trim().isNotEmpty)
            'apiToken': apiToken.trim(),
          if (snmpCommunity != null && snmpCommunity.trim().isNotEmpty)
            'snmpCommunity': snmpCommunity.trim(),
          'active': active,
        },
      );
      return NetworkDeviceModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar dispositivo de rede.'));
    }
  }

  Future<List<OnuModel>> getAllOnus() async {
    try {
      final response = await _apiClient.dio.get('/onus');
      final list = response.data as List? ?? [];
      return list
          .map((e) => OnuModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar ONUs provisionadas.'));
    }
  }

  Future<OnuDiagnosticsModel> diagnoseOnu(String onuId) async {
    try {
      final response = await _apiClient.dio.get('/onus/$onuId/diagnose');
      return OnuDiagnosticsModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao diagnosticar telemetria da ONU.'));
    }
  }

  Future<OnuModel> blockOnuByContract(String contractId,
      {String reason = 'Inadimplência'}) async {
    try {
      final response = await _apiClient.dio.post(
        '/onus/contract/$contractId/block',
        queryParameters: {'reason': reason},
      );
      return OnuModel.fromJson(Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao bloquear ONU na OLT.'));
    }
  }

  Future<OnuModel> unblockOnuByContract(String contractId) async {
    try {
      final response =
          await _apiClient.dio.post('/onus/contract/$contractId/unblock');
      return OnuModel.fromJson(Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao desbloquear ONU na OLT.'));
    }
  }

  // ==========================================
  // 2. Monitoramento NOC, Portas PON & Incidentes
  // ==========================================

  Future<NocMonitoringSummaryModel> getMonitoringSummary() async {
    try {
      final response = await _apiClient.dio.get('/ftth/monitoring/summary');
      return NocMonitoringSummaryModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao obter resumo do NOC.'));
    }
  }

  Future<NocMonitoringSummaryModel> forcePollNow() async {
    try {
      final response = await _apiClient.dio.post('/ftth/monitoring/poll-now');
      return NocMonitoringSummaryModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao executar ciclo de varredura PON.'));
    }
  }

  Future<List<OltPonPortModel>> getAllPonPorts() async {
    try {
      final response = await _apiClient.dio.get('/ftth/monitoring/pons');
      final list = response.data as List? ?? [];
      return list
          .map((e) => OltPonPortModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar portas PON monitoradas.'));
    }
  }

  Future<OltPonPortModel> createPonPort({
    required String networkDeviceId,
    int slotNumber = 0,
    int portNumber = 1,
    required String ponName,
    String? connectedCableId,
    double? txPowerDbm,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/monitoring/pons',
        data: {
          'networkDeviceId': networkDeviceId,
          'slotNumber': slotNumber,
          'portNumber': portNumber,
          'ponName': ponName,
          if (connectedCableId != null && connectedCableId.trim().isNotEmpty)
            'connectedCableId': connectedCableId.trim(),
          'txPowerDbm': ?txPowerDbm,
        },
      );
      return OltPonPortModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar porta PON.'));
    }
  }

  Future<List<FtthIncidentModel>> getAllIncidents({bool onlyActive = false}) async {
    try {
      final path = onlyActive
          ? '/ftth/monitoring/incidents/active'
          : '/ftth/monitoring/incidents';
      final response = await _apiClient.dio.get(path);
      final list = response.data as List? ?? [];
      return list
          .map((e) => FtthIncidentModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar incidentes do NOC.'));
    }
  }

  Future<FtthIncidentModel> dispatchIncident(
    String incidentId, {
    String? technicianId,
    String? notes,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/monitoring/incidents/$incidentId/dispatch',
        data: {
          if (technicianId != null && technicianId.trim().isNotEmpty)
            'technicianId': technicianId.trim(),
          if (notes != null && notes.trim().isNotEmpty) 'notes': notes.trim(),
        },
      );
      return FtthIncidentModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao despachar equipe para o incidente.'));
    }
  }

  Future<FtthIncidentModel> resolveIncident(
    String incidentId, {
    required String rootCauseNotes,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/monitoring/incidents/$incidentId/resolve',
        data: {'rootCauseNotes': rootCauseNotes},
      );
      return FtthIncidentModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao finalizar incidente FTTH.'));
    }
  }

  // ==========================================
  // 3. Topologia FTTH: POPs, Postes, Cabos, CTOs, Viabilidade & LightPath
  // ==========================================

  Future<List<FtthPopModel>> getAllPops() async {
    try {
      final response = await _apiClient.dio.get('/ftth/pops');
      final list = response.data as List? ?? [];
      return list
          .map((e) => FtthPopModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar POPs.'));
    }
  }

  Future<FtthPopModel> createPop({
    required String name,
    double? latitude,
    double? longitude,
    String? address,
    String? description,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/pops',
        data: {
          'name': name,
          'latitude': ?latitude,
          'longitude': ?longitude,
          if (address != null && address.trim().isNotEmpty)
            'address': address.trim(),
          if (description != null && description.trim().isNotEmpty)
            'description': description.trim(),
        },
      );
      return FtthPopModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar POP.'));
    }
  }

  Future<List<FtthPoleModel>> getAllPoles() async {
    try {
      final response = await _apiClient.dio.get('/ftth/poles');
      final list = response.data as List? ?? [];
      return list
          .map((e) => FtthPoleModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar postes.'));
    }
  }

  Future<FtthPoleModel> createPole({
    required String code,
    required double latitude,
    required double longitude,
    String poleType = 'CONCRETO',
    int reservationMeters = 0,
    String? description,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/poles',
        data: {
          'code': code,
          'latitude': latitude,
          'longitude': longitude,
          'poleType': poleType,
          'reservationMeters': reservationMeters,
          if (description != null && description.trim().isNotEmpty)
            'description': description.trim(),
        },
      );
      return FtthPoleModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar poste.'));
    }
  }

  Future<List<FtthCableModel>> getAllCables() async {
    try {
      final response = await _apiClient.dio.get('/ftth/cables');
      final list = response.data as List? ?? [];
      return list
          .map((e) => FtthCableModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar cabos ópticos.'));
    }
  }

  Future<FtthCableModel> createCable({
    required String name,
    String cableType = 'ALIMENTADOR',
    int fiberCount = 12,
    int tubeCount = 1,
    String colorStandard = 'ABNT_NBR_14106',
    double lengthMeters = 100.0,
    double attenuationDbPerKm = 0.35,
    String? sourcePopId,
    String? sourcePoleId,
    String? targetPoleId,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/cables',
        data: {
          'name': name,
          'cableType': cableType,
          'fiberCount': fiberCount,
          'tubeCount': tubeCount,
          'colorStandard': colorStandard,
          'lengthMeters': lengthMeters,
          'attenuationDbPerKm': attenuationDbPerKm,
          if (sourcePopId != null && sourcePopId.trim().isNotEmpty)
            'sourcePopId': sourcePopId.trim(),
          if (sourcePoleId != null && sourcePoleId.trim().isNotEmpty)
            'sourcePoleId': sourcePoleId.trim(),
          if (targetPoleId != null && targetPoleId.trim().isNotEmpty)
            'targetPoleId': targetPoleId.trim(),
        },
      );
      return FtthCableModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar cabo óptico.'));
    }
  }

  Future<List<FtthCtoModel>> getAllCtos() async {
    try {
      final response = await _apiClient.dio.get('/ftth/ctos');
      final list = response.data as List? ?? [];
      return list
          .map((e) => FtthCtoModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar CTOs.'));
    }
  }

  Future<FtthCtoModel> getCtoById(String id) async {
    try {
      final response = await _apiClient.dio.get('/ftth/ctos/$id');
      return FtthCtoModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao obter detalhes da CTO.'));
    }
  }

  Future<FtthCtoModel> createCto({
    required String name,
    required double latitude,
    required double longitude,
    int totalPorts = 16,
    String splitterType = 'BALANCED_1_16',
    String? poleId,
    String? closureId,
    String? description,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/ctos',
        data: {
          'name': name,
          'latitude': latitude,
          'longitude': longitude,
          'totalPorts': totalPorts,
          'splitterType': splitterType,
          if (poleId != null && poleId.trim().isNotEmpty) 'poleId': poleId.trim(),
          if (closureId != null && closureId.trim().isNotEmpty)
            'closureId': closureId.trim(),
          if (description != null && description.trim().isNotEmpty)
            'description': description.trim(),
        },
      );
      return FtthCtoModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar CTO.'));
    }
  }

  Future<FtthFeasibilityResultModel> checkFeasibility({
    required double latitude,
    required double longitude,
    double maxDistanceMeters = 200.0,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/feasibility',
        data: {
          'latitude': latitude,
          'longitude': longitude,
          'maxDistanceMeters': maxDistanceMeters,
        },
      );
      return FtthFeasibilityResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao consultar viabilidade FTTH.'));
    }
  }

  Future<LightPathTraceResultModel> traceLightPath(String ctoPortId) async {
    try {
      final response = await _apiClient.dio.get('/ftth/lightpath/$ctoPortId');
      return LightPathTraceResultModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao rastrear caminho óptico (LightPath).'));
    }
  }

  // ==========================================
  // 4. Caixas de Emenda (CEO), Splitters, Fusões & Diagrama Unifilar
  // ==========================================

  Future<List<FtthClosureModel>> getAllClosures() async {
    try {
      final response = await _apiClient.dio.get('/ftth/closures');
      final list = response.data as List? ?? [];
      return list
          .map((e) => FtthClosureModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao listar caixas de emenda (CEO).'));
    }
  }

  Future<FtthClosureModel> createClosure({
    required String name,
    String closureType = 'DOMO',
    int trayCount = 4,
    int capacityFusions = 48,
    String? poleId,
    double? latitude,
    double? longitude,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/closures',
        data: {
          'name': name,
          'closureType': closureType,
          'trayCount': trayCount,
          'capacityFusions': capacityFusions,
          if (poleId != null && poleId.trim().isNotEmpty) 'poleId': poleId.trim(),
          'latitude': ?latitude,
          'longitude': ?longitude,
        },
      );
      return FtthClosureModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao cadastrar caixa de emenda (CEO).'));
    }
  }

  Future<FtthClosureDiagramModel> getClosureDiagram(String closureId) async {
    try {
      final response =
          await _apiClient.dio.get('/ftth/closures/$closureId/diagram');
      return FtthClosureDiagramModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao carregar diagrama unifilar da CEO.'));
    }
  }

  Future<FtthSplitterModel> createSplitter({
    required String closureId,
    required String name,
    String splitterType = 'BALANCED_1_8',
    String? inputCableId,
    int? inputFiberNumber,
    double? attenuationDb,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/closures/$closureId/splitters',
        data: {
          'closureId': closureId,
          'name': name,
          'splitterType': splitterType,
          if (inputCableId != null && inputCableId.trim().isNotEmpty)
            'inputCableId': inputCableId.trim(),
          'inputFiberNumber': ?inputFiberNumber,
          'attenuationDb': ?attenuationDb,
        },
      );
      return FtthSplitterModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao instalar splitter na CEO.'));
    }
  }

  Future<FtthFusionModel> createFusion({
    required String closureId,
    int trayNumber = 1,
    required String sourceCableId,
    required int sourceFiberNumber,
    String? targetCableId,
    int? targetFiberNumber,
    String? targetSplitterId,
    String? targetCtoId,
    double lossDb = 0.05,
    String? description,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        '/ftth/closures/$closureId/fusions',
        data: {
          'closureId': closureId,
          'trayNumber': trayNumber,
          'sourceCableId': sourceCableId,
          'sourceFiberNumber': sourceFiberNumber,
          if (targetCableId != null && targetCableId.trim().isNotEmpty)
            'targetCableId': targetCableId.trim(),
          'targetFiberNumber': ?targetFiberNumber,
          if (targetSplitterId != null && targetSplitterId.trim().isNotEmpty)
            'targetSplitterId': targetSplitterId.trim(),
          if (targetCtoId != null && targetCtoId.trim().isNotEmpty)
            'targetCtoId': targetCtoId.trim(),
          'lossDb': lossDb,
          if (description != null && description.trim().isNotEmpty)
            'description': description.trim(),
        },
      );
      return FtthFusionModel.fromJson(
          Map<String, dynamic>.from(response.data as Map));
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao registrar fusão óptica na CEO.'));
    }
  }

  Future<void> deleteFusion(String fusionId) async {
    try {
      await _apiClient.dio.delete('/ftth/fusions/$fusionId');
    } on DioException catch (e) {
      throw Exception(_extractError(e, 'Falha ao desfazer fusão óptica.'));
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

final networkFtthRepositoryProvider = Provider<NetworkFtthRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return NetworkFtthRepository(apiClient);
});

final nocMonitoringSummaryProvider =
    FutureProvider.autoDispose<NocMonitoringSummaryModel>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getMonitoringSummary();
});

final oltPonPortsProvider =
    FutureProvider.autoDispose<List<OltPonPortModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllPonPorts();
});

final ftthIncidentsProvider =
    FutureProvider.autoDispose<List<FtthIncidentModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllIncidents();
});

final networkDevicesProvider =
    FutureProvider.autoDispose<List<NetworkDeviceModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllDevices();
});

final onusListProvider =
    FutureProvider.autoDispose<List<OnuModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllOnus();
});

final ftthPopsProvider =
    FutureProvider.autoDispose<List<FtthPopModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllPops();
});

final ftthPolesProvider =
    FutureProvider.autoDispose<List<FtthPoleModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllPoles();
});

final ftthCablesProvider =
    FutureProvider.autoDispose<List<FtthCableModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllCables();
});

final ftthCtosProvider =
    FutureProvider.autoDispose<List<FtthCtoModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllCtos();
});

final ftthClosuresProvider =
    FutureProvider.autoDispose<List<FtthClosureModel>>((ref) async {
  return ref.watch(networkFtthRepositoryProvider).getAllClosures();
});
