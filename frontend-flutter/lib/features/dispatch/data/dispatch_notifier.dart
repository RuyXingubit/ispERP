import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dispatch_models.dart';
import 'dispatch_repository.dart';

@immutable
class DispatchState {
  final bool isLoadingDemands;
  final List<InstallationDemandModel> demands;
  final InstallationDemandModel? selectedDemand;
  final bool isLoadingCandidates;
  final List<TechnicianCandidateModel> candidates;
  final bool isDispatching;
  final String? dispatchSuccessMessage;
  final String? errorMessage;
  final int selectedFilterTab; // 0: Todas / Pendentes, 1: Alocadas / Agendadas, 2: Concluídas

  const DispatchState({
    this.isLoadingDemands = false,
    this.demands = const [],
    this.selectedDemand,
    this.isLoadingCandidates = false,
    this.candidates = const [],
    this.isDispatching = false,
    this.dispatchSuccessMessage,
    this.errorMessage,
    this.selectedFilterTab = 0,
  });

  DispatchState copyWith({
    bool? isLoadingDemands,
    List<InstallationDemandModel>? demands,
    InstallationDemandModel? selectedDemand,
    bool clearSelectedDemand = false,
    bool? isLoadingCandidates,
    List<TechnicianCandidateModel>? candidates,
    bool? isDispatching,
    String? dispatchSuccessMessage,
    String? errorMessage,
    bool clearMessages = false,
    int? selectedFilterTab,
  }) {
    return DispatchState(
      isLoadingDemands: isLoadingDemands ?? this.isLoadingDemands,
      demands: demands ?? this.demands,
      selectedDemand: clearSelectedDemand ? null : (selectedDemand ?? this.selectedDemand),
      isLoadingCandidates: isLoadingCandidates ?? this.isLoadingCandidates,
      candidates: candidates ?? this.candidates,
      isDispatching: isDispatching ?? this.isDispatching,
      dispatchSuccessMessage: clearMessages ? null : (dispatchSuccessMessage ?? this.dispatchSuccessMessage),
      errorMessage: clearMessages ? null : (errorMessage ?? this.errorMessage),
      selectedFilterTab: selectedFilterTab ?? this.selectedFilterTab,
    );
  }

  /// Lista filtrada conforme a aba selecionada.
  List<InstallationDemandModel> get filteredDemands {
    if (selectedFilterTab == 1) {
      return demands.where((d) => d.status == MaterialDemandStatus.allocatedVehicle || d.status == MaterialDemandStatus.allocatedCentral).toList();
    } else if (selectedFilterTab == 2) {
      return demands.where((d) => d.status == MaterialDemandStatus.consumedInField).toList();
    }
    // Tab 0: Pendentes de Triagem / Agendamento
    return demands.where((d) => d.status == MaterialDemandStatus.pendingAllocation).toList();
  }
}

class DispatchNotifier extends StateNotifier<DispatchState> {
  final DispatchRepository _repository;

  DispatchNotifier(this._repository) : super(const DispatchState()) {
    loadDemands();
  }

  /// Carrega as demandas de instalação do backend.
  Future<void> loadDemands({bool clearMessages = true}) async {
    state = state.copyWith(isLoadingDemands: true, clearMessages: clearMessages);
    try {
      final demands = await _repository.listDemands();
      InstallationDemandModel? currentSelected = state.selectedDemand;

      // Mantém a seleção atual se ainda existir na lista, ou seleciona a primeira pendente
      if (currentSelected != null) {
        final found = demands.where((d) => d.workOrderId == currentSelected!.workOrderId).toList();
        currentSelected = found.isNotEmpty ? found.first : null;
      }

      if (currentSelected == null && demands.isNotEmpty) {
        currentSelected = demands.first;
      }

      state = state.copyWith(
        isLoadingDemands: false,
        demands: demands,
        selectedDemand: currentSelected,
      );

      if (currentSelected != null && currentSelected.workOrderId.isNotEmpty) {
        await loadCandidates(currentSelected.workOrderId);
      }
    } catch (e) {
      state = state.copyWith(
        isLoadingDemands: false,
        errorMessage: 'Erro ao carregar demandas de instalação: $e',
      );
    }
  }

  /// Seleciona uma demanda para ver a auditoria de estoque e candidatos.
  Future<void> selectDemand(InstallationDemandModel demand) async {
    if (state.selectedDemand?.id == demand.id) return;

    state = state.copyWith(
      selectedDemand: demand,
      candidates: const [],
      clearMessages: true,
    );
    await loadCandidates(demand.workOrderId);
  }

  /// Carrega os técnicos candidatos para a O.S. selecionada.
  Future<void> loadCandidates(String workOrderId, {bool clearMessages = false}) async {
    if (workOrderId.isEmpty) return;

    state = state.copyWith(isLoadingCandidates: true, clearMessages: clearMessages);
    try {
      final candidates = await _repository.listCandidates(workOrderId);
      state = state.copyWith(
        isLoadingCandidates: false,
        candidates: candidates,
      );
    } catch (e) {
      state = state.copyWith(
        isLoadingCandidates: false,
        errorMessage: 'Não foi possível carregar técnicos candidatos: $e',
      );
    }
  }

  /// Despacha a O.S. para o técnico selecionado.
  Future<bool> dispatchToTechnician(String technicianId) async {
    final demand = state.selectedDemand;
    if (demand == null || demand.workOrderId.isEmpty) {
      state = state.copyWith(errorMessage: 'Nenhuma Ordem de Serviço selecionada para despacho.');
      return false;
    }

    state = state.copyWith(isDispatching: true, clearMessages: true);
    try {
      final success = await _repository.dispatchWorkOrder(demand.workOrderId, technicianId);
      if (success) {
        final tech = state.candidates.where((c) => c.technicianId == technicianId).toList();
        final techName = tech.isNotEmpty ? tech.first.technicianName : 'Técnico';

        state = state.copyWith(
          isDispatching: false,
          dispatchSuccessMessage: 'O.S. despachada com sucesso para $techName! Estoque veicular alocado.',
        );
        await loadDemands(clearMessages: false);
        return true;
      } else {
        state = state.copyWith(
          isDispatching: false,
          errorMessage: 'Não foi possível despachar a O.S.',
        );
        return false;
      }
    } catch (e) {
      state = state.copyWith(
        isDispatching: false,
        errorMessage: 'Erro no despacho: $e',
      );
      return false;
    }
  }

  void setFilterTab(int tab) {
    state = state.copyWith(selectedFilterTab: tab);
  }

  void clearMessages() {
    state = state.copyWith(clearMessages: true);
  }
}

/// Provider global para a Torre de Controle de Despacho.
final dispatchProvider = StateNotifierProvider<DispatchNotifier, DispatchState>((ref) {
  final repo = ref.watch(dispatchRepositoryProvider);
  return DispatchNotifier(repo);
});
