import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../../attendance/data/attendance_notifier.dart';
import '../../attendance/presentation/attendance_hub_modal.dart';
import '../../financial/data/dashboard_bi_provider.dart';
import '../../technician/data/technician_repository.dart';

/// Painel de Atendimento ao Cliente & Suporte Técnico.
/// Estrutura de helpdesk com busca rápida de assinantes e contadores reais da API.
class SupportDashboardScreen extends ConsumerStatefulWidget {
  const SupportDashboardScreen({super.key});

  @override
  ConsumerState<SupportDashboardScreen> createState() => _SupportDashboardScreenState();
}

class _SupportDashboardScreenState extends ConsumerState<SupportDashboardScreen> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _triggerSearch(BuildContext context) {
    final query = _searchController.text.trim();
    if (query.isNotEmpty) {
      ref.read(attendanceProvider.notifier).searchCustomers(query);
    }
    AttendanceHubModal.show(context);
  }

  @override
  Widget build(BuildContext context) {
    final biAsync = ref.watch(dashboardBiProvider);
    final workOrdersAsync = ref.watch(technicianWorkOrdersProvider);

    final openSupportCount = workOrdersAsync.maybeWhen(
      data: (list) => list.where((wo) => wo.status.toUpperCase() != 'COMPLETED' && wo.status.toUpperCase() != 'CANCELLED').length,
      orElse: () => 0,
    );

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header responsivo
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text(
                        'Central de Atendimento & Helpdesk',
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Localização rápida de assinantes, 2ª via Pix, desbloqueio em confiança, diagnóstico de sinal e chamados',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Barra de Busca Rápida integrada ao AttendanceNotifier
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _searchController,
                        onSubmitted: (_) => _triggerSearch(context),
                        decoration: const InputDecoration(
                          hintText: 'Buscar assinante por CPF/CNPJ, Nome, MAC da ONU ou Contrato...',
                          prefixIcon: Icon(Icons.search, size: 20),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    ElevatedButton.icon(
                      onPressed: () => _triggerSearch(context),
                      icon: const Icon(Icons.person_search, size: 18),
                      label: const Text('Localizar'),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Cards de Métricas de Atendimento
            Wrap(
              spacing: 16,
              runSpacing: 16,
              children: [
                _buildTicketMetric('Fila de Atendimento', '$openSupportCount em espera', AppTheme.accentWarning, Icons.phone_in_talk),
                _buildTicketMetric('SLA Médio (ANATEL)', 'Dentro da meta', AppTheme.accentGreen, Icons.timer_outlined),
                _buildTicketMetric('Desbloqueio em Confiança', 'Regras Ativas', AppTheme.primaryBlue, Icons.lock_open),
                biAsync.maybeWhen(
                  data: (bi) => _buildTicketMetric(
                    'Alarmes Ópticos (NOC)',
                    '${bi.criticalSignalOnus} em alerta',
                    bi.criticalSignalOnus == 0 ? AppTheme.accentGreen : AppTheme.accentError,
                    Icons.cable,
                  ),
                  orElse: () => _buildTicketMetric('Alarmes Ópticos (NOC)', 'Verificando...', AppTheme.primaryIndigo, Icons.cable),
                ),
              ],
            ),
            const SizedBox(height: 28),

            // Fila de Chamados Ativos (GET /work-orders)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Expanded(
                          child: Text(
                            'Chamados em Andamento',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        IconButton(
                          onPressed: () {
                            ref.invalidate(technicianWorkOrdersProvider);
                            ref.invalidate(dashboardBiProvider);
                          },
                          icon: const Icon(Icons.refresh, size: 18, color: AppTheme.primaryBlue),
                          tooltip: 'Recarregar chamados',
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    workOrdersAsync.when(
                      loading: () => const Padding(
                        padding: EdgeInsets.symmetric(vertical: 24),
                        child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
                      ),
                      error: (err, _) => Padding(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        child: Text(
                          'Erro ao consultar fila de chamados: $err',
                          style: const TextStyle(color: AppTheme.accentError, fontSize: 13),
                        ),
                      ),
                      data: (allOrders) {
                        final openOrders = allOrders
                            .where((wo) => wo.status.toUpperCase() != 'COMPLETED' && wo.status.toUpperCase() != 'CANCELLED')
                            .toList();

                        if (openOrders.isEmpty) {
                          return Container(
                            padding: const EdgeInsets.symmetric(vertical: 24),
                            alignment: Alignment.center,
                            child: Column(
                              children: [
                                const Icon(Icons.check_circle_outline, color: AppTheme.accentGreen, size: 32),
                                const SizedBox(height: 10),
                                const Text(
                                  'Nenhum chamado aberto aguardando atendimento no momento.',
                                  style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                                ),
                                const SizedBox(height: 14),
                                OutlinedButton.icon(
                                  onPressed: () => AttendanceHubModal.show(context),
                                  icon: const Icon(Icons.add, size: 16),
                                  label: const Text('Abrir Novo Atendimento'),
                                ),
                              ],
                            ),
                          );
                        }

                        return Column(
                          children: [
                            ...openOrders.map(
                              (wo) => Container(
                                margin: const EdgeInsets.only(bottom: 10),
                                padding: const EdgeInsets.all(14),
                                decoration: BoxDecoration(
                                  color: AppTheme.darkBg,
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(color: AppTheme.darkBorder),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(Icons.support_agent_rounded, color: AppTheme.primaryBlue, size: 20),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            'O.S. #${wo.shortId} — ${wo.typeLabel}',
                                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            'Status: ${wo.statusLabel} • Técnico: ${wo.technicianName ?? "Aguardando despacho"}',
                                            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            const SizedBox(height: 8),
                            Align(
                              alignment: Alignment.centerRight,
                              child: OutlinedButton.icon(
                                onPressed: () => AttendanceHubModal.show(context),
                                icon: const Icon(Icons.add, size: 16),
                                label: const Text('Abrir Novo Atendimento'),
                              ),
                            ),
                          ],
                        );
                      },
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

  Widget _buildTicketMetric(String title, String val, Color col, IconData ic) {
    return Container(
      width: 240,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: col.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(ic, color: col, size: 22),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  val,
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
