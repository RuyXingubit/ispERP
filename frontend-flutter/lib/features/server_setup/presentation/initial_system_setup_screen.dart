import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';
import '../data/initial_setup_repository.dart';

/// Tela do Assistente de Primeiro Acesso (Setup Wizard) em instâncias virgens de Produção.
class InitialSystemSetupScreen extends ConsumerStatefulWidget {
  const InitialSystemSetupScreen({super.key});

  @override
  ConsumerState<InitialSystemSetupScreen> createState() => _InitialSystemSetupScreenState();
}

class _InitialSystemSetupScreenState extends ConsumerState<InitialSystemSetupScreen> {
  final _formKey = GlobalKey<FormState>();

  // Dados da Empresa
  final _companyNameController = TextEditingController();
  final _companyCnpjController = TextEditingController();
  final _companyPhoneController = TextEditingController();
  final _companyEmailController = TextEditingController();
  final _companyAddressController = TextEditingController();

  // Dados do Administrador
  final _adminNameController = TextEditingController();
  final _adminEmailController = TextEditingController();
  final _adminPasswordController = TextEditingController();
  final _adminConfirmPasswordController = TextEditingController();

  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  bool _isLoading = false;

  @override
  void dispose() {
    _companyNameController.dispose();
    _companyCnpjController.dispose();
    _companyPhoneController.dispose();
    _companyEmailController.dispose();
    _companyAddressController.dispose();
    _adminNameController.dispose();
    _adminEmailController.dispose();
    _adminPasswordController.dispose();
    _adminConfirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _submitSetup() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final repo = ref.read(initialSetupRepositoryProvider);
      final data = InitialSetupData(
        companyName: _companyNameController.text.trim(),
        companyCnpj: _companyCnpjController.text.trim(),
        companyPhone: _companyPhoneController.text.trim(),
        companyEmail: _companyEmailController.text.trim(),
        companyAddress: _companyAddressController.text.trim(),
        adminName: _adminNameController.text.trim(),
        adminEmail: _adminEmailController.text.trim(),
        adminPassword: _adminPasswordController.text,
      );

      await repo.performSetup(data);

      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Configuração inicial concluída com sucesso! Faça login com as credenciais criadas.'),
          backgroundColor: AppTheme.accentGreen,
          duration: Duration(seconds: 4),
        ),
      );

      context.go('/login');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Erro ao finalizar configuração: ${e.toString()}'),
          backgroundColor: AppTheme.accentError,
        ),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 32),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 580),
              child: Card(
                color: AppTheme.darkSurface,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                  side: const BorderSide(color: AppTheme.darkCard, width: 1),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(32.0),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Cabeçalho
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: const Icon(
                                Icons.rocket_launch_outlined,
                                color: AppTheme.primaryBlue,
                                size: 30,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: const [
                                  Text(
                                    'Inicialização do Provedor',
                                    style: TextStyle(
                                      fontSize: 22,
                                      fontWeight: FontWeight.bold,
                                      letterSpacing: -0.5,
                                    ),
                                  ),
                                  SizedBox(height: 2),
                                  Text(
                                    'Assistente de Primeiro Acesso (Setup de Produção)',
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
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: AppTheme.primaryBlue.withValues(alpha: 0.08),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.2)),
                          ),
                          child: const Text(
                            'Esta instância está em estado virgem. Cadastre os dados reais da sua empresa e crie a conta do Administrador principal. Uma vez finalizado, este assistente será bloqueado permanentemente.',
                            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary, height: 1.4),
                          ),
                        ),
                        const SizedBox(height: 24),

                        // SEÇÃO 1: DADOS DA EMPRESA
                        const Text(
                          '1. Dados da Empresa (Provedor)',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.primaryBlue,
                          ),
                        ),
                        const SizedBox(height: 14),

                        TextFormField(
                          controller: _companyNameController,
                          decoration: const InputDecoration(
                            labelText: 'Razão Social / Nome Fantasia *',
                            prefixIcon: Icon(Icons.business_outlined, size: 20),
                          ),
                          validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe a Razão Social' : null,
                        ),
                        const SizedBox(height: 12),

                        TextFormField(
                          controller: _companyCnpjController,
                          decoration: const InputDecoration(
                            labelText: 'CNPJ do Provedor *',
                            prefixIcon: Icon(Icons.badge_outlined, size: 20),
                            hintText: '00.000.000/0000-00',
                          ),
                          keyboardType: TextInputType.number,
                          validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe o CNPJ' : null,
                        ),
                        const SizedBox(height: 12),

                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: _companyPhoneController,
                                decoration: const InputDecoration(
                                  labelText: 'Telefone / WhatsApp',
                                  prefixIcon: Icon(Icons.phone_outlined, size: 20),
                                ),
                                keyboardType: TextInputType.phone,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: _companyEmailController,
                                decoration: const InputDecoration(
                                  labelText: 'E-mail Comercial',
                                  prefixIcon: Icon(Icons.alternate_email, size: 20),
                                ),
                                keyboardType: TextInputType.emailAddress,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        TextFormField(
                          controller: _companyAddressController,
                          decoration: const InputDecoration(
                            labelText: 'Endereço da Sede (Opcional)',
                            prefixIcon: Icon(Icons.location_on_outlined, size: 20),
                          ),
                        ),
                        const SizedBox(height: 28),

                        // SEÇÃO 2: ADMINISTRADOR PRINCIPAL
                        const Text(
                          '2. Administrador Principal (Super Admin)',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.primaryBlue,
                          ),
                        ),
                        const SizedBox(height: 14),

                        TextFormField(
                          controller: _adminNameController,
                          decoration: const InputDecoration(
                            labelText: 'Nome Completo do Administrador *',
                            prefixIcon: Icon(Icons.person_outline, size: 20),
                          ),
                          validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe o nome do admin' : null,
                        ),
                        const SizedBox(height: 12),

                        TextFormField(
                          controller: _adminEmailController,
                          decoration: const InputDecoration(
                            labelText: 'E-mail de Login do Administrador *',
                            prefixIcon: Icon(Icons.mail_outline, size: 20),
                          ),
                          keyboardType: TextInputType.emailAddress,
                          validator: (v) {
                            if (v == null || v.trim().isEmpty) return 'Informe o e-mail do admin';
                            if (!v.contains('@') || !v.contains('.')) return 'Informe um e-mail válido';
                            return null;
                          },
                        ),
                        const SizedBox(height: 12),

                        TextFormField(
                          controller: _adminPasswordController,
                          obscureText: _obscurePassword,
                          decoration: InputDecoration(
                            labelText: 'Senha de Acesso Mestre *',
                            prefixIcon: const Icon(Icons.lock_outline, size: 20),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                                size: 20,
                              ),
                              onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                            ),
                          ),
                          validator: (v) {
                            if (v == null || v.isEmpty) return 'Informe a senha';
                            if (v.length < 6) return 'A senha deve ter no mínimo 6 caracteres';
                            return null;
                          },
                        ),
                        const SizedBox(height: 12),

                        TextFormField(
                          controller: _adminConfirmPasswordController,
                          obscureText: _obscureConfirmPassword,
                          decoration: InputDecoration(
                            labelText: 'Confirme a Senha Mestre *',
                            prefixIcon: const Icon(Icons.lock_reset, size: 20),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscureConfirmPassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                                size: 20,
                              ),
                              onPressed: () => setState(() => _obscureConfirmPassword = !_obscureConfirmPassword),
                            ),
                          ),
                          validator: (v) {
                            if (v != _adminPasswordController.text) {
                              return 'As senhas não coincidem';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 28),

                        // BOTÃO SUBMIT
                        ElevatedButton.icon(
                          style: ElevatedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            backgroundColor: AppTheme.primaryBlue,
                            foregroundColor: Colors.white,
                          ),
                          onPressed: _isLoading ? null : _submitSetup,
                          icon: _isLoading
                              ? const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                )
                              : const Icon(Icons.check_circle_outline, size: 20),
                          label: Text(
                            _isLoading ? 'Inicializando Sistema...' : 'Concluir Setup e Iniciar ispERP',
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                          ),
                        ),
                        const SizedBox(height: 12),

                        TextButton(
                          onPressed: () => context.go('/login'),
                          child: const Text('Já possui credenciais? Ir para o Login'),
                        ),
                      ],
                    ),
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
