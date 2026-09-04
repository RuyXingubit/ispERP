/// Constantes globais da aplicação ispERP Flutter.
class AppConstants {
  AppConstants._();

  static const String appName = 'ispERP';
  static const String appTagline = 'ERP de Missão Crítica para Provedores de Internet';

  // Storage Keys
  static const String keyServerUrl = 'isperp_server_url';
  static const String keySavedServers = 'isperp_saved_servers';
  static const String keyAccessToken = 'isperp_access_token';
  static const String keyRefreshToken = 'isperp_refresh_token';
  static const String keyUserRole = 'isperp_user_role';
  static const String keyUserEmail = 'isperp_user_email';
  static const String keyUserName = 'isperp_user_name';

  // Default values
  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);

  // Endpoints padrão
  static const String endpointHealth = '/actuator/health';
  static const String endpointLogin = '/api/auth/login';
  static const String endpointRefreshToken = '/api/auth/refresh';
  static const String endpointMe = '/api/auth/me';
}
