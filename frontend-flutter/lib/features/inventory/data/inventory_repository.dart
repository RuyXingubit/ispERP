import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import '../../dispatch/data/dispatch_models.dart';
import 'inventory_models.dart';

class InventoryRepository {
  final ApiClient _apiClient;

  InventoryRepository(this._apiClient);

  Dio get _dio => _apiClient.dio;

  /// Lista todos os depósitos e pontos de apoio físicos cadastrados.
  Future<List<WarehouseModel>> listWarehouses() async {
    try {
      final response = await _dio.get('/warehouses');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => WarehouseModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      rethrow;
    }
  }

  /// Lista todos os colaboradores e usuários cadastrados para atribuição de transporte.
  Future<List<CollaboratorModel>> listCollaborators() async {
    try {
      final response = await _dio.get('/users');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => CollaboratorModel.fromJson(e as Map<String, dynamic>))
            .where((c) => c.active)
            .toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  /// Cadastra uma pessoa / terceiro formalmente no sistema para atuar no transporte.
  Future<CollaboratorModel?> createCollaborator({
    required String name,
    required String email,
    required String password,
    String? cpf,
    String role = 'USER',
  }) async {
    try {
      final response = await _dio.post(
        '/users',
        data: {
          'name': name,
          'email': email,
          'password': password,
          'cpf': cpf,
          'role': role,
          'active': true,
        },
      );
      if ((response.statusCode == 200 || response.statusCode == 201) && response.data is Map<String, dynamic>) {
        return CollaboratorModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      rethrow;
    }
  }

  /// Lista os itens do catálogo e saldos de insumos.
  Future<List<InventoryItemModel>> listInventoryItems() async {
    try {
      final response = await _dio.get('/inventory');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => InventoryItemModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      rethrow;
    }
  }

  /// Lista demandas de instalação para triagem e confirmação de materiais.
  Future<List<InstallationDemandModel>> listInstallationDemands() async {
    try {
      final response = await _dio.get('/dispatch/installations/demands');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => InstallationDemandModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      rethrow;
    }
  }

  /// Confirma e reserva materiais no almoxarifado para a O.S. (ALLOCATED_CENTRAL).
  Future<bool> confirmStockAllocation(String workOrderId, {String? warehouseId}) async {
    try {
      final response = await _dio.post(
        '/dispatch/installations/demands/$workOrderId/confirm-stock',
        queryParameters: warehouseId != null ? {'warehouseId': warehouseId} : null,
      );
      return response.statusCode == 200;
    } catch (e) {
      rethrow;
    }
  }

  /// Dá entrada em um lote de materiais (compra/nota fiscal).
  Future<InventoryItemModel?> registerStockEntry(StockEntryPayload payload) async {
    try {
      final response = await _dio.post('/inventory/entry', data: payload.toJson());
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return InventoryItemModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      rethrow;
    }
  }

  /// Lista todas as guias de transferência inter-bases.
  Future<List<StockTransferModel>> listTransfers() async {
    try {
      final response = await _dio.get('/inventory/custody/transfers');
      if (response.statusCode == 200 && response.data is List) {
        return (response.data as List)
            .map((e) => StockTransferModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  /// Cria uma nova guia de transferência entre depósitos com portador responsável.
  Future<StockTransferModel?> createTransfer({
    required String originWarehouseId,
    required String destinationWarehouseId,
    required String carrierName,
    required String carrierDocument,
    String? carrierUserId,
    String carrierType = 'COLABORADOR',
    String? notes,
  }) async {
    try {
      final response = await _dio.post(
        '/inventory/custody/transfers',
        data: {
          'originWarehouseId': originWarehouseId,
          'destinationWarehouseId': destinationWarehouseId,
          'carrierName': carrierName,
          'carrierDocument': carrierDocument,
          'carrierUserId': carrierUserId,
          'carrierType': carrierType,
          'notes': notes,
        },
      );
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return StockTransferModel.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      rethrow;
    }
  }

  /// Despacha uma guia de transferência colocando os itens em trânsito com o portador.
  Future<bool> dispatchTransfer(String transferId, {String? dispatchPhotoUrl, String? userId}) async {
    try {
      final response = await _dio.post(
        '/inventory/custody/transfers/$transferId/dispatch',
        data: {
          'userId': userId,
          'dispatchPhotoUrl': dispatchPhotoUrl,
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      rethrow;
    }
  }

  /// Confirma o recebimento da transferência na base de destino.
  Future<bool> confirmReceiptTransfer(String transferId, {String? receiptPhotoUrl, String? userId}) async {
    try {
      final response = await _dio.post(
        '/inventory/custody/transfers/$transferId/receive',
        data: {
          'userId': userId,
          'receiptPhotoUrl': receiptPhotoUrl,
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      rethrow;
    }
  }

  /// Regra de Ouro: Retirada de material vinculada obrigatoriamente a uma O.S.
  Future<bool> checkoutMaterialForWorkOrder(MaterialCheckoutPayload payload) async {
    try {
      final response = await _dio.post(
        '/inventory/custody/materials/checkout-os',
        data: payload.toJson(),
      );
      return response.statusCode == 200;
    } catch (e) {
      rethrow;
    }
  }

  /// Devolução de material com conferência métrica e fotos das 3 etapas probatórias.
  Future<MaterialCheckinResult?> checkinMaterialForWorkOrder(MaterialCheckinPayload payload) async {
    try {
      final response = await _dio.post(
        '/inventory/custody/materials/checkin-os',
        data: payload.toJson(),
      );
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return MaterialCheckinResult.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      rethrow;
    }
  }
}

final inventoryRepositoryProvider = Provider<InventoryRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return InventoryRepository(apiClient);
});
