import 'package:flutter/foundation.dart';

@immutable
class WarehouseModel {
  final String id;
  final String code;
  final String name;
  final String city;
  final String state;
  final String? address;
  final String? responsibleUserId;
  final String? responsibleName;
  final String? responsibleCpf;
  final bool active;

  const WarehouseModel({
    required this.id,
    required this.code,
    required this.name,
    required this.city,
    required this.state,
    this.address,
    this.responsibleUserId,
    this.responsibleName,
    this.responsibleCpf,
    this.active = true,
  });

  factory WarehouseModel.fromJson(Map<String, dynamic> json) {
    return WarehouseModel(
      id: json['id']?.toString() ?? '',
      code: json['code']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      city: json['city']?.toString() ?? '',
      state: json['state']?.toString() ?? 'PA',
      address: json['address']?.toString(),
      responsibleUserId: json['responsibleUserId']?.toString(),
      responsibleName: json['responsibleName']?.toString(),
      responsibleCpf: json['responsibleCpf']?.toString(),
      active: json['active'] as bool? ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'code': code,
      'name': name,
      'city': city,
      'state': state,
      'address': address,
      'responsibleUserId': responsibleUserId,
      'responsibleName': responsibleName,
      'responsibleCpf': responsibleCpf,
      'active': active,
    };
  }
}

@immutable
class InventoryItemModel {
  final String id;
  final String code;
  final String name;
  final String category;
  final int quantityInStock;
  final int minQuantity;
  final String unit;

  const InventoryItemModel({
    required this.id,
    required this.code,
    required this.name,
    required this.category,
    required this.quantityInStock,
    required this.minQuantity,
    this.unit = 'UN',
  });

  bool get isCriticalStock => quantityInStock < minQuantity;

  factory InventoryItemModel.fromJson(Map<String, dynamic> json) {
    return InventoryItemModel(
      id: json['id']?.toString() ?? '',
      code: json['code']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      category: json['category']?.toString() ?? 'GERAL',
      quantityInStock: (json['quantityInStock'] as num?)?.toInt() ?? 0,
      minQuantity: (json['minQuantity'] as num?)?.toInt() ?? 10,
      unit: json['unit']?.toString() ?? 'UN',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'code': code,
      'name': name,
      'category': category,
      'quantityInStock': quantityInStock,
      'minQuantity': minQuantity,
      'unit': unit,
    };
  }
}

enum TransferStatus {
  pending,
  inTransit,
  received,
  canceled;

  static TransferStatus fromString(String? val) {
    switch (val?.toUpperCase()) {
      case 'IN_TRANSIT':
        return TransferStatus.inTransit;
      case 'RECEIVED':
        return TransferStatus.received;
      case 'CANCELED':
        return TransferStatus.canceled;
      case 'PENDING':
      default:
        return TransferStatus.pending;
    }
  }

  String get label {
    switch (this) {
      case TransferStatus.inTransit:
        return 'Em Trânsito';
      case TransferStatus.received:
        return 'Recebido / Conferido';
      case TransferStatus.canceled:
        return 'Cancelada';
      case TransferStatus.pending:
        return 'Aguardando Despacho';
    }
  }
}

@immutable
class StockTransferModel {
  final String id;
  final String code;
  final String originWarehouseId;
  final String destinationWarehouseId;
  final String? carrierUserId;
  final String carrierName;
  final String carrierDocument;
  final String carrierType;
  final TransferStatus status;
  final String? notes;
  final String? dispatchPhotoUrl;
  final String? receiptPhotoUrl;
  final String? dispatchedAt;
  final String? receivedAt;

  const StockTransferModel({
    required this.id,
    required this.code,
    required this.originWarehouseId,
    required this.destinationWarehouseId,
    this.carrierUserId,
    required this.carrierName,
    required this.carrierDocument,
    this.carrierType = 'COLABORADOR',
    this.status = TransferStatus.pending,
    this.notes,
    this.dispatchPhotoUrl,
    this.receiptPhotoUrl,
    this.dispatchedAt,
    this.receivedAt,
  });

  factory StockTransferModel.fromJson(Map<String, dynamic> json) {
    return StockTransferModel(
      id: json['id']?.toString() ?? '',
      code: json['code']?.toString() ?? '',
      originWarehouseId: json['originWarehouseId']?.toString() ?? '',
      destinationWarehouseId: json['destinationWarehouseId']?.toString() ?? '',
      carrierUserId: json['carrierUserId']?.toString(),
      carrierName: json['carrierName']?.toString() ?? '',
      carrierDocument: json['carrierDocument']?.toString() ?? '',
      carrierType: json['carrierType']?.toString() ?? 'COLABORADOR',
      status: TransferStatus.fromString(json['status']?.toString()),
      notes: json['notes']?.toString(),
      dispatchPhotoUrl: json['dispatchPhotoUrl']?.toString(),
      receiptPhotoUrl: json['receiptPhotoUrl']?.toString(),
      dispatchedAt: json['dispatchedAt']?.toString(),
      receivedAt: json['receivedAt']?.toString(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'code': code,
      'originWarehouseId': originWarehouseId,
      'destinationWarehouseId': destinationWarehouseId,
      'carrierUserId': carrierUserId,
      'carrierName': carrierName,
      'carrierDocument': carrierDocument,
      'carrierType': carrierType,
      'status': status.name.toUpperCase(),
      'notes': notes,
      'dispatchPhotoUrl': dispatchPhotoUrl,
      'receiptPhotoUrl': receiptPhotoUrl,
      'dispatchedAt': dispatchedAt,
      'receivedAt': receivedAt,
    };
  }
}

@immutable
class StockEntryPayload {
  final String? warehouseId;
  final String itemCode;
  final String itemName;
  final String category;
  final int quantity;
  final String unit;
  final String? notes;

  const StockEntryPayload({
    this.warehouseId,
    required this.itemCode,
    required this.itemName,
    required this.category,
    required this.quantity,
    required this.unit,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'warehouseId': warehouseId,
      'itemCode': itemCode,
      'itemName': itemName,
      'category': category,
      'quantity': quantity,
      'unit': unit,
      'notes': notes,
    };
  }
}

@immutable
class MaterialCheckoutPayload {
  final String workOrderId;
  final String technicianUserId;
  final String? warehouseId;
  final String? itemCode;
  final String? assetId;
  final int quantityOrMeters;
  final String? beforePhotoUrl;
  final String? notes;

  const MaterialCheckoutPayload({
    required this.workOrderId,
    required this.technicianUserId,
    this.warehouseId,
    this.itemCode,
    this.assetId,
    required this.quantityOrMeters,
    this.beforePhotoUrl,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'workOrderId': workOrderId,
      'technicianUserId': technicianUserId,
      'warehouseId': warehouseId,
      'itemCode': itemCode,
      'assetId': assetId,
      'quantityOrMeters': quantityOrMeters,
      'beforePhotoUrl': beforePhotoUrl,
      'notes': notes,
    };
  }
}

@immutable
class MaterialCheckinPayload {
  final String workOrderId;
  final String technicianUserId;
  final String? warehouseId;
  final String? itemCode;
  final String? assetId;
  final int initialMetersOrQty;
  final int consumedMetersOrQty;
  final int actualRemainingMetersOrQty;
  final String? beforePhotoUrl;
  final String? installedPhotoUrl;
  final String? returnPhotoUrl;
  final String? notes;

  const MaterialCheckinPayload({
    required this.workOrderId,
    required this.technicianUserId,
    this.warehouseId,
    this.itemCode,
    this.assetId,
    required this.initialMetersOrQty,
    required this.consumedMetersOrQty,
    required this.actualRemainingMetersOrQty,
    this.beforePhotoUrl,
    this.installedPhotoUrl,
    this.returnPhotoUrl,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'workOrderId': workOrderId,
      'technicianUserId': technicianUserId,
      'warehouseId': warehouseId,
      'itemCode': itemCode,
      'assetId': assetId,
      'initialMetersOrQty': initialMetersOrQty,
      'consumedMetersOrQty': consumedMetersOrQty,
      'actualRemainingMetersOrQty': actualRemainingMetersOrQty,
      'beforePhotoUrl': beforePhotoUrl,
      'installedPhotoUrl': installedPhotoUrl,
      'returnPhotoUrl': returnPhotoUrl,
      'notes': notes,
    };
  }
}

@immutable
class MaterialCheckinResult {
  final String logId;
  final String workOrderId;
  final String technicianUserId;
  final String status;
  final bool hasDivergence;
  final int expectedRemaining;
  final int actualRemaining;
  final int divergenceQuantity;
  final String? beforePhotoUrl;
  final String? installedPhotoUrl;
  final String? returnPhotoUrl;
  final String? notes;
  final String? checkedAt;

  const MaterialCheckinResult({
    required this.logId,
    required this.workOrderId,
    required this.technicianUserId,
    required this.status,
    required this.hasDivergence,
    required this.expectedRemaining,
    required this.actualRemaining,
    required this.divergenceQuantity,
    this.beforePhotoUrl,
    this.installedPhotoUrl,
    this.returnPhotoUrl,
    this.notes,
    this.checkedAt,
  });

  factory MaterialCheckinResult.fromJson(Map<String, dynamic> json) {
    return MaterialCheckinResult(
      logId: json['logId']?.toString() ?? '',
      workOrderId: json['workOrderId']?.toString() ?? '',
      technicianUserId: json['technicianUserId']?.toString() ?? '',
      status: json['status']?.toString() ?? 'CONFORMANT',
      hasDivergence: json['hasDivergence'] as bool? ?? false,
      expectedRemaining: (json['expectedRemaining'] as num?)?.toInt() ?? 0,
      actualRemaining: (json['actualRemaining'] as num?)?.toInt() ?? 0,
      divergenceQuantity: (json['divergenceQuantity'] as num?)?.toInt() ?? 0,
      beforePhotoUrl: json['beforePhotoUrl']?.toString(),
      installedPhotoUrl: json['installedPhotoUrl']?.toString(),
      returnPhotoUrl: json['returnPhotoUrl']?.toString(),
      notes: json['notes']?.toString(),
      checkedAt: json['checkedAt']?.toString(),
    );
  }
}
