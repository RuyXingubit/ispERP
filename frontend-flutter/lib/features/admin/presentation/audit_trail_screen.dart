import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/audit_log_model.dart';
import '../data/audit_log_notifier.dart';
import '../data/users_notifier.dart';

/// Tela de Trilha de Auditoria do Administrador.
/// Permite rastrear todas as ações de colaboradores no sistema com filtros
/// temporais, por colaborador e por módulo (Financeiro, Faturamento, Usuários, etc.).
class AuditTrailScreen extends ConsumerStatefulWidget {
  const AuditTrailScreen({super.key});

  @override
  ConsumerState<AuditTrailScreen> createState() => _AuditTrailScreenState();
}

class _AuditTrailScreenState extends ConsumerState<AuditTrailScreen> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final auditState = ref.watch(auditLogNotifierProvider);
    final auditNotifier = ref.read(auditLogNotifierProvider.notifier);
    final usersState = ref.watch(usersProvider);

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Cabeçalho Executivo
            _buildHeader(context, auditNotifier),
            const SizedBox(height: 20),

            // Painel de Filtros
            _buildFilterPanel(context, auditState, auditNotifier, usersState),
            const SizedBox(height: 20),

            // Conteúdo Principal: Tabela / Lista de Logs
            auditState.pageAsync.when(
              loading: () => const Center(
                child: Padding(
                  padding: EdgeInsets.all(48.0),
                  child: CircularProgressIndicator(),
                ),
              ),
              error: (err, _) => _buildErrorCard(err.toString(), auditNotifier),
              data: (page) => _buildLogsContent(context, page, auditNotifier),
            ),
          ],
        ),
      ),
    );
  }

  /// Cabeçalho defensivo com título e botão de sincronização
  Widget _buildHeader(BuildContext context, AuditLogNotifier notifier) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isMobile = constraints.maxWidth < 600;

        return Wrap(
          spacing: 16,
          runSpacing: 12,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            ConstrainedBox(
              constraints: BoxConstraints(
                maxWidth: isMobile
                    ? constraints.maxWidth
                    : constraints.maxWidth * 0.7,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: AppTheme.primaryIndigo.withAlpha(50),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                            color: AppTheme.primaryIndigo.withAlpha(120),
                          ),
                        ),
                        child: const Icon(
                          Icons.fingerprint,
                          color: AppTheme.primaryBlue,
                          size: 24,
                        ),
                      ),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Text(
                          'Trilha de Auditoria',
                          style: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.textPrimary,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'Rastreabilidade imutável de ações operacionais, baixas de contas a receber e lançamentos financeiros.',
                    style: TextStyle(
                      fontSize: 13,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            ElevatedButton.icon(
              onPressed: () => notifier.refresh(),
              icon: const Icon(Icons.refresh, size: 18),
              label: const Text('Atualizar'),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.darkSurface,
                foregroundColor: AppTheme.textPrimary,
                side: const BorderSide(color: AppTheme.darkCard),
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  /// Painel de Filtros
  Widget _buildFilterPanel(
    BuildContext context,
    AuditLogState state,
    AuditLogNotifier notifier,
    UsersState usersState,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Linha 1: Dropdowns de Operador e Módulo + Campo de Busca
            LayoutBuilder(
              builder: (context, constraints) {
                final isNarrow = constraints.maxWidth < 800;

                if (isNarrow) {
                  return Column(
                    children: [
                      _buildUserDropdown(state, notifier, usersState),
                      const SizedBox(height: 12),
                      _buildEntityDropdown(state, notifier),
                      const SizedBox(height: 12),
                      _buildSearchField(notifier),
                    ],
                  );
                }

                return Row(
                  children: [
                    Expanded(
                      flex: 3,
                      child: _buildUserDropdown(state, notifier, usersState),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      flex: 3,
                      child: _buildEntityDropdown(state, notifier),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      flex: 4,
                      child: _buildSearchField(notifier),
                    ),
                  ],
                );
              },
            ),
            const SizedBox(height: 16),
            const Divider(color: AppTheme.darkCard, height: 1),
            const SizedBox(height: 16),

            // Linha 2: Filtros Rápidos de Período Temporal
            Wrap(
              spacing: 8,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                const Text(
                  'Período:',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.textSecondary,
                  ),
                ),
                _buildPeriodChip(
                  label: 'Histórico Completo',
                  filterKey: 'all',
                  activeKey: state.activePeriodFilter,
                  onSelected: () => notifier.setQuickPeriod('all'),
                ),
                _buildPeriodChip(
                  label: 'Hoje',
                  filterKey: 'today',
                  activeKey: state.activePeriodFilter,
                  onSelected: () => notifier.setQuickPeriod('today'),
                ),
                _buildPeriodChip(
                  label: 'Últimos 7 dias',
                  filterKey: '7d',
                  activeKey: state.activePeriodFilter,
                  onSelected: () => notifier.setQuickPeriod('7d'),
                ),
                _buildPeriodChip(
                  label: 'Últimos 30 dias',
                  filterKey: '30d',
                  activeKey: state.activePeriodFilter,
                  onSelected: () => notifier.setQuickPeriod('30d'),
                ),
                ActionChip(
                  avatar: const Icon(Icons.date_range, size: 16),
                  label: Text(
                    state.activePeriodFilter == 'custom' &&
                            state.startDate != null &&
                            state.endDate != null
                        ? '${state.startDate!.day}/${state.startDate!.month} até ${state.endDate!.day}/${state.endDate!.month}'
                        : 'Personalizado',
                    style: TextStyle(
                      fontSize: 12,
                      color: state.activePeriodFilter == 'custom'
                          ? AppTheme.primaryBlue
                          : AppTheme.textSecondary,
                    ),
                  ),
                  backgroundColor: state.activePeriodFilter == 'custom'
                      ? AppTheme.primaryIndigo.withAlpha(60)
                      : AppTheme.darkSurface,
                  side: BorderSide(
                    color: state.activePeriodFilter == 'custom'
                        ? AppTheme.primaryBlue
                        : AppTheme.darkCard,
                  ),
                  onPressed: () => _pickCustomDateRange(context, notifier),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildUserDropdown(
    AuditLogState state,
    AuditLogNotifier notifier,
    UsersState usersState,
  ) {
    return DropdownButtonFormField<String?>(
      value: state.selectedUserId,
      isExpanded: true,
      decoration: const InputDecoration(
        labelText: 'Colaborador / Operador',
        prefixIcon: Icon(Icons.person_outline, size: 18),
        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      ),
      items: [
        const DropdownMenuItem<String?>(
          value: null,
          child: Text('Todos os Colaboradores'),
        ),
        ...usersState.users.map((u) => DropdownMenuItem<String?>(
              value: u.id,
              child: Text(
                '${u.name} (${u.roleDisplay})',
                overflow: TextOverflow.ellipsis,
              ),
            )),
      ],
      onChanged: (val) => notifier.setUserId(val),
    );
  }

  Widget _buildEntityDropdown(AuditLogState state, AuditLogNotifier notifier) {
    return DropdownButtonFormField<String?>(
      value: state.selectedEntityName,
      isExpanded: true,
      decoration: const InputDecoration(
        labelText: 'Módulo / Área do Sistema',
        prefixIcon: Icon(Icons.category_outlined, size: 18),
        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      ),
      items: const [
        DropdownMenuItem<String?>(
          value: null,
          child: Text('Todas as Áreas'),
        ),
        DropdownMenuItem<String?>(
          value: 'FINANCIAL',
          child: Text('Financeiro / Lançamentos'),
        ),
        DropdownMenuItem<String?>(
          value: 'INVOICE',
          child: Text('Contas a Receber / Faturas'),
        ),
        DropdownMenuItem<String?>(
          value: 'USER',
          child: Text('Gestão de Usuários / RBAC'),
        ),
        DropdownMenuItem<String?>(
          value: 'CONTRACT',
          child: Text('Contratos'),
        ),
        DropdownMenuItem<String?>(
          value: 'CUSTOMER',
          child: Text('Clientes'),
        ),
      ],
      onChanged: (val) => notifier.setEntityName(val),
    );
  }

  Widget _buildSearchField(AuditLogNotifier notifier) {
    return TextField(
      controller: _searchController,
      decoration: InputDecoration(
        hintText: 'Buscar por ação ou ID do objeto...',
        prefixIcon: const Icon(Icons.search, size: 18),
        suffixIcon: _searchController.text.isNotEmpty
            ? IconButton(
                icon: const Icon(Icons.clear, size: 16),
                onPressed: () {
                  _searchController.clear();
                  notifier.setSearchAction('');
                },
              )
            : null,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      ),
      onSubmitted: (query) => notifier.setSearchAction(query),
    );
  }

  Widget _buildPeriodChip({
    required String label,
    required String filterKey,
    required String activeKey,
    required VoidCallback onSelected,
  }) {
    final isSelected = activeKey == filterKey;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      labelStyle: TextStyle(
        fontSize: 12,
        color: isSelected ? AppTheme.primaryBlue : AppTheme.textSecondary,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
      ),
      selectedColor: AppTheme.primaryIndigo.withAlpha(60),
      backgroundColor: AppTheme.darkSurface,
      side: BorderSide(
        color: isSelected ? AppTheme.primaryBlue : AppTheme.darkCard,
      ),
      onSelected: (_) => onSelected(),
    );
  }

  Future<void> _pickCustomDateRange(
    BuildContext context,
    AuditLogNotifier notifier,
  ) async {
    final now = DateTime.now();
    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: now,
      initialDateRange: DateTimeRange(
        start: now.subtract(const Duration(days: 7)),
        end: now,
      ),
      builder: (context, child) {
        return Theme(
          data: ThemeData.dark().copyWith(
            colorScheme: const ColorScheme.dark(
              primary: AppTheme.primaryBlue,
              surface: AppTheme.darkSurface,
              onSurface: AppTheme.textPrimary,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      notifier.setDateRange(
        DateTime(picked.start.year, picked.start.month, picked.start.day, 0, 0, 0),
        DateTime(picked.end.year, picked.end.month, picked.end.day, 23, 59, 59),
      );
    }
  }

  /// Conteúdo principal: Lista ou Tabela
  Widget _buildLogsContent(
    BuildContext context,
    AuditLogPage page,
    AuditLogNotifier notifier,
  ) {
    if (page.items.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 48.0, horizontal: 24.0),
          child: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.verified_outlined,
                  size: 48,
                  color: AppTheme.textMuted.withAlpha(150),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Nenhum registro de auditoria encontrado',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 6),
                const Text(
                  'Ajuste os filtros de colaborador, módulo ou período para expandir a consulta.',
                  style: TextStyle(
                    fontSize: 13,
                    color: AppTheme.textSecondary,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: ConstrainedBox(
                  constraints: BoxConstraints(minWidth: constraints.maxWidth),
                  child: DataTable(
                    horizontalMargin: 16,
                    columnSpacing: 20,
                    columns: const [
                      DataColumn(
                        label: Text(
                          'Data & Hora',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                      DataColumn(
                        label: Text(
                          'Colaborador / Operador',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                      DataColumn(
                        label: Text(
                          'Módulo',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                      DataColumn(
                        label: Text(
                          'Ação Executada',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                      DataColumn(
                        label: Text(
                          'Objeto Afetado',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                      DataColumn(
                        label: Text(
                          'Detalhes',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    ],
                    rows: page.items
                        .map((log) => _buildDataRow(context, log))
                        .toList(),
                  ),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 16),

        // Barra de Paginação Defensiva
        _buildPaginationBar(page, notifier),
      ],
    );
  }

  DataRow _buildDataRow(BuildContext context, AuditLogModel log) {
    return DataRow(
      cells: [
        // Data & Hora
        DataCell(
          Text(
            log.formattedDate,
            style: const TextStyle(
              fontSize: 12,
              color: AppTheme.textSecondary,
              fontFamily: 'monospace',
            ),
          ),
        ),

        // Operador
        DataCell(
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              CircleAvatar(
                radius: 12,
                backgroundColor: AppTheme.primaryIndigo.withAlpha(80),
                child: Text(
                  log.userName.isNotEmpty
                      ? log.userName.substring(0, 1).toUpperCase()
                      : 'S',
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.primaryBlue,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 160),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      log.userName,
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: AppTheme.textPrimary,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                    Text(
                      log.userEmail,
                      style: const TextStyle(
                        fontSize: 10,
                        color: AppTheme.textMuted,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),

        // Módulo
        DataCell(
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: AppTheme.darkBg,
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: AppTheme.darkCard),
            ),
            child: Text(
              log.moduleDisplay,
              style: const TextStyle(
                fontSize: 11,
                color: AppTheme.textSecondary,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ),

        // Ação Executada
        DataCell(
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: log.tagColor.withAlpha(35),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: log.tagColor.withAlpha(100)),
            ),
            child: Text(
              log.actionDisplay,
              style: TextStyle(
                fontSize: 11,
                color: log.tagColor,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),

        // Objeto Afetado
        DataCell(
          Text(
            log.entityId != null && log.entityId!.length > 12
                ? '${log.entityId!.substring(0, 8)}...'
                : log.entityId ?? '-',
            style: const TextStyle(
              fontSize: 12,
              fontFamily: 'monospace',
              color: AppTheme.textSecondary,
            ),
          ),
        ),

        // Botão Detalhes do Evento
        DataCell(
          IconButton(
            icon: const Icon(Icons.code, size: 18),
            tooltip: 'Detalhes do Evento',
            color: AppTheme.primaryBlue,
            onPressed: () => _showAuditDetailsDialog(context, log),
          ),
        ),
      ],
    );
  }

  /// Paginação Defensiva
  Widget _buildPaginationBar(AuditLogPage page, AuditLogNotifier notifier) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          spacing: 16,
          runSpacing: 8,
          children: [
            Text(
              'Total de ${page.totalElements} registros encontrados',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textMuted,
              ),
            ),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  icon: const Icon(Icons.chevron_left),
                  tooltip: 'Página Anterior',
                  onPressed: page.currentPage > 0
                      ? () => notifier.changePage(page.currentPage - 1)
                      : null,
                ),
                Text(
                  'Página ${page.currentPage + 1} de ${page.totalPages == 0 ? 1 : page.totalPages}',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.textPrimary,
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.chevron_right),
                  tooltip: 'Próxima Página',
                  onPressed: page.currentPage < page.totalPages - 1
                      ? () => notifier.changePage(page.currentPage + 1)
                      : null,
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  /// Modal de Detalhes do Evento
  void _showAuditDetailsDialog(BuildContext context, AuditLogModel log) {
    String prettyDetails = 'Nenhum detalhe adicional informado.';
    if (log.details != null && log.details!.isNotEmpty) {
      try {
        final parsed = jsonDecode(log.details!);
        const encoder = JsonEncoder.withIndent('  ');
        prettyDetails = encoder.convert(parsed);
      } catch (_) {
        prettyDetails = log.details!;
      }
    }

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: const BorderSide(color: AppTheme.darkCard),
          ),
          title: Row(
            children: [
              const Icon(Icons.shield_outlined,
                  color: AppTheme.primaryBlue, size: 22),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Detalhes do Evento',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.textPrimary,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          content: ConstrainedBox(
            constraints: const BoxConstraints(
              maxWidth: 600,
              maxHeight: 500,
            ),
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  _buildDetailRow('ID do Registro', log.id),
                  _buildDetailRow('Data & Hora', log.formattedDate),
                  _buildDetailRow('Operador', '${log.userName} (${log.userEmail})'),
                  _buildDetailRow('Módulo', log.moduleDisplay),
                  _buildDetailRow('Ação Executada', log.actionDisplay),
                  _buildDetailRow(
                      'ID do Objeto', log.entityId ?? 'Não especificado'),
                  const SizedBox(height: 16),
                  const Text(
                    'Parâmetros e Payload:',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppTheme.darkBg,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppTheme.darkBorder),
                    ),
                    child: SelectableText(
                      prettyDetails,
                      style: const TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 12,
                        color: AppTheme.primaryBlue,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Fechar'),
            ),
          ],
        );
      },
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 130,
            child: Text(
              '$label:',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: AppTheme.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: SelectableText(
              value,
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorCard(String error, AuditLogNotifier notifier) {
    return Card(
      color: AppTheme.accentError.withAlpha(20),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppTheme.accentError),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            const Icon(Icons.error_outline, color: AppTheme.accentError),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                'Falha ao carregar trilha de auditoria: $error',
                style: const TextStyle(color: AppTheme.accentError),
              ),
            ),
            TextButton(
              onPressed: () => notifier.refresh(),
              child: const Text('Tentar novamente'),
            ),
          ],
        ),
      ),
    );
  }
}
