import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'user_model.dart';
import 'users_repository.dart';

@immutable
class UsersState {
  final bool isLoading;
  final bool isSubmitting;
  final List<UserModel> users;
  final String searchQuery;
  final String statusFilter; // 'ALL', 'ACTIVE', 'SUSPENDED'
  final String roleFilter;   // 'ALL' or specific role
  final String? errorMessage;
  final String? successMessage;

  const UsersState({
    this.isLoading = false,
    this.isSubmitting = false,
    this.users = const [],
    this.searchQuery = '',
    this.statusFilter = 'ALL',
    this.roleFilter = 'ALL',
    this.errorMessage,
    this.successMessage,
  });

  UsersState copyWith({
    bool? isLoading,
    bool? isSubmitting,
    List<UserModel>? users,
    String? searchQuery,
    String? statusFilter,
    String? roleFilter,
    String? errorMessage,
    String? successMessage,
    bool clearError = false,
    bool clearSuccess = false,
  }) {
    return UsersState(
      isLoading: isLoading ?? this.isLoading,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      users: users ?? this.users,
      searchQuery: searchQuery ?? this.searchQuery,
      statusFilter: statusFilter ?? this.statusFilter,
      roleFilter: roleFilter ?? this.roleFilter,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      successMessage: clearSuccess ? null : (successMessage ?? this.successMessage),
    );
  }

  /// Retorna os usuários filtrados por busca, status e cargo
  List<UserModel> get filteredUsers {
    return users.where((u) {
      // Filtro de status
      if (statusFilter == 'ACTIVE' && !u.active) return false;
      if (statusFilter == 'SUSPENDED' && u.active) return false;

      // Filtro de cargo/role
      if (roleFilter != 'ALL' && u.role.toUpperCase() != roleFilter.toUpperCase()) {
        return false;
      }

      // Filtro textual
      if (searchQuery.trim().isNotEmpty) {
        final q = searchQuery.toLowerCase().trim();
        final nameMatch = u.name.toLowerCase().contains(q);
        final emailMatch = u.email.toLowerCase().contains(q);
        final cpfMatch = u.cpf?.toLowerCase().contains(q) ?? false;
        final roleMatch = u.role.toLowerCase().contains(q) || u.roleDisplay.toLowerCase().contains(q);
        return nameMatch || emailMatch || cpfMatch || roleMatch;
      }

      return true;
    }).toList();
  }

  int get totalActive => users.where((u) => u.active).length;
  int get totalSuspended => users.where((u) => !u.active).length;
}

final usersProvider = StateNotifierProvider<UsersNotifier, UsersState>((ref) {
  final repository = ref.watch(usersRepositoryProvider);
  return UsersNotifier(repository);
});

class UsersNotifier extends StateNotifier<UsersState> {
  final UsersRepository _repository;

  UsersNotifier(this._repository) : super(const UsersState()) {
    loadUsers();
  }

  Future<void> loadUsers() async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final list = await _repository.listUsers();
      state = state.copyWith(isLoading: false, users: list);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Falha ao carregar colaboradores: ${e.toString()}',
      );
    }
  }

  void setSearchQuery(String query) {
    state = state.copyWith(searchQuery: query);
  }

  void setStatusFilter(String filter) {
    state = state.copyWith(statusFilter: filter);
  }

  void setRoleFilter(String filter) {
    state = state.copyWith(roleFilter: filter);
  }

  void clearMessages() {
    state = state.copyWith(clearError: true, clearSuccess: true);
  }

  Future<bool> createUser({
    required String name,
    required String email,
    required String password,
    required String role,
    String? cpf,
  }) async {
    state = state.copyWith(isSubmitting: true, clearError: true, clearSuccess: true);
    try {
      final created = await _repository.createUser(
        name: name,
        email: email,
        password: password,
        role: role,
        cpf: cpf,
        active: true,
      );
      state = state.copyWith(
        isSubmitting: false,
        users: [created, ...state.users],
        successMessage: 'Colaborador "${created.name}" cadastrado com sucesso!',
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Falha ao cadastrar colaborador: ${e.toString()}',
      );
      return false;
    }
  }

  Future<bool> toggleUserStatus(String id, bool newStatus) async {
    state = state.copyWith(isSubmitting: true, clearError: true, clearSuccess: true);
    try {
      final updated = await _repository.updateUserStatus(id: id, active: newStatus);
      final updatedList = state.users.map((u) => u.id == id ? updated : u).toList();
      state = state.copyWith(
        isSubmitting: false,
        users: updatedList,
        successMessage: newStatus
            ? 'Acesso de "${updated.name}" reativado com sucesso!'
            : 'Acesso de "${updated.name}" suspenso.',
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Falha ao alterar status do usuário: ${e.toString()}',
      );
      return false;
    }
  }

  Future<bool> updateUserRole(String id, String newRole) async {
    state = state.copyWith(isSubmitting: true, clearError: true, clearSuccess: true);
    try {
      final updated = await _repository.updateUserRole(id: id, role: newRole);
      final updatedList = state.users.map((u) => u.id == id ? updated : u).toList();
      state = state.copyWith(
        isSubmitting: false,
        users: updatedList,
        successMessage: 'Perfil de "${updated.name}" alterado para ${updated.roleDisplay}!',
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Falha ao alterar perfil do usuário: ${e.toString()}',
      );
      return false;
    }
  }

  Future<bool> resetPassword(String id, String newPassword) async {
    state = state.copyWith(isSubmitting: true, clearError: true, clearSuccess: true);
    try {
      final updated = await _repository.resetPassword(id: id, newPassword: newPassword);
      state = state.copyWith(
        isSubmitting: false,
        successMessage: 'Senha de "${updated.name}" redefinida com sucesso!',
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Falha ao redefinir senha: ${e.toString()}',
      );
      return false;
    }
  }

  Future<bool> deleteUser(String id) async {
    state = state.copyWith(isSubmitting: true, clearError: true, clearSuccess: true);
    try {
      await _repository.deleteUser(id);
      final updatedList = state.users.where((u) => u.id != id).toList();
      state = state.copyWith(
        isSubmitting: false,
        users: updatedList,
        successMessage: 'Colaborador removido do sistema.',
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Falha ao remover colaborador: ${e.toString()}',
      );
      return false;
    }
  }
}
