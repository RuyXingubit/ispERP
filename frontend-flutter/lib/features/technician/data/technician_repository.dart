import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';

/// Modelo de Ordem de Serviço retornado pela API (WorkOrderResponse).
class TechnicianWorkOrderModel {
  final String id;
  final String contractId;
  final String customerId;
  final String type;
  final String status;
  final String? scheduledDate;
  final String? scheduledPeriod;
  final String? technicianName;
  final String? onuMac;
  final String? onuSerial;
  final double? fiberSignalDbm;
  final double? onuRxPowerDbm;
  final bool? radiusAuthenticated;
  final String? customerSignatureName;
  final String? notes;

  const TechnicianWorkOrderModel({
    required this.id,
    required this.contractId,
    required this.customerId,
    required this.type,
    required this.status,
    this.scheduledDate,
    this.scheduledPeriod,
    this.technicianName,
    this.onuMac,
    this.onuSerial,
    this.fiberSignalDbm,
    this.onuRxPowerDbm,
    this.radiusAuthenticated,
    this.customerSignatureName,
    this.notes,
  });

  factory TechnicianWorkOrderModel.fromJson(Map<String, dynamic> json) {
    double? parseDouble(dynamic val) {
      if (val == null) return null;
      if (val is num) return val.toDouble();
      return double.tryParse(val.toString());
    }

    return TechnicianWorkOrderModel(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      customerId: json['customerId']?.toString() ?? '',
      type: json['type']?.toString() ?? 'INSTALACAO',
      status: json['status']?.toString() ?? 'PENDING_SCHEDULE',
      scheduledDate: json['scheduledDate']?.toString(),
      scheduledPeriod: json['scheduledPeriod']?.toString(),
      technicianName: json['technicianName']?.toString(),
      onuMac: json['onuMac']?.toString(),
      onuSerial: json['onuSerial']?.toString(),
      fiberSignalDbm: parseDouble(json['fiberSignalDbm']),
      onuRxPowerDbm: parseDouble(json['onuRxPowerDbm']),
      radiusAuthenticated: json['radiusAuthenticated'] == true,
      customerSignatureName: json['customerSignatureName']?.toString(),
      notes: json['notes']?.toString(),
    );
  }

  String get shortId => id.length > 8 ? id.substring(0, 8).toUpperCase() : id;

  String get typeLabel {
    switch (type.toUpperCase()) {
      case 'INSTALACAO':
        return 'Instalação FTTH';
      case 'MANUTENCAO':
      case 'REPARO_FIBRA':
        return 'Manutenção / Sinal';
      case 'MUDANCA_ENDERECO':
        return 'Mudança de Endereço';
      case 'RETIRADA':
        return 'Retirada de Comodato';
      default:
        return type;
    }
  }

  String get statusLabel {
    switch (status.toUpperCase()) {
      case 'PENDING_SCHEDULE':
        return 'Aguardando Agendamento';
      case 'SCHEDULED':
        return 'Agendada';
      case 'IN_PROGRESS':
        return 'Em Execução';
      case 'COMPLETED':
        return 'Concluída';
      case 'CANCELLED':
        return 'Cancelada';
      case 'INFRUTIFERA':
        return 'Infrutífera';
      default:
        return status;
    }
  }
}

/// Repositório para operações técnicas de campo e Ordens de Serviço (/work-orders e /technician/execution).
class TechnicianRepository {
  final ApiClient _apiClient;

  TechnicianRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Lista todas as Ordens de Serviço cadastradas no backend.
  Future<List<TechnicianWorkOrderModel>> getWorkOrders({String? status}) async {
    try {
      final response = await _dio.get(
        '/work-orders',
        queryParameters: status != null ? {'status': status} : null,
      );
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => TechnicianWorkOrderModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Conclui a execução técnica em campo com validação óptica e baixa de estoque.
  Future<TechnicianWorkOrderModel?> completeFieldExecution({
    required String workOrderId,
    required String onuSerial,
    String? onuMac,
    double? fiberSignalDbm,
    String? customerSignatureName,
    String? notes,
  }) async {
    final response = await _dio.post(
      '/technician/execution/$workOrderId/complete',
      data: {
        'onuSerial': onuSerial,
        if (onuMac != null && onuMac.isNotEmpty) 'onuMac': onuMac,
        'fiberSignalDbm': ?fiberSignalDbm,
        if (customerSignatureName != null && customerSignatureName.isNotEmpty)
          'customerSignatureName': customerSignatureName,
        if (notes != null && notes.isNotEmpty) 'notes': notes,
      },
    );
    if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
      return TechnicianWorkOrderModel.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }
}

final technicianRepositoryProvider = Provider<TechnicianRepository>((ref) {
  final api = ref.watch(apiClientProvider);
  return TechnicianRepository(api);
});

final technicianWorkOrdersProvider = FutureProvider.autoDispose<List<TechnicianWorkOrderModel>>((ref) async {
  final repo = ref.watch(technicianRepositoryProvider);
  return repo.getWorkOrders();
});
