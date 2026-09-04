import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../../financial/data/dashboard_bi_provider.dart';

/// Painel Comercial & Vendas do Provedor.
/// Funil de contratos e planos ativos com dados reais consolidados da base.
class SalesDashboardScreen extends ConsumerWidget {
  const SalesDashboardScreen({super.key});

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final biAsync = ref.watch(dashboardBiProvider);

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text(
                        'Gestão Comercial & Captação',
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Funil de vendas, contratos ativos, consulta de viabilidade e catálogo de planos',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                ElevatedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.person_add_alt_1_outlined, size: 18),
                  label: const Text('Nova Proposta'),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Métricas do Funil e Base (reais do banco)
            biAsync.maybeWhen(
              data: (bi) => Wrap(
                spacing: 16,
                runSpacing: 16,
                children: [
                  _buildFunnelCard('Contratos Ativos', '${bi.activeContracts} Ativos', 'Base total: ${bi.totalCustomers} clientes', AppTheme.accentGreen, Icons.verified_outlined),
                  _buildFunnelCard('Instalações Pendentes', '${bi.pendingInstallationContracts} Aguardando', 'Aguardando agendamento técnico', AppTheme.accentWarning, Icons.build_outlined),
                  _buildFunnelCard('Contratos Suspensos', '${bi.suspendedContracts} Bloqueados', 'Bloqueio administrativo/financeiro', AppTheme.accentError, Icons.pause_circle_outline),
                  _buildFunnelCard('Ticket Médio (ARPU)', _formatCurrency(bi.arpu), 'Receita média por assinante', AppTheme.primaryIndigo, Icons.payments_outlined),
                ],
              ),
              orElse: () => Wrap(
                spacing: 16,
                runSpacing: 16,
                children: List.generate(
                  4,
                  (_) => Container(
                    width: 240,
                    height: 90,
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppTheme.darkCard),
                    ),
                    child: const Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 28),

            // Catálogo de Planos Ativos
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: const [
                        Text('Planos Vigentes para Comercialização', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                        Icon(Icons.inventory_2_outlined, color: AppTheme.primaryBlue, size: 20),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 16,
                      runSpacing: 16,
                      children: [
                        _buildPlanCard('Fibra 400 Mega', '400 Mbps Down / 200 Mbps Up', 'R\$ 89,90/mês', 'Wi-Fi 5 incluso', AppTheme.primaryBlue),
                        _buildPlanCard('Fibra 600 Mega', '600 Mbps Down / 300 Mbps Up', 'R\$ 109,90/mês', 'Wi-Fi 6 Mesh Incluso', AppTheme.accentGreen),
                        _buildPlanCard('Fibra 1 Giga Dedicado', '1000 Mbps Down / 500 Mbps Up', 'R\$ 149,90/mês', 'Roteador Wi-Fi 6 + IP Fixo', AppTheme.primaryIndigo),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFunnelCard(String title, String val, String sub, Color col, IconData ic) {
    return Container(
      width: 240,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(child: Text(title, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary), overflow: TextOverflow.ellipsis)),
              const SizedBox(width: 8),
              Icon(ic, color: col, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(val, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold), overflow: TextOverflow.ellipsis),
          const SizedBox(height: 4),
          Text(sub, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted), overflow: TextOverflow.ellipsis),
        ],
      ),
    );
  }

  Widget _buildPlanCard(String name, String speed, String price, String perks, Color col) {
    return Container(
      width: 280,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: col.withValues(alpha: 0.5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(name, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: col)),
          const SizedBox(height: 6),
          Text(speed, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
          const SizedBox(height: 12),
          Text(price, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text('• $perks', style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
        ],
      ),
    );
  }
}
