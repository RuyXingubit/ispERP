import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user_role.dart';
import '../network/api_client.dart';
import '../storage/storage_service.dart';

/// Provider para a instância de SharedPreferences inicializada na main.
final sharedPreferencesProvider = Provider<SharedPreferences>((ref) {
  throw UnimplementedError('SharedPreferences precisa ser inicializado no main');
});

/// Provider do serviço de persistência segura e local.
final storageServiceProvider = Provider<StorageService>((ref) {
  final prefs = ref.watch(sharedPreferencesProvider);
  return StorageService(prefs);
});

/// Provider do cliente HTTP Dio configurado com Base URL e Auth Interceptors.
final apiClientProvider = Provider<ApiClient>((ref) {
  final storage = ref.watch(storageServiceProvider);
  return ApiClient(storage);
});

/// Estado da sessão e autenticação no ispERP.
class AuthState {
  final bool isLoading;
  final bool hasServerConfigured;
  final bool isAuthenticated;
  final UserRole? role;
  final String? email;
  final String? name;
  final String? serverUrl;
  final String? errorMessage;

  const AuthState({
    this.isLoading = false,
    this.hasServerConfigured = false,
    this.isAuthenticated = false,
    this.role,
    this.email,
    this.name,
    this.serverUrl,
    this.errorMessage,
  });

  AuthState copyWith({
    bool? isLoading,
    bool? hasServerConfigured,
    bool? isAuthenticated,
    UserRole? role,
    String? email,
    String? name,
    String? serverUrl,
    String? errorMessage,
  }) {
    return AuthState(
      isLoading: isLoading ?? this.isLoading,
      hasServerConfigured: hasServerConfigured ?? this.hasServerConfigured,
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      role: role ?? this.role,
      email: email ?? this.email,
      name: name ?? this.name,
      serverUrl: serverUrl ?? this.serverUrl,
      errorMessage: errorMessage,
    );
  }
}

/// Notifier de gerenciamento do ciclo de vida de autenticação e servidor.
class AuthNotifier extends StateNotifier<AuthState> {
  final StorageService _storage;
  final ApiClient _apiClient;

  AuthNotifier(this._storage, this._apiClient) : super(const AuthState(isLoading: true)) {
    _init();
  }

  Future<void> _init() async {
    final serverUrl = _storage.getServerUrl();
    final hasServer = serverUrl != null && serverUrl.isNotEmpty;
    final hasSession = await _storage.hasValidSession();
    final role = _storage.getUserRole();
    final email = _storage.getUserEmail();
    final name = _storage.getUserName();

    state = AuthState(
      isLoading: false,
      hasServerConfigured: hasServer,
      isAuthenticated: hasSession,
      role: role,
      email: email,
      name: name,
      serverUrl: serverUrl,
    );
  }

  /// Salva a URL do servidor após validar sua saúde.
  Future<bool> setServerUrl(String url) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    final health = await _apiClient.testServerConnection(url);
    if (!health.isHealthy) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: health.errorMessage ?? 'Falha ao conectar com o servidor.',
      );
      return false;
    }

    await _storage.setServerUrl(url);
    state = state.copyWith(
      isLoading: false,
      hasServerConfigured: true,
      serverUrl: url,
    );
    return true;
  }

  /// Efetua login com usuário e senha contra a API central.
  Future<bool> login(String username, String password) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final response = await _apiClient.dio.post(
        '/api/auth/login',
        data: {
          'username': username,
          'password': password,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data as Map<String, dynamic>;
        final token = data['token'] ?? data['accessToken'] ?? '';
        final refreshToken = data['refreshToken'] ?? '';
        final roleStr = data['role'] ?? (data['roles'] is List ? (data['roles'] as List).firstOrNull : 'SUPPORT');
        final role = UserRole.fromString(roleStr?.toString());
        final email = data['email']?.toString() ?? username;
        final name = data['name']?.toString() ?? username;

        await _storage.saveSession(
          accessToken: token.toString(),
          refreshToken: refreshToken.toString(),
          role: role,
          email: email,
          name: name,
        );

        state = state.copyWith(
          isLoading: false,
          isAuthenticated: true,
          role: role,
          email: email,
          name: name,
        );
        return true;
      } else {
        state = state.copyWith(
          isLoading: false,
          errorMessage: 'Credenciais inválidas.',
        );
        return false;
      }
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Erro ao autenticar: $e',
      );
      return false;
    }
  }

  /// Desconecta o colaborador e limpa a sessão.
  Future<void> logout() async {
    await _storage.clearSession();
    state = state.copyWith(
      isAuthenticated: false,
      role: null,
      email: null,
      name: null,
    );
  }

  /// Limpa as configurações de servidor para apontar para outro host.
  Future<void> disconnectServer() async {
    await logout();
    // Limpa também a URL do servidor
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('isperp_server_url');
    state = const AuthState(
      isLoading: false,
      hasServerConfigured: false,
      isAuthenticated: false,
    );
  }
}

/// Provider do estado de autenticação.
final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  final storage = ref.watch(storageServiceProvider);
  final api = ref.watch(apiClientProvider);
  return AuthNotifier(storage, api);
});
