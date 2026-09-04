import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/sales_models.dart';
import '../../data/sales_onboarding_notifier.dart';

/// Passo 3 do Onboarding de Venda Expressa: Dados Cadastrais do Titular.
class StepSalesCustomerForm extends ConsumerStatefulWidget {
  const StepSalesCustomerForm({super.key});

  @override
  ConsumerState<StepSalesCustomerForm> createState() => _StepSalesCustomerFormState();
}

class _StepSalesCustomerFormState extends ConsumerState<StepSalesCustomerForm> {
  late TextEditingController _cpfCtrl;
  late TextEditingController _nameCtrl;
  late TextEditingController _phoneCtrl;
  late TextEditingController _emailCtrl;

  @override
  void initState() {
    super.initState();
    final state = ref.read(salesOnboardingProvider);
    _cpfCtrl = TextEditingController(text: CpfUtils.format(state.customerCpf));
    _nameCtrl = TextEditingController(text: state.customerName);
    _phoneCtrl = TextEditingController(text: state.customerPhone);
    _emailCtrl = TextEditingController(text: state.customerEmail);
  }

  @override
  void dispose() {
    _cpfCtrl.dispose();
    _nameCtrl.dispose();
    _phoneCtrl.dispose();
    _emailCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(salesOnboardingProvider);
    final notifier = ref.read(salesOnboardingProvider.notifier);

    final isCpfEntered = state.customerCpf.isNotEmpty;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            '3. Dados do Novo Assinante',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          const Text(
            'Informe os dados do titular para emissão do contrato digital e agendamento da O.S.',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 16),

          // Campo CPF com validação matemática em tempo real
          TextField(
            controller: _cpfCtrl,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
              labelText: 'CPF do Titular *',
              hintText: '000.000.000-00',
              prefixIcon: const Icon(Icons.badge_outlined, size: 20),
              suffixIcon: isCpfEntered
                  ? (state.isCpfValid
                      ? const Icon(Icons.check_circle, color: AppTheme.accentGreen, size: 20)
                      : const Icon(Icons.cancel, color: AppTheme.accentError, size: 20))
                  : null,
              helperText: isCpfEntered && !state.isCpfValid
                  ? 'CPF inválido ou incompleto (deve conter 11 dígitos válidos)'
                  : null,
              helperStyle: const TextStyle(color: AppTheme.accentError, fontSize: 11),
            ),
            onChanged: (val) {
              notifier.setCustomerCpf(val);
              // Auto formata quando completa
              final clean = CpfUtils.clean(val);
              if (clean.length == 11) {
                final formatted = CpfUtils.format(clean);
                if (_cpfCtrl.text != formatted) {
                  _cpfCtrl.value = TextEditingValue(
                    text: formatted,
                    selection: TextSelection.collapsed(offset: formatted.length),
                  );
                }
              }
            },
          ),
          const SizedBox(height: 14),

          // Nome Completo
          TextField(
            controller: _nameCtrl,
            decoration: const InputDecoration(
              labelText: 'Nome Completo *',
              hintText: 'Ex: Carlos Alberto da Silva',
              prefixIcon: Icon(Icons.person_outline, size: 20),
            ),
            onChanged: (val) => notifier.setCustomerName(val),
          ),
          const SizedBox(height: 14),

          // WhatsApp / Telefone
          TextField(
            controller: _phoneCtrl,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(
              labelText: 'WhatsApp / Telefone para Contato *',
              hintText: '(00) 90000-0000',
              prefixIcon: Icon(Icons.phone_outlined, size: 20),
              helperText: 'Usado para envio do link de assinatura digital e avisos da O.S.',
            ),
            onChanged: (val) => notifier.setCustomerPhone(val),
          ),
          const SizedBox(height: 14),

          // E-mail (Opcional)
          TextField(
            controller: _emailCtrl,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(
              labelText: 'E-mail do Assinante (Opcional)',
              hintText: 'cliente@exemplo.com.br',
              prefixIcon: Icon(Icons.email_outlined, size: 20),
            ),
            onChanged: (val) => notifier.setCustomerEmail(val),
          ),
          const SizedBox(height: 20),

          // Canal de Notificação Preferencial
          const Text(
            'Canal de Notificação Preferencial:',
            style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              _buildChannelChip(
                label: 'WhatsApp',
                icon: Icons.chat_outlined,
                value: 'WHATSAPP',
                current: state.notificationChannel,
                onSelect: (v) => notifier.setNotificationChannel(v),
              ),
              const SizedBox(width: 8),
              _buildChannelChip(
                label: 'SMS',
                icon: Icons.sms_outlined,
                value: 'SMS',
                current: state.notificationChannel,
                onSelect: (v) => notifier.setNotificationChannel(v),
              ),
              const SizedBox(width: 8),
              _buildChannelChip(
                label: 'E-mail',
                icon: Icons.alternate_email_outlined,
                value: 'EMAIL',
                current: state.notificationChannel,
                onSelect: (v) => notifier.setNotificationChannel(v),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildChannelChip({
    required String label,
    required IconData icon,
    required String value,
    required String current,
    required ValueChanged<String> onSelect,
  }) {
    final isSelected = current == value;
    return ChoiceChip(
      avatar: Icon(
        icon,
        size: 16,
        color: isSelected ? Colors.white : AppTheme.textSecondary,
      ),
      label: Text(label),
      selected: isSelected,
      selectedColor: AppTheme.primaryBlue,
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : AppTheme.textPrimary,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
      ),
      onSelected: (_) => onSelect(value),
    );
  }
}
