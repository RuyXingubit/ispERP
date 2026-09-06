import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'audit_log_model.dart';
import 'audit_log_repository.dart';

class AuditLogState {
  final AsyncValue<AuditLogPage> pageAsync;
  final String? selectedUserId;
  final String? selectedEntityName;
  final String searchAction;
  final DateTime? startDate;
  final DateTime? endDate;
  final String activePeriodFilter;
  final int currentPage;
  final int pageSize;

  AuditLogState({
    required this.pageAsync,
    this.selectedUserId,
    this.selectedEntityName,
    this.searchAction = '',
    this.startDate,
    this.endDate,
    this.activePeriodFilter = 'all',
    this.currentPage = 0,
    this.pageSize = 20,
  });

  AuditLogState copyWith({
    AsyncValue<AuditLogPage>? pageAsync,
    String? Function()? selectedUserId,
    String? Function()? selectedEntityName,
    String? searchAction,
    DateTime? Function()? startDate,
    DateTime? Function()? endDate,
    String? activePeriodFilter,
    int? currentPage,
    int? pageSize,
  }) {
    return AuditLogState(
      pageAsync: pageAsync ?? this.pageAsync,
      selectedUserId:
          selectedUserId != null ? selectedUserId() : this.selectedUserId,
      selectedEntityName: selectedEntityName != null
          ? selectedEntityName()
          : this.selectedEntityName,
      searchAction: searchAction ?? this.searchAction,
      startDate: startDate != null ? startDate() : this.startDate,
      endDate: endDate != null ? endDate() : this.endDate,
      activePeriodFilter: activePeriodFilter ?? this.activePeriodFilter,
      currentPage: currentPage ?? this.currentPage,
      pageSize: pageSize ?? this.pageSize,
    );
  }
}

final auditLogNotifierProvider =
    StateNotifierProvider<AuditLogNotifier, AuditLogState>((ref) {
  final repository = ref.watch(auditLogRepositoryProvider);
  return AuditLogNotifier(repository);
});

class AuditLogNotifier extends StateNotifier<AuditLogState> {
  final AuditLogRepository _repository;

  AuditLogNotifier(this._repository)
      : super(AuditLogState(pageAsync: const AsyncValue.loading())) {
    loadLogs();
  }

  Future<void> loadLogs() async {
    state = state.copyWith(pageAsync: const AsyncValue.loading());
    try {
      final result = await _repository.getAuditLogs(
        userId: state.selectedUserId,
        entityName: state.selectedEntityName,
        action: state.searchAction.isEmpty ? null : state.searchAction,
        startDate: state.startDate,
        endDate: state.endDate,
        page: state.currentPage,
        size: state.pageSize,
      );
      state = state.copyWith(pageAsync: AsyncValue.data(result));
    } catch (e, stack) {
      state = state.copyWith(pageAsync: AsyncValue.error(e, stack));
    }
  }

  void setUserId(String? userId) {
    if (state.selectedUserId == userId) return;
    state = state.copyWith(
      selectedUserId: () => userId,
      currentPage: 0,
    );
    loadLogs();
  }

  void setEntityName(String? entityName) {
    final value = (entityName == null || entityName.isEmpty || entityName == 'ALL')
        ? null
        : entityName;
    if (state.selectedEntityName == value) return;
    state = state.copyWith(
      selectedEntityName: () => value,
      currentPage: 0,
    );
    loadLogs();
  }

  void setSearchAction(String query) {
    state = state.copyWith(
      searchAction: query.trim(),
      currentPage: 0,
    );
    loadLogs();
  }

  void setDateRange(DateTime? start, DateTime? end) {
    state = state.copyWith(
      startDate: () => start,
      endDate: () => end,
      activePeriodFilter: 'custom',
      currentPage: 0,
    );
    loadLogs();
  }

  void setQuickPeriod(String periodKey) {
    final now = DateTime.now();
    DateTime? start;
    DateTime? end;

    switch (periodKey) {
      case 'today':
        start = DateTime(now.year, now.month, now.day, 0, 0, 0);
        end = DateTime(now.year, now.month, now.day, 23, 59, 59);
        break;
      case '7d':
        start = now.subtract(const Duration(days: 7));
        end = now;
        break;
      case '30d':
        start = now.subtract(const Duration(days: 30));
        end = now;
        break;
      case 'all':
      default:
        start = null;
        end = null;
        break;
    }

    state = state.copyWith(
      startDate: () => start,
      endDate: () => end,
      activePeriodFilter: periodKey,
      currentPage: 0,
    );
    loadLogs();
  }

  void changePage(int page) {
    if (page == state.currentPage || page < 0) return;
    state = state.copyWith(currentPage: page);
    loadLogs();
  }

  Future<void> refresh() async {
    await loadLogs();
  }
}
