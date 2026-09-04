import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants/app_constants.dart';
import '../models/user_role.dart';

/// Serviço unificado de persistência do ispERP.
/// Utiliza [FlutterSecureStorage] para tokens sensíveis e [SharedPreferences] para configurações locais.
class StorageService {
  final SharedPreferences _prefs;
  final FlutterSecureStorage _secureStorage;

  StorageService(this._prefs, [this._secureStorage = const FlutterSecureStorage()]);

  // --- Gerenciamento da URL do Servidor ---

  /// Retorna a URL base do servidor configurado (ex: https://erp.meuprovedor.com.br).
  String? getServerUrl() {
    return _prefs.getString(AppConstants.keyServerUrl);
  }

  /// Salva a URL base do servidor ativo e adiciona à lista de servidores recentes.
  Future<void> setServerUrl(String url) async {
    final cleanUrl = url.trim().replaceAll(RegExp(r'/+$'), '');
    await _prefs.setString(AppConstants.keyServerUrl, cleanUrl);

    // Atualiza histórico de servidores (MRU - Mais recentemente usado)
    final saved = getSavedServers();
    saved.remove(cleanUrl);
    saved.insert(0, cleanUrl);
    if (saved.length > 5) saved.removeLast();
    await _prefs.setStringList(AppConstants.keySavedServers, saved);
  }

  /// Lista de servidores previamente conectados para alternância rápida.
  List<String> getSavedServers() {
    return _prefs.getStringList(AppConstants.keySavedServers) ?? [];
  }

  // --- Gerenciamento de Autenticação & Sessão ---

  /// Salva os tokens de sessão (Access Token e Refresh Token de forma segura).
  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    required UserRole role,
    required String email,
    required String name,
  }) async {
    try {
      await _secureStorage.write(
        key: AppConstants.keyAccessToken,
        value: accessToken,
      );
      await _secureStorage.write(
        key: AppConstants.keyRefreshToken,
        value: refreshToken,
      );
    } catch (_) {
      // Fallback para SharedPreferences caso o Keychain do OS esteja inacessível no ambiente de dev
      await _prefs.setString(AppConstants.keyAccessToken, accessToken);
      await _prefs.setString(AppConstants.keyRefreshToken, refreshToken);
    }
    await _prefs.setString(AppConstants.keyUserRole, role.name);
    await _prefs.setString(AppConstants.keyUserEmail, email);
    await _prefs.setString(AppConstants.keyUserName, name);
  }

  /// Retorna o token de acesso (Bearer JWT).
  Future<String?> getAccessToken() async {
    try {
      final token = await _secureStorage.read(key: AppConstants.keyAccessToken);
      if (token != null && token.isNotEmpty) return token;
    } catch (_) {}
    return _prefs.getString(AppConstants.keyAccessToken);
  }

  /// Retorna o token de renovação.
  Future<String?> getRefreshToken() async {
    try {
      final token = await _secureStorage.read(key: AppConstants.keyRefreshToken);
      if (token != null && token.isNotEmpty) return token;
    } catch (_) {}
    return _prefs.getString(AppConstants.keyRefreshToken);
  }

  /// Retorna o perfil (Role) do colaborador conectado.
  UserRole? getUserRole() {
    final roleStr = _prefs.getString(AppConstants.keyUserRole);
    if (roleStr == null) return null;
    return UserRole.fromString(roleStr);
  }

  /// Retorna o e-mail do usuário ativo.
  String? getUserEmail() {
    return _prefs.getString(AppConstants.keyUserEmail);
  }

  /// Retorna o nome do usuário ativo.
  String? getUserName() {
    return _prefs.getString(AppConstants.keyUserName);
  }

  /// Verifica se existe uma sessão com token salvo.
  Future<bool> hasValidSession() async {
    final token = await getAccessToken();
    return token != null && token.isNotEmpty;
  }

  /// Limpa os tokens e encerra a sessão ativa (Logout).
  Future<void> clearSession() async {
    try {
      await _secureStorage.delete(key: AppConstants.keyAccessToken);
      await _secureStorage.delete(key: AppConstants.keyRefreshToken);
    } catch (_) {}
    await _prefs.remove(AppConstants.keyAccessToken);
    await _prefs.remove(AppConstants.keyRefreshToken);
    await _prefs.remove(AppConstants.keyUserRole);
    await _prefs.remove(AppConstants.keyUserEmail);
    await _prefs.remove(AppConstants.keyUserName);
  }
}
