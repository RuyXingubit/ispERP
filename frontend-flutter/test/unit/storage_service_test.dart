import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/constants/app_constants.dart';
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

class FailingSecureStorage extends Fake implements FlutterSecureStorage {
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
    throw Exception('Keychain unavailable');
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
    throw Exception('Keychain unavailable');
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
    throw Exception('Keychain unavailable');
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

    test('Segurança: Quando FlutterSecureStorage está ativo, NÃO deve vazar accessToken ou refreshToken em texto plano no SharedPreferences', () async {
      await storageService.saveSession(
        accessToken: 'secret_jwt_access_token',
        refreshToken: 'secret_jwt_refresh_token',
        role: UserRole.admin,
        email: 'admin@provedor.com',
        name: 'Admin',
      );

      // Tokens devem ser lidos do SecureStorage, mas NUNCA gravados em texto plano no SharedPreferences
      expect(prefs.getString(AppConstants.keyAccessToken), isNull);
      expect(prefs.getString(AppConstants.keyRefreshToken), isNull);
      expect(await storageService.getAccessToken(), 'secret_jwt_access_token');
      expect(await storageService.getRefreshToken(), 'secret_jwt_refresh_token');
    });

    test('Regressão: Deve usar fallback no SharedPreferences apenas se o Keychain do SO falhar', () async {
      final fallbackService = StorageService(prefs, FailingSecureStorage());

      await fallbackService.saveSession(
        accessToken: 'fallback_access_token',
        refreshToken: 'fallback_refresh_token',
        role: UserRole.admin,
        email: 'admin@provedor.com',
        name: 'Admin',
      );

      expect(await fallbackService.getAccessToken(), 'fallback_access_token');
      expect(await fallbackService.getRefreshToken(), 'fallback_refresh_token');
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

    test('Deve detectar token JWT expirado, limpar a sessão proativamente e retornar false', () async {
      final pastEpoch = (DateTime.now().millisecondsSinceEpoch ~/ 1000) - 3600;
      final payload = base64Url.encode(utf8.encode('{"sub":"admin@nexusfibra.com.br","exp":$pastEpoch}')).replaceAll('=', '');
      final expiredJwt = 'eyJhbGciOiJIUzUxMiJ9.$payload.mockSignature';

      await storageService.saveSession(
        accessToken: expiredJwt,
        refreshToken: 'refresh_123',
        role: UserRole.admin,
        email: 'admin@nexusfibra.com.br',
        name: 'Admin Nexus',
      );

      // Deve identificar que o token expirou e retornar false
      final isValid = await storageService.hasValidSession();
      expect(isValid, isFalse);

      // Deve ter limpado a sessão automaticamente
      expect(await storageService.getAccessToken(), isNull);
      expect(storageService.getUserRole(), isNull);
    });

    test('Deve validar com sucesso token JWT dentro do prazo de validade', () async {
      final futureEpoch = (DateTime.now().millisecondsSinceEpoch ~/ 1000) + 86400;
      final payload = base64Url.encode(utf8.encode('{"sub":"admin@nexusfibra.com.br","exp":$futureEpoch}')).replaceAll('=', '');
      final validJwt = 'eyJhbGciOiJIUzUxMiJ9.$payload.mockSignature';

      await storageService.saveSession(
        accessToken: validJwt,
        refreshToken: 'refresh_123',
        role: UserRole.admin,
        email: 'admin@nexusfibra.com.br',
        name: 'Admin Nexus',
      );

      final isValid = await storageService.hasValidSession();
      expect(isValid, isTrue);
      expect(await storageService.getAccessToken(), validJwt);
    });
  });
}

