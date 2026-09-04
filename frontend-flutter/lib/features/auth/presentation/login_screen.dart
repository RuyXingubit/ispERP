import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/models/user_role.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';

/// Tela de autenticação dos colaboradores do ispERP.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController(text: 'admin@nexusfibra.com.br');
  final _passwordController = TextEditingController(text: 'password123');
  bool _obscurePassword = true;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;
    final username = _usernameController.text.trim();
    final password = _passwordController.text;

    final success = await ref.read(authProvider.notifier).login(username, password);
    if (!success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(ref.read(authProvider).errorMessage ?? 'Falha ao autenticar'),
          backgroundColor: AppTheme.accentError,
        ),
      );
    }
  }

  /// Atalho de desenvolvimento/demonstração para simular login direto com qualquer perfil
  Future<void> _simulateRoleLogin(UserRole role) async {
    final storage = ref.read(storageServiceProvider);
    await storage.saveSession(
      accessToken: 'mock_jwt_token_${role.name}',
      refreshToken: 'mock_refresh_token',
      role: role,
      email: '${role.name}@provedor.com.br',
      name: 'Colaborador (${role.name.toUpperCase()})',
    );
    // Força re-inicialização do AuthNotifier
    ref.invalidate(authProvider);
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final serverUrl = authState.serverUrl ?? 'Não configurado';

    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Header & Indicador de Servidor Conectado
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Icon(
                              Icons.lock_outline,
                              color: AppTheme.primaryBlue,
                              size: 28,
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'Acesso Colaborador',
                                  style: TextStyle(
                                    fontSize: 22,
                                    fontWeight: FontWeight.bold,
                                    letterSpacing: -0.5,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Row(
                                  children: [
                                    Container(
                                      width: 8,
                                      height: 8,
                                      decoration: const BoxDecoration(
                                        color: AppTheme.accentGreen,
                                        shape: BoxShape.circle,
                                      ),
                                    ),
                                    const SizedBox(width: 6),
                                    Expanded(
                                      child: Text(
                                        serverUrl,
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary,
                                        ),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                          IconButton(
                            icon: const Icon(Icons.swap_horiz, size: 20),
                            tooltip: 'Trocar Servidor',
                            onPressed: () {
                              context.go('/server-setup');
                            },
                          ),
                        ],
                      ),
                      const SizedBox(height: 28),

                      // Input Usuário / E-mail
                      TextFormField(
                        controller: _usernameController,
                        decoration: const InputDecoration(
                          labelText: 'Usuário ou E-mail',
                          prefixIcon: Icon(Icons.person_outline, size: 20),
                        ),
                        keyboardType: TextInputType.emailAddress,
                        validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe seu usuário' : null,
                      ),
                      const SizedBox(height: 16),

                      // Input Senha
                      TextFormField(
                        controller: _passwordController,
                        obscureText: _obscurePassword,
                        decoration: InputDecoration(
                          labelText: 'Senha de Acesso',
                          prefixIcon: const Icon(Icons.key_outlined, size: 20),
                          suffixIcon: IconButton(
                            icon: Icon(
                              _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                              size: 20,
                            ),
                            onPressed: () {
                              setState(() => _obscurePassword = !_obscurePassword);
                            },
                          ),
                        ),
                        validator: (v) => (v == null || v.isEmpty) ? 'Informe sua senha' : null,
                      ),
                      const SizedBox(height: 24),

                      // Botão de Login
                      ElevatedButton(
                        onPressed: authState.isLoading ? null : _handleLogin,
                        child: authState.isLoading
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Color(0xFF0A0F1D),
                                ),
                              )
                            : const Text('Entrar no Sistema'),
                      ),

                      // Seletor de Perfil Rápido para Avaliação/Testes
                      const SizedBox(height: 28),
                      const Divider(),
                      const SizedBox(height: 12),
                      const Text(
                        'Simulação Rápida por Perfil (Ambiente de Testes):',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: AppTheme.textMuted,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 12),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        alignment: WrapAlignment.center,
                        children: [
                          _buildRoleChip(UserRole.admin, 'Admin', Icons.shield_outlined),
                          _buildRoleChip(UserRole.financial, 'Financeiro', Icons.account_balance_outlined),
                          _buildRoleChip(UserRole.support, 'Suporte', Icons.headset_mic_outlined),
                          _buildRoleChip(UserRole.sales, 'Vendas', Icons.trending_up_outlined),
                          _buildRoleChip(UserRole.technician, 'Técnico', Icons.build_circle_outlined),
                        ],
                      ),
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

  Widget _buildRoleChip(UserRole role, String label, IconData icon) {
    return ActionChip(
      avatar: Icon(icon, size: 16, color: AppTheme.primaryBlue),
      label: Text(label, style: const TextStyle(fontSize: 12)),
      backgroundColor: AppTheme.darkBg,
      side: const BorderSide(color: AppTheme.darkCard),
      onPressed: () => _simulateRoleLogin(role),
    );
  }
}
