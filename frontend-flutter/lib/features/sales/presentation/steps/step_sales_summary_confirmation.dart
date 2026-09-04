import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/sales_models.dart';
import '../../data/sales_onboarding_notifier.dart';

/// Passo 4 do Onboarding de Venda Expressa: Resumo da Contratação & Confirmação.
class StepSalesSummaryConfirmation extends ConsumerWidget {
  const StepSalesSummaryConfirmation({super.key});

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(salesOnboardingProvider);
    final notifier = ref.read(salesOnboardingProvider.notifier);

    // 1. Feedback de Sucesso Pós-Submissão
    if (state.saleResult != null) {
      final res = state.saleResult!;
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppTheme.accentGreen.withValues(alpha: 0.15),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check_circle_rounded, color: AppTheme.accentGreen, size: 56),
              ),
              const SizedBox(height: 16),
              const Text(
                'Venda Concluída com Sucesso!',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 6),
              Text(
                'Assinante ${res.customerName} cadastrado no sistema.',
                style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppTheme.darkSurface,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppTheme.darkBorder),
                ),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Protocolo da Venda:', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                        Text(res.id, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
                      ],
                    ),
                    const Divider(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Status do Contrato:', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: const Text(
                            'DRAFT / Assinatura Pendente',
                            style: TextStyle(color: AppTheme.primaryBlue, fontWeight: FontWeight.bold, fontSize: 11),
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: const [
                        Text('Ordem de Serviço:', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                        Text(
                          'Encaminhada para Despacho Técnico',
                          style: TextStyle(color: AppTheme.accentGreen, fontWeight: FontWeight.bold, fontSize: 11),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: () {
                  notifier.reset();
                  Navigator.of(context).pop();
                },
                icon: const Icon(Icons.done_all, size: 18),
                label: const Text('Finalizar Atendimento'),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                ),
              ),
            ],
          ),
        ),
      );
    }

    final plan = state.selectedPlan;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            '4. Resumo & Conclusão da Contratação',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          const Text(
            'Confira os dados da proposta antes de gerar o contrato digital e a O.S. de instalação.',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 16),

          // Card do Plano
          if (plan != null)
            _buildSectionCard(
              title: 'Plano Selecionado',
              icon: Icons.wifi,
              children: [
                _buildInfoRow('Nome do Plano', plan.name),
                _buildInfoRow('Velocidades', '${plan.downloadSpeed} Mbps Down / ${plan.uploadSpeed} Mbps Up'),
                _buildInfoRow('Mensalidade', _formatCurrency(plan.price), isHighlighted: true),
                _buildInfoRow('Vencimento Preferencial', 'Todo dia ${state.preferredDueDate}'),
              ],
            ),
          const SizedBox(height: 12),

          // Card do Assinante
          _buildSectionCard(
            title: 'Dados do Titular',
            icon: Icons.person_outline,
            children: [
              _buildInfoRow('Nome Completo', state.customerName),
              _buildInfoRow('CPF', CpfUtils.format(state.customerCpf)),
              _buildInfoRow('Telefone / WhatsApp', state.customerPhone),
              if (state.customerEmail.isNotEmpty) _buildInfoRow('E-mail', state.customerEmail),
              _buildInfoRow('Canal de Contrato', state.notificationChannel),
            ],
          ),
          const SizedBox(height: 12),

          // Card de Endereço de Instalação
          _buildSectionCard(
            title: 'Endereço de Instalação',
            icon: Icons.location_on_outlined,
            children: [
              _buildInfoRow(
                'Endereço',
                '${state.street}, ${state.number}${state.complement.isNotEmpty ? " (${state.complement})" : ""}',
              ),
              _buildInfoRow('Bairro / Cidade', '${state.neighborhood}, ${state.city} - ${state.state}'),
              _buildInfoRow('CEP', CpfUtils.formatCep(state.cep)),
              if (state.feasibility != null && state.feasibility!.nearbyCtos.isNotEmpty)
                _buildInfoRow(
                  'CTO Prevista',
                  '${state.feasibility!.nearbyCtos.first.ctoName} (${state.feasibility!.nearbyCtos.first.distanceMeters.toStringAsFixed(1)}m)',
                ),
            ],
          ),
          const SizedBox(height: 20),

          // Botão de Submissão da Venda
          ElevatedButton.icon(
            onPressed: state.isSubmitting ? null : () => notifier.submitSale(),
            icon: state.isSubmitting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.flash_on_rounded, size: 18),
            label: Text(
              state.isSubmitting
                  ? 'Cadastrando & Gerando Contrato...'
                  : 'Concluir Venda & Emitir Contrato',
            ),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentGreen,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionCard({
    required String title,
    required IconData icon,
    required List<Widget> children,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppTheme.darkBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 18, color: AppTheme.primaryBlue),
              const SizedBox(width: 8),
              Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
            ],
          ),
          const Divider(height: 16),
          ...children,
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value, {bool isHighlighted = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontWeight: isHighlighted ? FontWeight.bold : FontWeight.normal,
                color: isHighlighted ? AppTheme.accentGreen : AppTheme.textPrimary,
                fontSize: 12,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
