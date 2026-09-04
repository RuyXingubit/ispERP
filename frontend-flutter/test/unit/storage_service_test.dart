import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/models/user_role.dart';
import 'package:isperp_app/core/storage/storage_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class FakeSecureStorage extends Fake implements FlutterSecureStorage {
  final Map<String, String> _store = {};

  @override
  Future<void> write({
    required String key,
    required String? value,
    IOSOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    MacOsOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    if (value != null) {
      _store[key] = value;
    }
  }

  @override
  Future<String?> read({
    required String key,
    IOSOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    MacOsOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    return _store[key];
  }

  @override
  Future<void> delete({
    required String key,
    IOSOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    MacOsOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    _store.remove(key);
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SharedPreferences prefs;
  late FakeSecureStorage secureStorage;
  late StorageService storageService;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    prefs = await SharedPreferences.getInstance();
    secureStorage = FakeSecureStorage();
    storageService = StorageService(prefs, secureStorage);
  });

  group('StorageService Unit Tests', () {
    test('Deve salvar e retornar a URL do servidor corretamente, removendo barras finais', () async {
      await storageService.setServerUrl('https://erp.provedor.com.br///');

      expect(storageService.getServerUrl(), 'https://erp.provedor.com.br');
      expect(storageService.getSavedServers(), contains('https://erp.provedor.com.br'));
    });

    test('Deve manter histórico dos últimos 5 servidores conectados sem duplicatas', () async {
      await storageService.setServerUrl('https://srv1.com');
      await storageService.setServerUrl('https://srv2.com');
      await storageService.setServerUrl('https://srv3.com');
      await storageService.setServerUrl('https://srv1.com');

      final list = storageService.getSavedServers();
      expect(list.length, 3);
      expect(list.first, 'https://srv1.com');
    });

    test('Deve salvar sessão do usuário com tokens e credenciais', () async {
      await storageService.saveSession(
        accessToken: 'jwt_access_123',
        refreshToken: 'jwt_refresh_456',
        role: UserRole.technician,
        email: 'tecnico@provedor.com',
        name: 'João Técnico',
      );

      expect(await storageService.getAccessToken(), 'jwt_access_123');
      expect(await storageService.getRefreshToken(), 'jwt_refresh_456');
      expect(storageService.getUserRole(), UserRole.technician);
      expect(storageService.getUserEmail(), 'tecnico@provedor.com');
      expect(storageService.getUserName(), 'João Técnico');
      expect(await storageService.hasValidSession(), isTrue);
    });

    test('Deve limpar a sessão completamente no logout', () async {
      await storageService.saveSession(
        accessToken: 'jwt_access_123',
        refreshToken: 'jwt_refresh_456',
        role: UserRole.admin,
        email: 'admin@provedor.com',
        name: 'Administrador',
      );

      await storageService.clearSession();

      expect(await storageService.getAccessToken(), isNull);
      expect(await storageService.getRefreshToken(), isNull);
      expect(storageService.getUserRole(), isNull);
      expect(await storageService.hasValidSession(), isFalse);
    });
  });
}
