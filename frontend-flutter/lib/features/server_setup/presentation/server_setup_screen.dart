import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';

/// Tela de configuração inicial de conexão com o servidor do provedor (Self-Hosted).
class ServerSetupScreen extends ConsumerStatefulWidget {
  const ServerSetupScreen({super.key});

  @override
  ConsumerState<ServerSetupScreen> createState() => _ServerSetupScreenState();
}

class _ServerSetupScreenState extends ConsumerState<ServerSetupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _urlController = TextEditingController();
  bool _isTesting = false;
  String? _testResult;
  bool _testSuccess = false;

  @override
  void initState() {
    super.initState();
    final currentUrl = ref.read(storageServiceProvider).getServerUrl();
    if (currentUrl != null && currentUrl.isNotEmpty) {
      _urlController.text = currentUrl;
    } else {
      _urlController.text = 'http://localhost:8080';
    }
  }

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _testConnection() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) return;

    setState(() {
      _isTesting = true;
      _testResult = null;
    });

    final api = ref.read(apiClientProvider);
    final result = await api.testServerConnection(url);

    if (mounted) {
      // Se detectou que a API responde sob /api, atualiza o campo automaticamente
      if (result.isHealthy && result.resolvedBaseUrl != null) {
        _urlController.text = result.resolvedBaseUrl!;
      }
      setState(() {
        _isTesting = false;
        _testSuccess = result.isHealthy;
        _testResult = result.isHealthy
            ? 'Conexão estabelecida com sucesso! API em ${result.resolvedBaseUrl} (Status: ${result.status})'
            : (result.errorMessage ?? 'Falha ao conectar com o servidor');
      });
    }
  }

  Future<void> _saveAndConnect() async {
    if (!_formKey.currentState!.validate()) return;
    final url = _urlController.text.trim();

    final success = await ref.read(authProvider.notifier).setServerUrl(url);
    if (!mounted) return;

    if (success) {
      context.go('/login');
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(ref.read(authProvider).errorMessage ?? 'Servidor inacessível'),
          backgroundColor: AppTheme.accentError,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final savedServers = ref.read(storageServiceProvider).getSavedServers();
    final authState = ref.watch(authProvider);

    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Header & Logo
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Icon(
                              Icons.hub_outlined,
                              color: AppTheme.primaryBlue,
                              size: 32,
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: const [
                                Text(
                                  'ispERP',
                                  style: TextStyle(
                                    fontSize: 24,
                                    fontWeight: FontWeight.bold,
                                    letterSpacing: -0.5,
                                  ),
                                ),
                                Text(
                                  'Conexão com Servidor do Provedor',
                                  style: TextStyle(
                                    fontSize: 13,
                                    color: AppTheme.textSecondary,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24),
                      const Text(
                        'Informe a URL onde a instância do ispERP do seu provedor está hospedada.',
                        style: TextStyle(
                          color: AppTheme.textSecondary,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 20),

                      // Input URL
                      TextFormField(
                        controller: _urlController,
                        decoration: const InputDecoration(
                          labelText: 'URL da Instância',
                          hintText: 'http://localhost:8080 ou https://erp.provedor.com.br',
                          prefixIcon: Icon(Icons.dns_outlined, size: 20),
                        ),
                        keyboardType: TextInputType.url,
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) {
                            return 'Informe a URL do servidor';
                          }
                          final trimmed = value.trim();
                          if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) {
                            return 'A URL deve iniciar com http:// ou https://';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 12),

                      // Feedback do Teste
                      if (_testResult != null)
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: _testSuccess
                                ? AppTheme.accentGreen.withValues(alpha: 0.15)
                                : AppTheme.accentError.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: _testSuccess ? AppTheme.accentGreen : AppTheme.accentError,
                              width: 1,
                            ),
                          ),
                          child: Row(
                            children: [
                              Icon(
                                _testSuccess ? Icons.check_circle_outline : Icons.error_outline,
                                color: _testSuccess ? AppTheme.accentGreen : AppTheme.accentError,
                                size: 20,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  _testResult!,
                                  style: TextStyle(
                                    color: _testSuccess ? AppTheme.accentGreen : AppTheme.accentError,
                                    fontSize: 13,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),

                      const SizedBox(height: 16),

                      // Botões de Ação
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: _isTesting ? null : _testConnection,
                              icon: _isTesting
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(strokeWidth: 2),
                                    )
                                  : const Icon(Icons.wifi_tethering, size: 18),
                              label: Text(_isTesting ? 'Testando...' : 'Testar Conexão'),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: ElevatedButton.icon(
                              onPressed: authState.isLoading ? null : _saveAndConnect,
                              icon: authState.isLoading
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        color: Color(0xFF0A0F1D),
                                      ),
                                    )
                                  : const Icon(Icons.arrow_forward, size: 18),
                              label: const Text('Conectar'),
                            ),
                          ),
                        ],
                      ),

                      // Servidores Recentes
                      if (savedServers.isNotEmpty) ...[
                        const SizedBox(height: 24),
                        const Divider(),
                        const SizedBox(height: 12),
                        const Text(
                          'Servidores Conectados Recentemente:',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 8),
                        ...savedServers.map((server) => ListTile(
                              dense: true,
                              contentPadding: EdgeInsets.zero,
                              leading: const Icon(Icons.history, size: 18, color: AppTheme.textMuted),
                              title: Text(server, style: const TextStyle(fontSize: 13)),
                              trailing: const Icon(Icons.chevron_right, size: 18),
                              onTap: () {
                                setState(() {
                                  _urlController.text = server;
                                  _testResult = null;
                                });
                              },
                            )),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
