import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../dispatch/data/dispatch_models.dart';
import 'inventory_models.dart';
import 'inventory_repository.dart';

@immutable
class InventoryState {
  final bool isLoading;
  final List<WarehouseModel> warehouses;
  final WarehouseModel? selectedWarehouse;
  final List<InventoryItemModel> items;
  final List<InstallationDemandModel> demands;
  final List<StockTransferModel> transfers;
  final int selectedTab;
  final bool isSubmitting;
  final MaterialCheckinResult? lastCheckinResult;
  final String? successMessage;
  final String? errorMessage;

  const InventoryState({
    this.isLoading = false,
    this.warehouses = const [],
    this.selectedWarehouse,
    this.items = const [],
    this.demands = const [],
    this.transfers = const [],
    this.selectedTab = 0,
    this.isSubmitting = false,
    this.lastCheckinResult,
    this.successMessage,
    this.errorMessage,
  });

  InventoryState copyWith({
    bool? isLoading,
    List<WarehouseModel>? warehouses,
    WarehouseModel? selectedWarehouse,
    bool clearSelectedWarehouse = false,
    List<InventoryItemModel>? items,
    List<InstallationDemandModel>? demands,
    List<StockTransferModel>? transfers,
    int? selectedTab,
    bool? isSubmitting,
    MaterialCheckinResult? lastCheckinResult,
    bool clearCheckinResult = false,
    String? successMessage,
    String? errorMessage,
    bool clearMessages = false,
  }) {
    return InventoryState(
      isLoading: isLoading ?? this.isLoading,
      warehouses: warehouses ?? this.warehouses,
      selectedWarehouse: clearSelectedWarehouse ? null : (selectedWarehouse ?? this.selectedWarehouse),
      items: items ?? this.items,
      demands: demands ?? this.demands,
      transfers: transfers ?? this.transfers,
      selectedTab: selectedTab ?? this.selectedTab,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      lastCheckinResult: clearCheckinResult ? null : (lastCheckinResult ?? this.lastCheckinResult),
      successMessage: clearMessages ? null : (successMessage ?? this.successMessage),
      errorMessage: clearMessages ? null : (errorMessage ?? this.errorMessage),
    );
  }

  /// Quantidade de demandas pendentes de triagem no estoque.
  int get pendingDemandsCount => demands.where((d) => d.status == MaterialDemandStatus.pendingAllocation).length;

  /// Quantidade de itens em nível crítico.
  int get criticalItemsCount => items.where((i) => i.isCriticalStock).length;
}

class InventoryNotifier extends StateNotifier<InventoryState> {
  final InventoryRepository _repository;

  InventoryNotifier(this._repository) : super(const InventoryState()) {
    loadAll();
  }

  /// Carrega depósitos, catálogo de insumos, demandas de instalação e transferências.
  Future<void> loadAll({bool clearMessages = true}) async {
    state = state.copyWith(isLoading: true, clearMessages: clearMessages);
    try {
      final results = await Future.wait([
        _repository.listWarehouses(),
        _repository.listInventoryItems(),
        _repository.listInstallationDemands(),
        _repository.listTransfers(),
      ]);

      final warehouses = results[0] as List<WarehouseModel>;
      final items = results[1] as List<InventoryItemModel>;
      final demands = results[2] as List<InstallationDemandModel>;
      final transfers = results[3] as List<StockTransferModel>;

      WarehouseModel? selected = state.selectedWarehouse;
      if (selected == null && warehouses.isNotEmpty) {
        selected = warehouses.first;
      }

      state = state.copyWith(
        isLoading: false,
        warehouses: warehouses,
        selectedWarehouse: selected,
        items: items,
        demands: demands,
        transfers: transfers,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Falha ao carregar dados do estoque: $e',
      );
    }
  }

  void selectWarehouse(WarehouseModel? warehouse) {
    state = state.copyWith(
      selectedWarehouse: warehouse,
      clearSelectedWarehouse: warehouse == null,
    );
  }

  void setTab(int tab) {
    state = state.copyWith(selectedTab: tab);
  }

  /// Confirma e reserva materiais no almoxarifado para uma O.S. (ALLOCATED_CENTRAL).
  Future<bool> confirmStockForWorkOrder(String workOrderId) async {
    state = state.copyWith(isSubmitting: true, clearMessages: true);
    try {
      final success = await _repository.confirmStockAllocation(
        workOrderId,
        warehouseId: state.selectedWarehouse?.id,
      );
      if (success) {
        state = state.copyWith(
          isSubmitting: false,
          successMessage: 'Materiais confirmados e separados com sucesso no almoxarifado!',
        );
        await loadAll(clearMessages: false);
        return true;
      } else {
        state = state.copyWith(
          isSubmitting: false,
          errorMessage: 'Não foi possível confirmar os materiais da O.S.',
        );
        return false;
      }
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Erro ao confirmar materiais: $e',
      );
      return false;
    }
  }

  /// Dá entrada em novo lote de insumos/bobinas.
  Future<bool> registerStockEntry(StockEntryPayload payload) async {
    state = state.copyWith(isSubmitting: true, clearMessages: true);
    try {
      final created = await _repository.registerStockEntry(payload);
      if (created != null) {
        state = state.copyWith(
          isSubmitting: false,
          successMessage: 'Entrada de ${payload.quantity} ${payload.unit} de ${payload.itemName} registrada com sucesso!',
        );
        await loadAll(clearMessages: false);
        return true;
      } else {
        state = state.copyWith(
          isSubmitting: false,
          errorMessage: 'Não foi possível registrar a entrada do material.',
        );
        return false;
      }
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Erro na entrada de material: $e',
      );
      return false;
    }
  }

  /// Cria guia de transferência inter-bases com portador responsável.
  Future<bool> createTransfer({
    required String destinationWarehouseId,
    required String carrierName,
    required String carrierDocument,
    String? carrierUserId,
    String carrierType = 'COLABORADOR',
    String? notes,
  }) async {
    final originId = state.selectedWarehouse?.id;
    if (originId == null) {
      state = state.copyWith(errorMessage: 'Selecione a base de origem para a transferência.');
      return false;
    }

    state = state.copyWith(isSubmitting: true, clearMessages: true);
    try {
      final transfer = await _repository.createTransfer(
        originWarehouseId: originId,
        destinationWarehouseId: destinationWarehouseId,
        carrierName: carrierName,
        carrierDocument: carrierDocument,
        carrierUserId: carrierUserId,
        carrierType: carrierType,
        notes: notes,
      );

      if (transfer != null) {
        state = state.copyWith(
          isSubmitting: false,
          successMessage: 'Guia de transferência ${transfer.code} criada com sucesso para $carrierName!',
        );
        await loadAll(clearMessages: false);
        return true;
      }
      return false;
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Erro ao criar transferência: $e',
      );
      return false;
    }
  }

  /// Despacha a guia com o portador (status IN_TRANSIT).
  Future<bool> dispatchTransfer(String transferId) async {
    state = state.copyWith(isSubmitting: true, clearMessages: true);
    try {
      final success = await _repository.dispatchTransfer(transferId);
      if (success) {
        state = state.copyWith(
          isSubmitting: false,
          successMessage: 'Transferência despachada! Custódia sob a responsabilidade do portador.',
        );
        await loadAll(clearMessages: false);
        return true;
      }
      return false;
    } catch (e) {
      state = state.copyWith(isSubmitting: false, errorMessage: 'Erro ao despachar: $e');
      return false;
    }
  }

  /// Confirma recebimento da carga na filial de destino (status RECEIVED).
  Future<bool> receiveTransfer(String transferId) async {
    state = state.copyWith(isSubmitting: true, clearMessages: true);
    try {
      final success = await _repository.confirmReceiptTransfer(transferId);
      if (success) {
        state = state.copyWith(
          isSubmitting: false,
          successMessage: 'Carga recebida e conferida! Custódia transferida para a filial de destino.',
        );
        await loadAll(clearMessages: false);
        return true;
      }
      return false;
    } catch (e) {
      state = state.copyWith(isSubmitting: false, errorMessage: 'Erro ao receber carga: $e');
      return false;
    }
  }

  /// Retirada de material vinculada estritamente a uma O.S. (Regra de Ouro).
  Future<bool> checkoutMaterial(MaterialCheckoutPayload payload) async {
    state = state.copyWith(isSubmitting: true, clearMessages: true);
    try {
      final success = await _repository.checkoutMaterialForWorkOrder(payload);
      if (success) {
        state = state.copyWith(
          isSubmitting: false,
          successMessage: 'Retirada autorizada e vinculada à O.S. com sucesso!',
        );
        await loadAll(clearMessages: false);
        return true;
      }
      return false;
    } catch (e) {
      state = state.copyWith(isSubmitting: false, errorMessage: 'Erro na retirada: $e');
      return false;
    }
  }

  /// Devolução com conferência métrica e cálculo de divergência.
  Future<MaterialCheckinResult?> checkinMaterial(MaterialCheckinPayload payload) async {
    state = state.copyWith(isSubmitting: true, clearMessages: true);
    try {
      final result = await _repository.checkinMaterialForWorkOrder(payload);
      if (result != null) {
        state = state.copyWith(
          isSubmitting: false,
          lastCheckinResult: result,
          successMessage: result.hasDivergence
              ? 'Devolução registrada com RESSALVA DE DIVERGÊNCIA (${result.divergenceQuantity} un/m)!'
              : 'Devolução conferida 100% íntegra!',
        );
        await loadAll(clearMessages: false);
        return result;
      }
      return null;
    } catch (e) {
      state = state.copyWith(isSubmitting: false, errorMessage: 'Erro na conferência: $e');
      return null;
    }
  }

  void clearMessages() {
    state = state.copyWith(clearMessages: true, clearCheckinResult: true);
  }
}

final inventoryProvider = StateNotifierProvider<InventoryNotifier, InventoryState>((ref) {
  final repo = ref.watch(inventoryRepositoryProvider);
  return InventoryNotifier(repo);
});
