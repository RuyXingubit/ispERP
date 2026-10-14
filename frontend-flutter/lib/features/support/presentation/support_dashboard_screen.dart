import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../../attendance/data/attendance_notifier.dart';
import '../../attendance/presentation/attendance_hub_modal.dart';
import '../../financial/data/dashboard_bi_provider.dart';
import '../../technician/data/technician_repository.dart';
import '../data/helpdesk_models.dart';
import '../data/helpdesk_repository.dart';
import 'ticket_detail_modal.dart';

/// Painel de Atendimento ao Cliente & Helpdesk ANATEL (Esteira N1 -> N2/NOC -> O.S. de Campo).
class SupportDashboardScreen extends ConsumerStatefulWidget {
  const SupportDashboardScreen({super.key});

  @override
  ConsumerState<SupportDashboardScreen> createState() => _SupportDashboardScreenState();
}

class _SupportDashboardScreenState extends ConsumerState<SupportDashboardScreen> {
  final _searchController = TextEditingController();
  final _protocolController = TextEditingController();
  String _statusFilter = 'TODOS';
  bool _isSearchingProtocol = false;

  @override
  void dispose() {
    _searchController.dispose();
    _protocolController.dispose();
    super.dispose();
  }

  void _triggerSearch(BuildContext context) {
    final query = _searchController.text.trim();
    if (query.isNotEmpty) {
      ref.read(attendanceProvider.notifier).searchCustomers(query);
    }
    AttendanceHubModal.show(context);
  }

  Future<void> _searchByProtocol(BuildContext context) async {
    final protocol = _protocolController.text.trim();
    if (protocol.isEmpty) return;

    setState(() => _isSearchingProtocol = true);
    try {
      final repo = ref.read(helpdeskRepositoryProvider);
      final ticket = await repo.getTicketByProtocol(protocol);
      if (!context.mounted) return;
      if (ticket != null) {
        TicketDetailModal.show(context, ticket);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Protocolo Anatel "$protocol" não localizado.'),
            backgroundColor: AppTheme.accentWarning,
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Protocolo "$protocol" não encontrado ou erro na busca.'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSearchingProtocol = false);
    }
  }

  void _refreshAll() {
    ref.invalidate(helpdeskTicketsProvider);
    ref.invalidate(technicianWorkOrdersProvider);
    ref.invalidate(dashboardBiProvider);
  }

  @override
  Widget build(BuildContext context) {
    final biAsync = ref.watch(dashboardBiProvider);
    final workOrdersAsync = ref.watch(technicianWorkOrdersProvider);
    final ticketsAsync = ref.watch(helpdeskTicketsProvider);

    final activeTicketsCount = ticketsAsync.maybeWhen(
      data: (list) => list.where((t) => t.isActive).length,
      orElse: () => 0,
    );

    final n2QueueCount = ticketsAsync.maybeWhen(
      data: (list) => list.where((t) => t.isN2Queue).length,
      orElse: () => 0,
    );

    final openWorkOrdersCount = workOrdersAsync.maybeWhen(
      data: (list) => list
          .where((wo) =>
              wo.status.toUpperCase() != 'COMPLETED' &&
              wo.status.toUpperCase() != 'CANCELLED')
          .length,
      orElse: () => 0,
    );

    final totalWaitingCount = activeTicketsCount + openWorkOrdersCount;

    return DefaultTabController(
      length: 3,
      child: Scaffold(
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header responsivo
              Wrap(
                spacing: 16,
                runSpacing: 12,
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Central de Atendimento & Helpdesk',
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Esteira N1 / N2 (NOC) com Protocolo Anatel (YYYYMMDDNNNNN), controle de SLA 24h/48h/72h e O.S. de Campo',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                  Wrap(
                    spacing: 10,
                    runSpacing: 8,
                    children: [
                      OutlinedButton.icon(
                        onPressed: _refreshAll,
                        icon: const Icon(Icons.refresh, size: 16),
                        label: const Text('Atualizar Filas'),
                      ),
                      ElevatedButton.icon(
                        onPressed: () => AttendanceHubModal.show(context),
                        icon: const Icon(Icons.support_agent_rounded, size: 18),
                        label: const Text('Iniciar Atendimento (Hub 360)'),
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 20),

              // Barra Dupla: Localizar Assinante (Hub 360) + Busca Direta por Protocolo Anatel
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Wrap(
                    spacing: 16,
                    runSpacing: 12,
                    children: [
                      SizedBox(
                        width: 460,
                        child: Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _searchController,
                                onSubmitted: (_) => _triggerSearch(context),
                                decoration: const InputDecoration(
                                  hintText: 'Buscar assinante por CPF/CNPJ, Nome ou Contrato...',
                                  prefixIcon: Icon(Icons.person_search, size: 20),
                                  isDense: true,
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            ElevatedButton.icon(
                              onPressed: () => _triggerSearch(context),
                              icon: const Icon(Icons.search, size: 16),
                              label: const Text('Localizar'),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(
                        width: 380,
                        child: Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _protocolController,
                                onSubmitted: (_) => _searchByProtocol(context),
                                decoration: const InputDecoration(
                                  hintText: 'Protocolo Anatel (ex: 2026090400001)...',
                                  prefixIcon: Icon(Icons.confirmation_number_outlined, size: 18),
                                  isDense: true,
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            OutlinedButton.icon(
                              onPressed: _isSearchingProtocol ? null : () => _searchByProtocol(context),
                              icon: const Icon(Icons.manage_search, size: 18),
                              label: Text(_isSearchingProtocol ? '...' : 'Protocolo'),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // Cards de Métricas de Atendimento (N1 / N2 / O.S. / NOC)
              Wrap(
                spacing: 16,
                runSpacing: 16,
                children: [
                  _buildTicketMetric(
                    'Fila de Atendimento',
                    '$totalWaitingCount em espera',
                    AppTheme.accentWarning,
                    Icons.phone_in_talk,
                  ),
                  _buildTicketMetric(
                    'Fila Técnica N2 / NOC',
                    '$n2QueueCount escalonados',
                    AppTheme.primaryBlue,
                    Icons.router_outlined,
                  ),
                  _buildTicketMetric(
                    'SLA Médio (ANATEL)',
                    'Dentro da meta',
                    AppTheme.accentGreen,
                    Icons.timer_outlined,
                  ),
                  biAsync.maybeWhen(
                    data: (bi) => _buildTicketMetric(
                      'Alarmes Ópticos (NOC)',
                      '${bi.criticalSignalOnus} em alerta',
                      bi.criticalSignalOnus == 0 ? AppTheme.accentGreen : AppTheme.accentError,
                      Icons.cable,
                    ),
                    orElse: () => _buildTicketMetric(
                      'Alarmes Ópticos (NOC)',
                      'Verificando...',
                      AppTheme.primaryIndigo,
                      Icons.cable,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),

              // Abas Operacionais da Esteira Anatel (N1 / N2 / Campo)
              Container(
                decoration: BoxDecoration(
                  color: AppTheme.darkSurface,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.darkBorder),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    TabBar(
                      isScrollable: true,
                      indicatorColor: AppTheme.primaryBlue,
                      labelColor: AppTheme.primaryBlue,
                      unselectedLabelColor: AppTheme.textSecondary,
                      tabs: [
                        Tab(
                          icon: const Icon(Icons.support_agent_rounded, size: 18),
                          text: '1. Fila Geral & Protocolos Anatel ($activeTicketsCount)',
                        ),
                        Tab(
                          icon: const Icon(Icons.engineering_outlined, size: 18),
                          text: '2. Fila Escalada N2 / NOC ($n2QueueCount)',
                        ),
                        Tab(
                          icon: const Icon(Icons.handyman_outlined, size: 18),
                          text: '3. Ordens de Serviço de Campo ($openWorkOrdersCount)',
                        ),
                      ],
                    ),
                    const Divider(height: 1),
                    SizedBox(
                      height: 520,
                      child: TabBarView(
                        children: [
                          _buildN1GeneralTab(context, ticketsAsync),
                          _buildN2EscalatedTab(context, ticketsAsync),
                          _buildFieldWorkOrdersTab(context, workOrdersAsync, ticketsAsync),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// Aba 1: Fila Geral de Protocolos Anatel & Triagem N1.
  Widget _buildN1GeneralTab(
    BuildContext context,
    AsyncValue<List<HelpdeskTicket>> ticketsAsync,
  ) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  _buildFilterChip('TODOS', 'Todos'),
                  _buildFilterChip('OPEN', 'Fila N1 (Abertos)'),
                  _buildFilterChip('IN_PROGRESS', 'Em N2 / NOC'),
                  _buildFilterChip('WAITING_FIELD_VISIT', 'Aguardando Visita'),
                  _buildFilterChip('RESOLVED', 'Resolvidos N2'),
                  _buildFilterChip('CLOSED', 'Encerrados (CSAT)'),
                ],
              ),
              OutlinedButton.icon(
                onPressed: () => AttendanceHubModal.show(context),
                icon: const Icon(Icons.add, size: 16),
                label: const Text('Abrir Novo Atendimento'),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Expanded(
            child: ticketsAsync.when(
              loading: () => const Center(child: CircularProgressIndicator(strokeWidth: 2)),
              error: (err, _) => _buildEmptyTicketState(
                context,
                'Nenhum chamado aberto aguardando atendimento no momento.',
              ),
              data: (tickets) {
                final filtered = _statusFilter == 'TODOS'
                    ? tickets
                    : tickets
                        .where((t) => t.status.toUpperCase() == _statusFilter)
                        .toList();

                if (filtered.isEmpty) {
                  return _buildEmptyTicketState(
                    context,
                    'Nenhum chamado aberto aguardando atendimento no momento.',
                  );
                }

                return ListView.separated(
                  itemCount: filtered.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (context, idx) => _buildTicketRow(context, filtered[idx]),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  /// Aba 2: Fila Técnica Escalada para N2 / NOC (`IN_PROGRESS`).
  Widget _buildN2EscalatedTab(
    BuildContext context,
    AsyncValue<List<HelpdeskTicket>> ticketsAsync,
  ) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: ticketsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator(strokeWidth: 2)),
        error: (err, _) => _buildEmptyTicketState(
          context,
          'Nenhum chamado escalonado na fila N2 / NOC no momento.',
        ),
        data: (tickets) {
          final n2List = tickets.where((t) => t.isN2Queue).toList();
          if (n2List.isEmpty) {
            return _buildEmptyTicketState(
              context,
              'Nenhum chamado escalonado na fila N2 / NOC no momento.',
            );
          }

          return ListView.separated(
            itemCount: n2List.length,
            separatorBuilder: (_, _) => const SizedBox(height: 10),
            itemBuilder: (context, idx) => _buildTicketRow(context, n2List[idx]),
          );
        },
      ),
    );
  }

  /// Aba 3: Ordens de Serviço de Campo (`/work-orders`) e Chamados `WAITING_FIELD_VISIT`.
  Widget _buildFieldWorkOrdersTab(
    BuildContext context,
    AsyncValue<List<TechnicianWorkOrderModel>> workOrdersAsync,
    AsyncValue<List<HelpdeskTicket>> ticketsAsync,
  ) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: workOrdersAsync.when(
        loading: () => const Center(child: CircularProgressIndicator(strokeWidth: 2)),
        error: (err, _) => Center(
          child: Text(
            'Erro ao consultar fila de O.S. de campo: $err',
            style: const TextStyle(color: AppTheme.accentError, fontSize: 13),
          ),
        ),
        data: (allOrders) {
          final openOrders = allOrders
              .where((wo) =>
                  wo.status.toUpperCase() != 'COMPLETED' &&
                  wo.status.toUpperCase() != 'CANCELLED')
              .toList();

          final fieldTickets = ticketsAsync.maybeWhen(
            data: (list) => list.where((t) => t.isWaitingFieldVisit).toList(),
            orElse: () => const <HelpdeskTicket>[],
          );

          if (openOrders.isEmpty && fieldTickets.isEmpty) {
            return _buildEmptyTicketState(
              context,
              'Nenhuma Ordem de Serviço de campo pendente no momento.',
            );
          }

          return ListView(
            children: [
              if (fieldTickets.isNotEmpty) ...[
                const Text(
                  'Protocolos Anatel Aguardando Visita Técnica',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.primaryBlue),
                ),
                const SizedBox(height: 8),
                ...fieldTickets.map((t) => Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _buildTicketRow(context, t),
                    )),
                const SizedBox(height: 12),
              ],
              const Text(
                'Ordens de Serviço em Execução / Despacho de Campo',
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 8),
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
                      const Icon(Icons.handyman_outlined, color: AppTheme.accentWarning, size: 20),
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
            ],
          );
        },
      ),
    );
  }

  Widget _buildFilterChip(String value, String label) {
    final isSelected = _statusFilter == value;
    return ChoiceChip(
      label: Text(label, style: const TextStyle(fontSize: 11)),
      selected: isSelected,
      onSelected: (_) => setState(() => _statusFilter = value),
    );
  }

  Widget _buildTicketRow(BuildContext context, HelpdeskTicket ticket) {
    return InkWell(
      onTap: () => TicketDetailModal.show(context, ticket),
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppTheme.darkBg,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppTheme.darkBorder),
        ),
        child: Wrap(
          spacing: 12,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  ticket.isN2Queue ? Icons.engineering_outlined : Icons.confirmation_number_outlined,
                  color: ticket.isN2Queue ? AppTheme.primaryBlue : AppTheme.accentWarning,
                  size: 20,
                ),
                const SizedBox(width: 12),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 520),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Protocolo ${ticket.protocol} — ${ticket.subject}',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 3),
                      Text(
                        '${ticket.categoryLabel} • ${ticket.statusLabel} • Prioridade: ${ticket.priorityLabel}',
                        style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            Wrap(
              spacing: 10,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: ticket.isSlaExpired
                        ? AppTheme.accentError.withValues(alpha: 0.15)
                        : AppTheme.accentGreen.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    'SLA: ${ticket.formattedSlaDeadline}',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: ticket.isSlaExpired ? AppTheme.accentError : AppTheme.accentGreen,
                    ),
                  ),
                ),
                OutlinedButton.icon(
                  onPressed: () => TicketDetailModal.show(context, ticket),
                  icon: const Icon(Icons.open_in_new, size: 15),
                  label: const Text('Tratar Chamado', style: TextStyle(fontSize: 12)),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyTicketState(BuildContext context, String message) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.check_circle_outline, color: AppTheme.accentGreen, size: 32),
          const SizedBox(height: 10),
          Text(
            message,
            style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13),
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
