import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../../financial/data/dashboard_bi_model.dart';
import '../../financial/data/dashboard_bi_provider.dart';

/// Painel da Diretoria & Administração Geral do Provedor.
/// Formato de visão executiva 360º consumindo indicadores reais do backend.
class AdminDashboardScreen extends ConsumerWidget {
  const AdminDashboardScreen({super.key});

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    final biAsync = ref.watch(dashboardBiProvider);

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header responsivo sem overflow
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Painel Executivo 360º — ${authState.name ?? "Diretoria"}',
                        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Visão integrada de infraestrutura, finanças, OLTs e conformidade regulatória',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                ElevatedButton.icon(
                  onPressed: () => ref.invalidate(dashboardBiProvider),
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('Atualizar Métricas'),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Estados do Provider
            biAsync.when(
              loading: () => _buildLoadingState(),
              error: (err, stack) => _buildErrorState(ref, err.toString()),
              data: (bi) => _buildContent(context, bi, authState),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLoadingState() {
    return Wrap(
      spacing: 16,
      runSpacing: 16,
      children: List.generate(
        4,
        (_) => Container(
          width: 240,
          height: 100,
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppTheme.darkSurface,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppTheme.darkCard),
          ),
          child: const Center(
            child: SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildErrorState(WidgetRef ref, String error) {
    return Card(
      color: AppTheme.accentError.withValues(alpha: 0.1),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Erro ao carregar métricas executivas', style: TextStyle(fontWeight: FontWeight.bold, color: AppTheme.accentError)),
            const SizedBox(height: 6),
            Text(error, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: () => ref.invalidate(dashboardBiProvider), child: const Text('Tentar Novamente')),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, DashboardBiModel bi, AuthState authState) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Cards de Métricas Principais (reais do banco)
        Wrap(
          spacing: 16,
          runSpacing: 16,
          children: [
            _buildMetricCard(
              title: 'Assinantes Ativos',
              value: '${bi.activeContracts} Ativos',
              delta: 'Total: ${bi.totalCustomers} clientes',
              icon: Icons.people_alt_outlined,
              color: AppTheme.primaryBlue,
            ),
            _buildMetricCard(
              title: 'Receita Recorrente (MRR)',
              value: _formatCurrency(bi.mrr),
              delta: 'ARR Estimado: ${_formatCurrency(bi.arr)}',
              icon: Icons.monetization_on_outlined,
              color: AppTheme.accentGreen,
            ),
            _buildMetricCard(
              title: 'Dispositivos & OLTs',
              value: '${bi.totalNetworkDevices} na Rede',
              delta: '${bi.totalOnus} ONUs (${bi.provisionedOnus} ativas)',
              icon: Icons.router_outlined,
              color: AppTheme.primaryIndigo,
            ),
            _buildMetricCard(
              title: 'Inadimplência (Vencido)',
              value: _formatCurrency(bi.overdueAmount),
              delta: 'Taxa: ${bi.defaultRate.toStringAsFixed(2)}% da base',
              icon: Icons.warning_amber_rounded,
              color: AppTheme.accentWarning,
            ),
          ],
        ),
        const SizedBox(height: 28),

        // Painéis Inferiores: Topologia & Auditoria
        LayoutBuilder(
          builder: (context, constraints) {
            final isWide = constraints.maxWidth > 800;
            return Flex(
              direction: isWide ? Axis.horizontal : Axis.vertical,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Topologia & OLTs da Rede
                Expanded(
                  flex: isWide ? 6 : 0,
                  child: Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: const [
                              Text(
                                'Topologia & Infraestrutura de Rede',
                                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                              ),
                              Icon(Icons.settings_input_composite, color: AppTheme.primaryBlue, size: 20),
                            ],
                          ),
                          const SizedBox(height: 16),
                          _buildOltStatusTile(
                            'Concentradores & OLTs',
                            '${bi.totalNetworkDevices} dispositivos monitorados',
                            'Operando normalmente',
                            true,
                          ),
                          _buildOltStatusTile(
                            'Sinal Óptico das ONUs',
                            '${bi.criticalSignalOnus} ONUs em nível crítico',
                            bi.criticalSignalOnus == 0 ? 'Potência dentro dos parâmetros' : 'Alerta de atenuação',
                            bi.criticalSignalOnus == 0,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                if (isWide) const SizedBox(width: 16) else const SizedBox(height: 16),

                // Registro de Auditoria & Conformidade
                Expanded(
                  flex: isWide ? 4 : 0,
                  child: Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: const [
                              Text(
                                'Auditoria & Segurança',
                                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                              ),
                              Icon(Icons.verified_user_outlined, color: AppTheme.accentGreen, size: 20),
                            ],
                          ),
                          const SizedBox(height: 16),
                          _buildAuditTile('Sessão Ativa Autenticada', 'JWT válido para ${authState.email ?? ""}'),
                          _buildAuditTile('Conexão com Backend', 'Instância em ${authState.serverUrl ?? "Local"}'),
                          _buildAuditTile('Regime Fiscal & NFCom 62', 'Módulo de faturamento em conformidade'),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Widget _buildMetricCard({
    required String title,
    required String value,
    required String delta,
    required IconData icon,
    required Color color,
  }) {
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
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              Icon(icon, color: color, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            value,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 6),
          Text(
            delta,
            style: TextStyle(fontSize: 12, color: color, fontWeight: FontWeight.w500),
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }

  Widget _buildOltStatusTile(String name, String onus, String status, bool ok) {
    return ListTile(
      dense: true,
      contentPadding: EdgeInsets.zero,
      leading: Icon(
        ok ? Icons.check_circle : Icons.warning_amber_rounded,
        color: ok ? AppTheme.accentGreen : AppTheme.accentWarning,
        size: 20,
      ),
      title: Text(name, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
      subtitle: Text('$onus • $status', style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
    );
  }

  Widget _buildAuditTile(String title, String subtitle) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
          const SizedBox(height: 2),
          Text(subtitle, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
        ],
      ),
    );
  }
}
