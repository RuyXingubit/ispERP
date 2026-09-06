import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants/app_constants.dart';
import '../models/user_role.dart';

/// Serviço unificado de persistência do ispERP.
/// Utiliza [FlutterSecureStorage] para tokens sensíveis e [SharedPreferences] para configurações locais.
class StorageService {
  final SharedPreferences _prefs;
  final FlutterSecureStorage _secureStorage;

  StorageService(
    this._prefs, [
    this._secureStorage = const FlutterSecureStorage(
      mOptions: MacOsOptions(useDataProtectionKeyChain: false),
    ),
  ]);

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
      ).timeout(const Duration(milliseconds: 300));
      await _secureStorage.write(
        key: AppConstants.keyRefreshToken,
        value: refreshToken,
      ).timeout(const Duration(milliseconds: 300));
    } catch (_) {
      // Fallback gracioso para SharedPreferences caso o Keychain do OS demore ou esteja inacessível
    }
    await _prefs.setString(AppConstants.keyAccessToken, accessToken);
    await _prefs.setString(AppConstants.keyRefreshToken, refreshToken);
    await _prefs.setString(AppConstants.keyUserRole, role.name);
    await _prefs.setString(AppConstants.keyUserEmail, email);
    await _prefs.setString(AppConstants.keyUserName, name);
  }

  /// Retorna o token de acesso (Bearer JWT).
  Future<String?> getAccessToken() async {
    final prefsToken = _prefs.getString(AppConstants.keyAccessToken);
    if (prefsToken == null || prefsToken.isEmpty) {
      return null;
    }
    try {
      final token = await _secureStorage
          .read(key: AppConstants.keyAccessToken)
          .timeout(const Duration(milliseconds: 300));
      if (token != null && token.isNotEmpty) return token;
    } catch (_) {}
    return prefsToken;
  }

  /// Retorna o token de renovação.
  Future<String?> getRefreshToken() async {
    final prefsToken = _prefs.getString(AppConstants.keyRefreshToken);
    if (prefsToken == null || prefsToken.isEmpty) {
      return null;
    }
    try {
      final token = await _secureStorage
          .read(key: AppConstants.keyRefreshToken)
          .timeout(const Duration(milliseconds: 300));
      if (token != null && token.isNotEmpty) return token;
    } catch (_) {}
    return prefsToken;
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

  /// Verifica se um token JWT está expirado com base na claim 'exp'.
  /// Retorna false se o token for válido e não expirado, ou se não for um JWT padrão com claim 'exp'.
  /// Retorna true se contiver a claim 'exp' e o tempo atual for posterior à data de expiração.
  static bool isJwtExpired(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return false;
      final payloadNormalized = base64Url.normalize(parts[1]);
      final payloadString = utf8.decode(base64Url.decode(payloadNormalized));
      final dynamic decoded = jsonDecode(payloadString);
      if (decoded is Map<String, dynamic> && decoded.containsKey('exp')) {
        final exp = decoded['exp'];
        if (exp is num) {
          final expDate = DateTime.fromMillisecondsSinceEpoch((exp * 1000).toInt());
          return DateTime.now().isAfter(expDate);
        }
      }
      return false;
    } catch (_) {
      return true;
    }
  }

  /// Verifica se existe uma sessão com token salvo e não expirado.
  /// Caso o token esteja vencido, limpa a sessão proativamente e retorna false.
  Future<bool> hasValidSession() async {
    final token = await getAccessToken();
    if (token == null || token.isEmpty) return false;

    if (isJwtExpired(token)) {
      await clearSession();
      return false;
    }

    return true;
  }

  /// Limpa os tokens e encerra a sessão ativa (Logout).
  Future<void> clearSession() async {
    try {
      await _secureStorage
          .delete(key: AppConstants.keyAccessToken)
          .timeout(const Duration(milliseconds: 300));
      await _secureStorage
          .delete(key: AppConstants.keyRefreshToken)
          .timeout(const Duration(milliseconds: 300));
    } catch (_) {}
    await _prefs.remove(AppConstants.keyAccessToken);
    await _prefs.remove(AppConstants.keyRefreshToken);
    await _prefs.remove(AppConstants.keyUserRole);
    await _prefs.remove(AppConstants.keyUserEmail);
    await _prefs.remove(AppConstants.keyUserName);
  }
}
