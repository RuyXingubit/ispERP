import 'package:dio/dio.dart';
import '../constants/app_constants.dart';
import '../storage/storage_service.dart';

/// Resultado de teste de conectividade com o servidor do provedor.
class ServerHealthResult {
  final bool isHealthy;
  final String? status;
  final String? version;
  final String? errorMessage;

  ServerHealthResult({
    required this.isHealthy,
    this.status,
    this.version,
    this.errorMessage,
  });
}

/// Cliente HTTP unificado com suporte a base URL dinâmica e interceptor de autenticação JWT.
class ApiClient {
  final StorageService _storageService;
  late final Dio dio;

  ApiClient(this._storageService) {
    dio = Dio(
      BaseOptions(
        connectTimeout: AppConstants.connectTimeout,
        receiveTimeout: AppConstants.receiveTimeout,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // Interceptor para injeção de Bearer Token e logging de erros
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Atualiza base URL caso tenha mudado no StorageService
          final currentBaseUrl = _storageService.getServerUrl();
          if (currentBaseUrl != null && currentBaseUrl.isNotEmpty) {
            if (!options.path.startsWith('http')) {
              options.baseUrl = currentBaseUrl;
            }
          }

          // Injeta token JWT se o endpoint não for público
          final token = await _storageService.getAccessToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }

          return handler.next(options);
        },
        onError: (DioException error, handler) async {
          // Se receber 401 (Não autorizado), limpa sessão para forçar novo login
          if (error.response?.statusCode == 401) {
            await _storageService.clearSession();
          }
          return handler.next(error);
        },
      ),
    );
  }

  /// Testa a conectividade e a saúde do servidor informado pelo usuário.
  Future<ServerHealthResult> testServerConnection(String url) async {
    final cleanUrl = url.trim().replaceAll(RegExp(r'/+$'), '');
    final testDio = Dio(
      BaseOptions(
        connectTimeout: const Duration(seconds: 5),
        receiveTimeout: const Duration(seconds: 5),
      ),
    );

    try {
      final response = await testDio.get('$cleanUrl${AppConstants.endpointHealth}');
      if (response.statusCode == 200) {
        final data = response.data;
        String status = 'UP';
        String? version;
        if (data is Map<String, dynamic>) {
          status = data['status']?.toString() ?? 'UP';
          version = data['version']?.toString();
        }
        return ServerHealthResult(
          isHealthy: true,
          status: status,
          version: version,
        );
      } else {
        return ServerHealthResult(
          isHealthy: false,
          errorMessage: 'Servidor respondeu com código HTTP ${response.statusCode}',
        );
      }
    } on DioException catch (e) {
      String message = 'Não foi possível conectar ao servidor.';
      if (e.type == DioExceptionType.connectionTimeout ||
          e.type == DioExceptionType.receiveTimeout) {
        message = 'Tempo limite esgotado. Verifique se o servidor está online.';
      } else if (e.type == DioExceptionType.connectionError) {
        message = 'Erro de rede ou endereço inacessível. Confira a URL e o protocolo (https://).';
      }
      return ServerHealthResult(
        isHealthy: false,
        errorMessage: message,
      );
    } catch (e) {
      return ServerHealthResult(
        isHealthy: false,
        errorMessage: e.toString(),
      );
    }
  }
}
