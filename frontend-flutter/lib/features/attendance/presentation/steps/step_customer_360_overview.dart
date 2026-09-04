import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../sales/data/sales_onboarding_notifier.dart';
import '../../../sales/presentation/sales_onboarding_modal.dart';
import '../../data/attendance_models.dart';
import '../../data/attendance_notifier.dart';

/// Passo 2 do Hub de Atendimento: Raio-X Contextual do Assinante, Linha do Tempo Viva e Escolha de Trilha.
class StepCustomer360Overview extends ConsumerWidget {
  const StepCustomer360Overview({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);
    final customer = state.selectedCustomer;

    if (customer == null) {
      return const Center(child: Text('Nenhum assinante selecionado.'));
    }

    final hasPendingInvoice = state.invoices.isNotEmpty;
    final totalDue = state.invoices.fold<double>(0.0, (acc, item) => acc + item.amount);
    final workOrder = state.liveWorkOrder;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 1. Identificação do Cliente Selecionado
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 24,
                  backgroundColor: AppTheme.primaryBlue.withValues(alpha: 0.2),
                  child: Text(
                    customer.name.isNotEmpty ? customer.name[0].toUpperCase() : '?',
                    style: const TextStyle(color: AppTheme.primaryBlue, fontWeight: FontWeight.bold, fontSize: 18),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(customer.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      const SizedBox(height: 3),
                      Text(
                        'CPF: ${customer.formattedCpf} • ${customer.city ?? "Altamira"}/${customer.state ?? "PA"}',
                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                      ),
                      if (customer.address != null && customer.address!.isNotEmpty)
                        Text(
                          'Endereço: ${customer.address}',
                          style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                        ),
                    ],
                  ),
                ),
                TextButton.icon(
                  icon: const Icon(Icons.swap_horiz, size: 16),
                  label: const Text('Trocar', style: TextStyle(fontSize: 12)),
                  onPressed: () => notifier.goBack(),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // 2. Alerta Financeiro: Fatura em Aberto
          if (hasPendingInvoice)
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.accentError.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.accentError.withValues(alpha: 0.4)),
              ),
              child: Row(
                children: [
                  const Icon(Icons.warning_amber_rounded, color: AppTheme.accentError, size: 24),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '${state.invoices.length} fatura(s) pendente(s) totalizando R\$ ${totalDue.toStringAsFixed(2)}',
                          style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.accentError, fontSize: 13),
                        ),
                        Text(
                          'Fatura mais antiga venceu em ${state.invoices.first.dueDate}',
                          style: TextStyle(color: AppTheme.accentError.withValues(alpha: 0.8), fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppTheme.accentError,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    ),
                    icon: const Icon(Icons.point_of_sale, size: 16),
                    label: const Text('Receber Agora', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                    onPressed: () => notifier.selectIntent(AttendanceIntent.payment),
                  ),
                ],
              ),
            ),
          const SizedBox(height: 14),

          // 3. Linha do Tempo Viva de Ordem de Serviço (Live Tracking)
          if (workOrder != null)
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.primaryBlue.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.3)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.engineering_rounded, color: AppTheme.primaryBlue, size: 20),
                      const SizedBox(width: 8),
                      const Text(
                        'Ordem de Serviço em Andamento',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.primaryBlue),
                      ),
                      const Spacer(),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: AppTheme.primaryBlue.withValues(alpha: 0.2),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          workOrder.friendlyStatus,
                          style: const TextStyle(color: AppTheme.primaryBlue, fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Técnico Responsável: ${workOrder.technicianName ?? "Aguardando despacho"}',
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
                  ),
                  if (workOrder.scheduledDate != null)
                    Text(
                      'Agendamento: ${workOrder.scheduledDate} (${workOrder.scheduledPeriod ?? "Comercial"})',
                      style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                    ),
                  const SizedBox(height: 6),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Row(
                      children: [
                        Icon(Icons.info_outline, size: 14, color: AppTheme.primaryBlue),
                        SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            'Informação para o assinante: O técnico está cumprindo a rota do dia e entrará em deslocamento.',
                            style: TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          const SizedBox(height: 16),

          // 4. Seleção da Intenção de Atendimento (Multi-Caminho)
          const Text(
            'Qual é a solicitação do cliente?',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 10),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 10,
            crossAxisSpacing: 10,
            childAspectRatio: 2.8,
            children: [
              _buildIntentCard(
                icon: Icons.payments_rounded,
                title: 'Pagar Fatura',
                subtitle: 'Cartão, PIX ou Dinheiro',
                color: AppTheme.accentGreen,
                onTap: () => notifier.selectIntent(AttendanceIntent.payment),
              ),
              _buildIntentCard(
                icon: Icons.support_agent_rounded,
                title: 'Suporte Técnico',
                subtitle: 'Lentidão, queda ou O.S.',
                color: AppTheme.primaryBlue,
                onTap: () => notifier.selectIntent(AttendanceIntent.support),
              ),
              _buildIntentCard(
                icon: Icons.add_business_rounded,
                title: 'Venda / Upgrade',
                subtitle: 'Novo plano ou endereço',
                color: Colors.purpleAccent,
                onTap: () {
                  notifier.selectIntent(AttendanceIntent.sales);
                  final salesNotifier = ref.read(salesOnboardingProvider.notifier);
                  salesNotifier.setCustomerCpf(customer.cpf);
                  salesNotifier.setCustomerName(customer.name);
                  SalesOnboardingModal.show(context);
                },
              ),
              _buildIntentCard(
                icon: Icons.help_outline_rounded,
                title: 'Dúvida / 2ª Via',
                subtitle: 'Informações e contrato',
                color: Colors.orangeAccent,
                onTap: () => notifier.selectIntent(AttendanceIntent.general),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildIntentCard({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: AppTheme.darkSurface,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: AppTheme.darkBorder),
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(icon, color: color, size: 20),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                  Text(subtitle, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 10), overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
