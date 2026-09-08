import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../data/payment_gateway_model.dart';
import '../data/payment_gateway_notifier.dart';

/// Tela de Configuração de Gateways de Pagamento (Foco primário: Xingubit Pay).
class PaymentGatewayConfigScreen extends ConsumerStatefulWidget {
  const PaymentGatewayConfigScreen({super.key});

  @override
  ConsumerState<PaymentGatewayConfigScreen> createState() => _PaymentGatewayConfigScreenState();
}

class _PaymentGatewayConfigScreenState extends ConsumerState<PaymentGatewayConfigScreen> {
  final _formKey = GlobalKey<FormState>();

  final _nameController = TextEditingController();
  final _apiKeyController = TextEditingController();
  final _secretKeyController = TextEditingController();
  final _webhookSecretController = TextEditingController();
  final _pixKeyController = TextEditingController();

  bool _sandbox = false;
  bool _active = true;
  bool _obscureApiKey = true;
  bool _obscureSecretKey = true;
  bool _obscureWebhookSecret = true;
  bool _isSaving = false;
  PaymentGatewayModel? _currentConfig;

  @override
  void dispose() {
    _nameController.dispose();
    _apiKeyController.dispose();
    _secretKeyController.dispose();
    _webhookSecretController.dispose();
    _pixKeyController.dispose();
    super.dispose();
  }

  void _populateFields(PaymentGatewayModel config) {
    _currentConfig = config;
    _nameController.text = config.name;
    _apiKeyController.text = config.apiKey ?? '';
    _secretKeyController.text = config.secretKey ?? '';
    _webhookSecretController.text = config.webhookSecret ?? '';
    _pixKeyController.text = config.pixKey ?? '';
    _sandbox = config.sandbox;
    _active = config.active;
  }

  Future<void> _saveConfig() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSaving = true);

    try {
      final modelToSave = PaymentGatewayModel(
        id: _currentConfig?.id,
        companyId: _currentConfig?.companyId,
        gatewayType: PaymentGatewayType.xingubitPay,
        name: _nameController.text.trim().isNotEmpty ? _nameController.text.trim() : 'Xingubit Pay Principal',
        apiKey: _apiKeyController.text.trim(),
        secretKey: _secretKeyController.text.trim(),
        webhookSecret: _webhookSecretController.text.trim(),
        pixKey: _pixKeyController.text.trim(),
        sandbox: _sandbox,
        active: _active,
      );

      await ref.read(paymentGatewaysProvider.notifier).saveConfig(modelToSave);

      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Configurações do Xingubit Pay salvas com sucesso!'),
          backgroundColor: AppTheme.accentGreen,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Falha ao salvar configurações: ${e.toString()}'),
          backgroundColor: AppTheme.accentError,
        ),
      );
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final gatewaysAsync = ref.watch(paymentGatewaysProvider);
    final serverUrl = ref.watch(authProvider).serverUrl ?? 'https://seu-provedor.com.br';
    final cleanServerUrl = serverUrl.replaceAll(RegExp(r'/+$'), '');
    final webhookEndpointUrl = '$cleanServerUrl/api/webhooks/xingubit';

    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Cabeçalho
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text(
                        'Gateway de Pagamento & Cobrança',
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Gerenciamento de credenciais do Xingubit Pay para geração de Pix dinâmico e conciliação bancária',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                OutlinedButton.icon(
                  onPressed: () => ref.read(paymentGatewaysProvider.notifier).loadConfigs(),
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('Recarregar'),
                ),
              ],
            ),
            const SizedBox(height: 24),

            gatewaysAsync.when(
              loading: () => const Center(
                child: Padding(
                  padding: EdgeInsets.all(48.0),
                  child: CircularProgressIndicator(),
                ),
              ),
              error: (err, _) => Card(
                color: AppTheme.accentError.withValues(alpha: 0.1),
                child: Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: Row(
                    children: [
                      const Icon(Icons.error_outline, color: AppTheme.accentError),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Erro ao carregar configurações de pagamento: $err',
                          style: const TextStyle(color: AppTheme.accentError),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              data: (configs) {
                // Se ainda não populou os campos e há configuração do Xingubit Pay
                final xingubitConfig = configs.firstWhere(
                  (c) => c.gatewayType == PaymentGatewayType.xingubitPay,
                  orElse: () => PaymentGatewayModel(
                    gatewayType: PaymentGatewayType.xingubitPay,
                    name: 'Xingubit Pay Principal',
                  ),
                );

                if (_currentConfig == null && xingubitConfig.id != null) {
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (mounted) _populateFields(xingubitConfig);
                  });
                }

                return ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 860),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // CARD XINGUBIT PAY
                      Card(
                        color: AppTheme.darkSurface,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                          side: const BorderSide(color: AppTheme.darkCard),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(28.0),
                          child: Form(
                            key: _formKey,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // Header do Card Xingubit Pay
                                Row(
                                  children: [
                                    Container(
                                      padding: const EdgeInsets.all(10),
                                      decoration: BoxDecoration(
                                        color: AppTheme.accentGreen.withValues(alpha: 0.15),
                                        borderRadius: BorderRadius.circular(10),
                                      ),
                                      child: const Icon(
                                        Icons.payments_outlined,
                                        color: AppTheme.accentGreen,
                                        size: 26,
                                      ),
                                    ),
                                    const SizedBox(width: 14),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Row(
                                            children: [
                                              const Text(
                                                'Xingubit Pay',
                                                style: TextStyle(
                                                  fontSize: 18,
                                                  fontWeight: FontWeight.bold,
                                                ),
                                              ),
                                              const SizedBox(width: 10),
                                              Container(
                                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                                decoration: BoxDecoration(
                                                  color: _active
                                                      ? AppTheme.accentGreen.withValues(alpha: 0.15)
                                                      : AppTheme.textMuted.withValues(alpha: 0.15),
                                                  borderRadius: BorderRadius.circular(12),
                                                ),
                                                child: Text(
                                                  _active ? 'ATIVO' : 'INATIVO',
                                                  style: TextStyle(
                                                    color: _active ? AppTheme.accentGreen : AppTheme.textMuted,
                                                    fontSize: 10,
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                ),
                                              ),
                                              if (_sandbox) ...[
                                                const SizedBox(width: 6),
                                                Container(
                                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                                  decoration: BoxDecoration(
                                                    color: AppTheme.accentWarning.withValues(alpha: 0.15),
                                                    borderRadius: BorderRadius.circular(12),
                                                  ),
                                                  child: const Text(
                                                    'SANDBOX',
                                                    style: TextStyle(
                                                      color: AppTheme.accentWarning,
                                                      fontSize: 10,
                                                      fontWeight: FontWeight.bold,
                                                    ),
                                                  ),
                                                ),
                                              ],
                                            ],
                                          ),
                                          const SizedBox(height: 2),
                                          const Text(
                                            'Gateway oficial de emissão Pix instantâneo, boletos e baixa automática',
                                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 24),
                                const Divider(),
                                const SizedBox(height: 16),

                                // Nome da Identificação
                                TextFormField(
                                  controller: _nameController,
                                  decoration: const InputDecoration(
                                    labelText: 'Nome da Integração *',
                                    hintText: 'Ex: Xingubit Pay Conta Principal',
                                    prefixIcon: Icon(Icons.label_outline, size: 20),
                                  ),
                                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe um nome' : null,
                                ),
                                const SizedBox(height: 16),

                                // API Key
                                TextFormField(
                                  controller: _apiKeyController,
                                  obscureText: _obscureApiKey,
                                  decoration: InputDecoration(
                                    labelText: 'Chave de API (API Key) *',
                                    prefixIcon: const Icon(Icons.vpn_key_outlined, size: 20),
                                    suffixIcon: IconButton(
                                      icon: Icon(_obscureApiKey ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 20),
                                      onPressed: () => setState(() => _obscureApiKey = !_obscureApiKey),
                                    ),
                                  ),
                                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe a API Key fornecida pela Xingubit' : null,
                                ),
                                const SizedBox(height: 16),

                                // Secret Key
                                TextFormField(
                                  controller: _secretKeyController,
                                  obscureText: _obscureSecretKey,
                                  decoration: InputDecoration(
                                    labelText: 'Chave Secreta (Secret Key) *',
                                    prefixIcon: const Icon(Icons.lock_outline, size: 20),
                                    suffixIcon: IconButton(
                                      icon: Icon(_obscureSecretKey ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 20),
                                      onPressed: () => setState(() => _obscureSecretKey = !_obscureSecretKey),
                                    ),
                                  ),
                                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe a Secret Key' : null,
                                ),
                                const SizedBox(height: 16),

                                // Chave Pix
                                TextFormField(
                                  controller: _pixKeyController,
                                  decoration: const InputDecoration(
                                    labelText: 'Chave Pix da Conta Xingubit *',
                                    hintText: 'CNPJ, e-mail, telefone ou chave aleatória',
                                    prefixIcon: Icon(Icons.qr_code_2_outlined, size: 20),
                                  ),
                                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe a Chave Pix associada' : null,
                                ),
                                const SizedBox(height: 16),

                                // Webhook Secret
                                TextFormField(
                                  controller: _webhookSecretController,
                                  obscureText: _obscureWebhookSecret,
                                  decoration: InputDecoration(
                                    labelText: 'Segredo de Assinatura do Webhook (Opcional / Recomendado)',
                                    prefixIcon: const Icon(Icons.security_outlined, size: 20),
                                    suffixIcon: IconButton(
                                      icon: Icon(_obscureWebhookSecret ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 20),
                                      onPressed: () => setState(() => _obscureWebhookSecret = !_obscureWebhookSecret),
                                    ),
                                  ),
                                ),
                                const SizedBox(height: 20),

                                // URL do Webhook do ispERP para copiar
                                Container(
                                  padding: const EdgeInsets.all(14),
                                  decoration: BoxDecoration(
                                    color: AppTheme.darkBg,
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: AppTheme.darkCard),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: const [
                                          Icon(Icons.link, size: 18, color: AppTheme.primaryBlue),
                                          SizedBox(width: 8),
                                          Text(
                                            'URL do Webhook para Notificação de Pagamento:',
                                            style: TextStyle(
                                              fontSize: 12,
                                              fontWeight: FontWeight.bold,
                                              color: AppTheme.textSecondary,
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 8),
                                      Row(
                                        children: [
                                          Expanded(
                                            child: SelectableText(
                                              webhookEndpointUrl,
                                              style: const TextStyle(
                                                fontFamily: 'monospace',
                                                fontSize: 12,
                                                color: AppTheme.primaryBlue,
                                              ),
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          IconButton(
                                            icon: const Icon(Icons.copy, size: 18),
                                            tooltip: 'Copiar URL',
                                            onPressed: () {
                                              Clipboard.setData(ClipboardData(text: webhookEndpointUrl));
                                              ScaffoldMessenger.of(context).showSnackBar(
                                                const SnackBar(
                                                  content: Text('URL do Webhook copiada para a área de transferência!'),
                                                  duration: Duration(seconds: 2),
                                                ),
                                              );
                                            },
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 4),
                                      const Text(
                                        'Cadastre esta URL no painel do Xingubit Pay para receber notificações em tempo real de liquidação Pix.',
                                        style: TextStyle(fontSize: 11, color: AppTheme.textMuted),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(height: 20),

                                // Switches: Sandbox & Ativo
                                SwitchListTile(
                                  contentPadding: EdgeInsets.zero,
                                  title: const Text('Modo Sandbox (Ambiente de Testes)'),
                                  subtitle: const Text(
                                    'Permite simular pagamentos e testes sem transacionar dinheiro real',
                                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                  ),
                                  value: _sandbox,
                                  onChanged: (val) => setState(() => _sandbox = val),
                                ),
                                SwitchListTile(
                                  contentPadding: EdgeInsets.zero,
                                  title: const Text('Habilitar Gateway'),
                                  subtitle: const Text(
                                    'Quando ativado, faturas geradas no ERP utilizarão este canal para emissão Pix',
                                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                  ),
                                  value: _active,
                                  onChanged: (val) => setState(() => _active = val),
                                ),
                                const SizedBox(height: 24),

                                // Botão Salvar
                                SizedBox(
                                  width: double.infinity,
                                  child: ElevatedButton.icon(
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: AppTheme.primaryBlue,
                                      foregroundColor: Colors.white,
                                      padding: const EdgeInsets.symmetric(vertical: 16),
                                    ),
                                    onPressed: _isSaving ? null : _saveConfig,
                                    icon: _isSaving
                                        ? const SizedBox(
                                            width: 18,
                                            height: 18,
                                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                          )
                                        : const Icon(Icons.save_outlined, size: 20),
                                    label: Text(
                                      _isSaving ? 'Salvando...' : 'Salvar Credenciais do Xingubit Pay',
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 24),

                      // PRONTIDÃO PARA FUTUROS GATEWAYS (ZERO DADOS FALSOS)
                      Card(
                        color: AppTheme.darkSurface.withValues(alpha: 0.5),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                          side: const BorderSide(color: AppTheme.darkCard),
                        ),
                        child: const Padding(
                          padding: EdgeInsets.all(20.0),
                          child: Row(
                            children: [
                              Icon(Icons.extension_outlined, color: AppTheme.textMuted, size: 24),
                              SizedBox(width: 14),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Arquitetura Aberta para Multi-Gateways',
                                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                    ),
                                    SizedBox(height: 2),
                                    Text(
                                      'O backend ispERP possui padrão Strategy preparado para suporte futuro a Mercado Pago, Efí e Asaas sob demanda.',
                                      style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
