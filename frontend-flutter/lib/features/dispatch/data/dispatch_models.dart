import 'package:flutter/foundation.dart';

/// Status do ciclo de vida da demanda de materiais para a instalação.
enum MaterialDemandStatus {
  pendingAllocation,
  allocatedVehicle,
  allocatedCentral,
  consumedInField,
  cancelled;

  static MaterialDemandStatus fromString(String? val) {
    if (val == null) return MaterialDemandStatus.pendingAllocation;
    switch (val.toUpperCase().trim()) {
      case 'ALLOCATED_VEHICLE':
        return MaterialDemandStatus.allocatedVehicle;
      case 'ALLOCATED_CENTRAL':
        return MaterialDemandStatus.allocatedCentral;
      case 'CONSUMED_IN_FIELD':
        return MaterialDemandStatus.consumedInField;
      case 'CANCELLED':
        return MaterialDemandStatus.cancelled;
      case 'PENDING_ALLOCATION':
      default:
        return MaterialDemandStatus.pendingAllocation;
    }
  }

  String get label {
    switch (this) {
      case MaterialDemandStatus.pendingAllocation:
        return 'Aguardando Triagem';
      case MaterialDemandStatus.allocatedVehicle:
        return 'Alocado no Veículo';
      case MaterialDemandStatus.allocatedCentral:
        return 'Reservado no Central';
      case MaterialDemandStatus.consumedInField:
        return 'Instalado em Campo';
      case MaterialDemandStatus.cancelled:
        return 'Cancelado';
    }
  }
}

/// DTO com os detalhes calculados da demanda de materiais FTTH de uma O.S.
@immutable
class InstallationDemandModel {
  final String id;
  final String workOrderId;
  final String contractId;
  final String? contractNumber;
  final String customerName;
  final String? customerPhone;
  final String customerAddress;
  final double? customerLatitude;
  final double? customerLongitude;
  final String? ctoId;
  final String? ctoName;
  final double? ctoLatitude;
  final double? ctoLongitude;
  final int? ctoPortNumber;
  final int estimatedDropMeters;
  final String onuModelRequired;
  final int fastConnectorsCount;
  final int ptoRosetteCount;
  final MaterialDemandStatus status;
  final String? allocatedWarehouseId;
  final String? allocatedWarehouseName;
  final String? allocatedTechnicianName;
  final DateTime? createdAt;

  const InstallationDemandModel({
    required this.id,
    required this.workOrderId,
    required this.contractId,
    this.contractNumber,
    required this.customerName,
    this.customerPhone,
    required this.customerAddress,
    this.customerLatitude,
    this.customerLongitude,
    this.ctoId,
    this.ctoName,
    this.ctoLatitude,
    this.ctoLongitude,
    this.ctoPortNumber,
    required this.estimatedDropMeters,
    required this.onuModelRequired,
    required this.fastConnectorsCount,
    required this.ptoRosetteCount,
    required this.status,
    this.allocatedWarehouseId,
    this.allocatedWarehouseName,
    this.allocatedTechnicianName,
    this.createdAt,
  });

  factory InstallationDemandModel.fromJson(Map<String, dynamic> json) {
    double? parseDouble(dynamic v) {
      if (v == null) return null;
      if (v is num) return v.toDouble();
      return double.tryParse(v.toString());
    }

    int parseInt(dynamic v, [int fallback = 0]) {
      if (v == null) return fallback;
      if (v is num) return v.toInt();
      return int.tryParse(v.toString()) ?? fallback;
    }

    DateTime? parseDate(dynamic v) {
      if (v == null) return null;
      try {
        return DateTime.parse(v.toString());
      } catch (_) {
        return null;
      }
    }

    return InstallationDemandModel(
      id: json['id']?.toString() ?? '',
      workOrderId: json['workOrderId']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      contractNumber: json['contractNumber']?.toString(),
      customerName: json['customerName']?.toString() ?? 'Cliente',
      customerPhone: json['customerPhone']?.toString(),
      customerAddress: json['customerAddress']?.toString() ?? 'Endereço não informado',
      customerLatitude: parseDouble(json['customerLatitude']),
      customerLongitude: parseDouble(json['customerLongitude']),
      ctoId: json['ctoId']?.toString(),
      ctoName: json['ctoName']?.toString(),
      ctoLatitude: parseDouble(json['ctoLatitude']),
      ctoLongitude: parseDouble(json['ctoLongitude']),
      ctoPortNumber: json['ctoPortNumber'] != null ? parseInt(json['ctoPortNumber']) : null,
      estimatedDropMeters: parseInt(json['estimatedDropMeters'], 50),
      onuModelRequired: json['onuModelRequired']?.toString() ?? 'ONT Wi-Fi Dual-Band GPON Gigabit',
      fastConnectorsCount: parseInt(json['fastConnectorsCount'], 2),
      ptoRosetteCount: parseInt(json['ptoRosetteCount'], 1),
      status: MaterialDemandStatus.fromString(json['status']?.toString()),
      allocatedWarehouseId: json['allocatedWarehouseId']?.toString(),
      allocatedWarehouseName: json['allocatedWarehouseName']?.toString(),
      allocatedTechnicianName: json['allocatedTechnicianName']?.toString(),
      createdAt: parseDate(json['createdAt']),
    );
  }
}

/// DTO de técnico candidato ranqueado por posse de kit no veículo e proximidade GPS.
@immutable
class TechnicianCandidateModel {
  final String technicianId;
  final String technicianName;
  final String? warehouseId;
  final String? vehicleWarehouseName;
  final bool hasCompleteKit;
  final bool hasOnu;
  final bool hasDropCable;
  final bool hasConnectors;
  final int dropCableBalanceMeters;
  final double? currentLatitude;
  final double? currentLongitude;
  final double? distanceKmToCustomer;
  final String? lastServiceAddress;
  final double recommendedScore;

  const TechnicianCandidateModel({
    required this.technicianId,
    required this.technicianName,
    this.warehouseId,
    this.vehicleWarehouseName,
    required this.hasCompleteKit,
    required this.hasOnu,
    required this.hasDropCable,
    required this.hasConnectors,
    required this.dropCableBalanceMeters,
    this.currentLatitude,
    this.currentLongitude,
    this.distanceKmToCustomer,
    this.lastServiceAddress,
    required this.recommendedScore,
  });

  factory TechnicianCandidateModel.fromJson(Map<String, dynamic> json) {
    double? parseDouble(dynamic v) {
      if (v == null) return null;
      if (v is num) return v.toDouble();
      return double.tryParse(v.toString());
    }

    int parseInt(dynamic v, [int fallback = 0]) {
      if (v == null) return fallback;
      if (v is num) return v.toInt();
      return int.tryParse(v.toString()) ?? fallback;
    }

    bool parseBool(dynamic v) {
      if (v == null) return false;
      if (v is bool) return v;
      return v.toString().toLowerCase() == 'true';
    }

    return TechnicianCandidateModel(
      technicianId: json['technicianId']?.toString() ?? '',
      technicianName: json['technicianName']?.toString() ?? 'Técnico',
      warehouseId: json['warehouseId']?.toString(),
      vehicleWarehouseName: json['vehicleWarehouseName']?.toString(),
      hasCompleteKit: parseBool(json['hasCompleteKit']),
      hasOnu: parseBool(json['hasOnu']),
      hasDropCable: parseBool(json['hasDropCable']),
      hasConnectors: parseBool(json['hasConnectors']),
      dropCableBalanceMeters: parseInt(json['dropCableBalanceMeters']),
      currentLatitude: parseDouble(json['currentLatitude']),
      currentLongitude: parseDouble(json['currentLongitude']),
      distanceKmToCustomer: parseDouble(json['distanceKmToCustomer']),
      lastServiceAddress: json['lastServiceAddress']?.toString(),
      recommendedScore: parseDouble(json['recommendedScore']) ?? 0.0,
    );
  }
}
