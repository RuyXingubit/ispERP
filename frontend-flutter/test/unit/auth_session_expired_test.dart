import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/models/user_role.dart';
import 'package:isperp_app/core/network/api_client.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/core/storage/storage_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late StorageService storageService;
  late ApiClient apiClient;

  setUp(() async {
    SharedPreferences.setMockInitialValues({
      'isperp_server_url': 'http://localhost:8080/api',
      'isperp_user_role': 'ADMIN',
      'isperp_user_email': 'admin@provedor.com.br',
      'isperp_user_name': 'Administrador',
    });
    final prefs = await SharedPreferences.getInstance();
    storageService = StorageService(prefs);
    apiClient = ApiClient(storageService);
  });

  test('AuthNotifier - handleSessionExpired deve deslogar e exibir mensagem de sessão expirada', () {
    final notifier = AuthNotifier(storageService, apiClient);
    apiClient.onSessionExpired = notifier.handleSessionExpired;

    // Simula estado autenticado
    notifier.state = const AuthState(
      isAuthenticated: true,
      role: UserRole.admin,
      email: 'admin@provedor.com.br',
      name: 'Administrador',
    );

    expect(notifier.state.isAuthenticated, isTrue);

    // Dispara callback de sessão expirada
    apiClient.onSessionExpired?.call();

    expect(notifier.state.isAuthenticated, isFalse);
    expect(notifier.state.role, isNull);
    expect(notifier.state.email, isNull);
    expect(notifier.state.name, isNull);
    expect(notifier.state.errorMessage, 'Sua sessão expirou. Por favor, faça login novamente.');
  });
}
