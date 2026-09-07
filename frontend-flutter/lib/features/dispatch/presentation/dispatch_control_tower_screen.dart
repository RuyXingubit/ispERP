import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/process_lifecycle_stepper.dart';
import '../data/dispatch_models.dart';
import '../data/dispatch_notifier.dart';

/// Torre de Controle de Despacho Técnico e Triagem de Estoque de Materiais.
class DispatchControlTowerScreen extends ConsumerWidget {
  const DispatchControlTowerScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    final state = ref.watch(dispatchProvider);
    final notifier = ref.read(dispatchProvider.notifier);
    final currentUserRole = authState.role?.name.toUpperCase();

    return Scaffold(
      body: LayoutBuilder(
        builder: (context, constraints) {
          final isWide = constraints.maxWidth > 900;

          return Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Topo da Tela: Título e Botão de Atualização
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Icon(Icons.local_shipping_outlined, color: AppTheme.primaryBlue, size: 24),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: const [
                          Text(
                            'Torre de Controle de Despacho & Estoque',
                            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, letterSpacing: -0.5),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Triagem prévia de materiais FTTH, conferência de kit veicular e despacho inteligente',
                            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.refresh, size: 20),
                      tooltip: 'Recarregar Fila',
                      onPressed: state.isLoadingDemands ? null : () => notifier.loadDemands(),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // Banners de Mensagem de Sucesso ou Erro
                if (state.dispatchSuccessMessage != null) ...[
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    decoration: BoxDecoration(
                      color: AppTheme.accentGreen.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.4)),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.check_circle_rounded, color: AppTheme.accentGreen, size: 20),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            state.dispatchSuccessMessage!,
                            style: const TextStyle(fontSize: 13, color: AppTheme.accentGreen, fontWeight: FontWeight.bold),
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.close, size: 16, color: AppTheme.accentGreen),
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                          onPressed: () => notifier.clearMessages(),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                ],

                if (state.errorMessage != null) ...[
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    decoration: BoxDecoration(
                      color: AppTheme.accentError.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppTheme.accentError.withValues(alpha: 0.4)),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.error_outline_rounded, color: AppTheme.accentError, size: 20),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            state.errorMessage!,
                            style: const TextStyle(fontSize: 13, color: AppTheme.accentError),
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.close, size: 16, color: AppTheme.accentError),
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                          onPressed: () => notifier.clearMessages(),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                ],

                // Abas de Filtro da Fila (Status e Tipo de Processo)
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    // Status da Fila
                    _buildFilterChip(
                      context,
                      label: 'Aguardando Triagem (${state.demands.where((d) => d.status == MaterialDemandStatus.pendingAllocation).length})',
                      isSelected: state.selectedFilterTab == 0,
                      onSelected: () => notifier.setFilterTab(0),
                    ),
                    _buildFilterChip(
                      context,
                      label: 'Alocadas / Em Campo (${state.demands.where((d) => d.status == MaterialDemandStatus.allocatedVehicle || d.status == MaterialDemandStatus.allocatedCentral).length})',
                      isSelected: state.selectedFilterTab == 1,
                      onSelected: () => notifier.setFilterTab(1),
                    ),
                    _buildFilterChip(
                      context,
                      label: 'Concluídas (${state.demands.where((d) => d.status == MaterialDemandStatus.consumedInField).length})',
                      isSelected: state.selectedFilterTab == 2,
                      onSelected: () => notifier.setFilterTab(2),
                    ),
                    Container(width: 1, height: 26, color: AppTheme.darkBorder, margin: const EdgeInsets.symmetric(horizontal: 4)),
                    // Filtro por Tipo de O.S.
                    _buildFilterChip(
                      context,
                      label: 'Todas as Ordens',
                      isSelected: state.selectedTypeFilter == 'ALL',
                      onSelected: () => notifier.setTypeFilter('ALL'),
                    ),
                    _buildFilterChip(
                      context,
                      label: 'Instalação FTTH',
                      isSelected: state.selectedTypeFilter == 'INSTALACAO',
                      onSelected: () => notifier.setTypeFilter('INSTALACAO'),
                      selectedColor: AppTheme.primaryBlue,
                    ),
                    _buildFilterChip(
                      context,
                      label: 'Reparo / Manutenção',
                      isSelected: state.selectedTypeFilter == 'MANUTENCAO',
                      onSelected: () => notifier.setTypeFilter('MANUTENCAO'),
                      selectedColor: AppTheme.accentWarning,
                    ),
                    _buildFilterChip(
                      context,
                      label: 'Logística Reversa / Retirada',
                      isSelected: state.selectedTypeFilter == 'RETIRADA',
                      onSelected: () => notifier.setTypeFilter('RETIRADA'),
                      selectedColor: Colors.purpleAccent,
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // Conteúdo Master-Detail
                Expanded(
                  child: state.isLoadingDemands
                      ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
                      : (isWide
                          ? Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // Coluna Esquerda: Fila de Ordens
                                Expanded(
                                  flex: 4,
                                  child: _buildDemandsList(context, state, notifier),
                                ),
                                const SizedBox(width: 16),
                                // Coluna Direita: Auditoria de Estoque & Técnicos
                                Expanded(
                                  flex: 6,
                                  child: _buildDemandDetails(context, state, notifier, currentUserRole),
                                ),
                              ],
                            )
                          : SingleChildScrollView(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  SizedBox(
                                    height: 300,
                                    child: _buildDemandsList(context, state, notifier),
                                  ),
                                  const SizedBox(height: 16),
                                  _buildDemandDetails(context, state, notifier, currentUserRole),
                                ],
                              ),
                            )),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildFilterChip(
    BuildContext context, {
    required String label,
    required bool isSelected,
    required VoidCallback onSelected,
    Color? selectedColor,
  }) {
    final activeColor = selectedColor ?? AppTheme.primaryBlue;
    return ChoiceChip(
      label: Text(
        label,
        style: TextStyle(
          fontSize: 12,
          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
          color: isSelected ? const Color(0xFF0A0F1D) : AppTheme.textSecondary,
        ),
      ),
      selected: isSelected,
      selectedColor: activeColor,
      backgroundColor: AppTheme.darkSurface,
      side: BorderSide(color: isSelected ? activeColor : AppTheme.darkBorder),
      onSelected: (_) => onSelected(),
    );
  }

  Widget _buildDemandsList(
    BuildContext context,
    DispatchState state,
    DispatchNotifier notifier,
  ) {
    final list = state.filteredDemands;

    if (list.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: AppTheme.darkSurface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppTheme.darkBorder),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: const [
            Icon(Icons.inbox_outlined, size: 40, color: AppTheme.textMuted),
            SizedBox(height: 12),
            Text(
              'Nenhuma O.S. nesta etapa',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 4),
            Text(
              'Novas vendas realizadas aparecerão automaticamente nesta fila.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
          ],
        ),
      );
    }

    return ListView.separated(
      itemCount: list.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (context, index) {
        final item = list[index];
        final isSelected = state.selectedDemand?.id == item.id;

        return InkWell(
          onTap: () => notifier.selectDemand(item),
          borderRadius: BorderRadius.circular(10),
          child: Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: isSelected ? AppTheme.primaryBlue.withValues(alpha: 0.12) : AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(
                color: isSelected ? AppTheme.primaryBlue : AppTheme.darkBorder,
                width: isSelected ? 1.5 : 1,
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        item.customerName,
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: _getStatusColor(item.status).withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        item.status.label,
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: _getStatusColor(item.status),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                // Badge de Tipo de Processo / O.S.
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: _getTypeColor(item.workOrderType).withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(4),
                        border: Border.all(color: _getTypeColor(item.workOrderType).withValues(alpha: 0.35)),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(_getTypeIcon(item.workOrderType), size: 11, color: _getTypeColor(item.workOrderType)),
                          const SizedBox(width: 4),
                          Text(
                            _getTypeLabel(item.workOrderType),
                            style: TextStyle(
                              fontSize: 9,
                              fontWeight: FontWeight.bold,
                              color: _getTypeColor(item.workOrderType),
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (item.maintenanceReason != null) ...[
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          item.maintenanceReason!,
                          style: const TextStyle(fontSize: 10, color: AppTheme.accentWarning, fontStyle: FontStyle.italic),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  item.customerAddress,
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 12,
                  runSpacing: 4,
                  children: [
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.cable, size: 14, color: AppTheme.primaryBlue),
                        const SizedBox(width: 4),
                        Text(
                          'Drop: ${item.estimatedDropMeters}m',
                          style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                        ),
                      ],
                    ),
                    if (item.ctoName != null)
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.hub_outlined, size: 14, color: AppTheme.accentGreen),
                          const SizedBox(width: 4),
                          Text(
                            '${item.ctoName} (P${item.ctoPortNumber ?? 1})',
                            style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                  ],
                ),
                if (item.allocatedTechnicianName != null) ...[
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      const Icon(Icons.person_pin_circle_rounded, size: 14, color: AppTheme.accentGreen),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          'Técnico: ${item.allocatedTechnicianName}',
                          style: const TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.accentGreen,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildDemandDetails(
    BuildContext context,
    DispatchState state,
    DispatchNotifier notifier,
    String? currentUserRole,
  ) {
    final demand = state.selectedDemand;

    if (demand == null) {
      return Container(
        padding: const EdgeInsets.all(32),
        decoration: BoxDecoration(
          color: AppTheme.darkSurface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppTheme.darkBorder),
        ),
        child: const Center(
          child: Text(
            'Selecione uma Ordem de Serviço na fila para auditar o estoque e os técnicos candidatos.',
            style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
          ),
        ),
      );
    }

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Esteira Visual de Processo (Governança e Atribuição de Etapa)
          ProcessLifecycleStepper(
            processTitle: demand.isMaintenance
                ? 'Esteira de Resolução de Incidente & Reparo FTTH (Incident to Restore)'
                : (demand.isRemoval
                    ? 'Esteira de Logística Reversa & Recolhimento de Comodato'
                    : 'Esteira Operacional de Instalação FTTH (Order to Cash)'),
            steps: demand.isMaintenance
                ? _buildMaintenanceSteps(demand)
                : (demand.isRemoval
                    ? _buildRemovalSteps(demand)
                    : _buildInstallationSteps(demand)),
            currentUserRole: currentUserRole,
          ),
          const SizedBox(height: 16),

          // Banner Operacional se a O.S. já estiver despachada/agendada
          if (demand.status == MaterialDemandStatus.allocatedVehicle ||
              demand.status == MaterialDemandStatus.allocatedCentral ||
              demand.allocatedTechnicianName != null) ...[
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              decoration: BoxDecoration(
                color: AppTheme.accentGreen.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.4)),
              ),
              child: Row(
                children: [
                  const Icon(Icons.check_circle_rounded, color: AppTheme.accentGreen, size: 28),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'ORDEM DE SERVIÇO DESPACHADA & AGENDADA',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.accentGreen,
                            letterSpacing: 0.5,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          demand.allocatedTechnicianName != null
                              ? 'Técnico Designado: ${demand.allocatedTechnicianName} (Insumos alocados no veículo)'
                              : 'Insumos alocados para atendimento imediato',
                          style: const TextStyle(fontSize: 12, color: AppTheme.textPrimary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],

          // Card 1: Auditoria de Insumos & Kit de Materiais Obrigatório
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: const [
                        Icon(Icons.inventory_2_outlined, color: AppTheme.primaryBlue, size: 20),
                        SizedBox(width: 8),
                        Text(
                          '1. Kit de Materiais & Triagem de Estoque',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppTheme.accentGreen.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: const Text(
                        'Dimensionamento FTTH Validado',
                        style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppTheme.accentGreen),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                _buildMaterialItem(
                  icon: Icons.router_outlined,
                  title: 'Equipamento ONU / ONT Requerido',
                  value: demand.onuModelRequired,
                  subtitle: 'Definido automaticamente pelas especificações do plano contratado',
                ),
                const Divider(height: 16),
                _buildMaterialItem(
                  icon: Icons.linear_scale_rounded,
                  title: 'Cabo Drop Óptico Autossustentado',
                  value: '${demand.estimatedDropMeters} metros (estimado via GeoCEP com 20% folga)',
                  subtitle: demand.ctoName != null
                      ? 'Origem: Caixa ${demand.ctoName} (Porta ${demand.ctoPortNumber ?? 1}) até o imóvel'
                      : 'Origem: CTO de melhor proximidade calculada',
                ),
                const Divider(height: 16),
                _buildMaterialItem(
                  icon: Icons.cable,
                  title: 'Conectores Rápidos & Acessórios',
                  value: '${demand.fastConnectorsCount}x Conectores SC-APC + ${demand.ptoRosetteCount}x Roseta PTO de Assinante',
                  subtitle: 'Insumos necessários para a conectorização interna e externa',
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Card 2: Candidatos Técnicos & Auditoria de Estoque Veicular
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: const [
                        Icon(Icons.badge_outlined, color: AppTheme.primaryBlue, size: 20),
                        SizedBox(width: 8),
                        Text(
                          '2. Técnicos Candidatos & Kit no Veículo',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                    if (state.isLoadingCandidates)
                      const SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                  ],
                ),
                const SizedBox(height: 6),
                const Text(
                  'O sistema audita em tempo real a custódia de materiais em cada veículo e prioriza quem possui o kit completo.',
                  style: TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 14),

                if (state.candidates.isEmpty && !state.isLoadingCandidates)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 16),
                    child: Center(
                      child: Text(
                        'Nenhum técnico com veículo cadastrado no sistema.',
                        style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                      ),
                    ),
                  )
                else
                  ListView.separated(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: state.candidates.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final tech = state.candidates[index];
                      return _buildCandidateCard(context, tech, demand, state.isDispatching, notifier);
                    },
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMaterialItem({
    required IconData icon,
    required String title,
    required String value,
    required String subtitle,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 20, color: AppTheme.primaryBlue),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              const SizedBox(height: 2),
              Text(value, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.textPrimary)),
              const SizedBox(height: 2),
              Text(subtitle, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildCandidateCard(
    BuildContext context,
    TechnicianCandidateModel tech,
    InstallationDemandModel demand,
    bool isDispatching,
    DispatchNotifier notifier,
  ) {
    final isAssigned = demand.allocatedTechnicianName != null &&
        (demand.allocatedTechnicianName == tech.technicianName ||
            demand.allocatedTechnicianName!.toLowerCase() == tech.technicianName.toLowerCase());
    final isDemandDispatched = demand.status == MaterialDemandStatus.allocatedVehicle ||
        demand.status == MaterialDemandStatus.allocatedCentral;

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isAssigned
            ? AppTheme.accentGreen.withValues(alpha: 0.08)
            : AppTheme.darkCard.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: isAssigned
              ? AppTheme.accentGreen
              : (tech.hasCompleteKit ? AppTheme.accentGreen.withValues(alpha: 0.3) : AppTheme.darkBorder),
          width: isAssigned ? 1.5 : 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 14,
                      backgroundColor: isAssigned
                          ? AppTheme.accentGreen.withValues(alpha: 0.2)
                          : AppTheme.primaryBlue.withValues(alpha: 0.2),
                      child: Text(
                        tech.technicianName.isNotEmpty ? tech.technicianName[0] : 'T',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: isAssigned ? AppTheme.accentGreen : AppTheme.primaryBlue,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  tech.technicianName,
                                  style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              if (isAssigned) ...[
                                const SizedBox(width: 6),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: AppTheme.accentGreen.withValues(alpha: 0.2),
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: const Text(
                                    'ATUAL',
                                    style: TextStyle(
                                      fontSize: 9,
                                      fontWeight: FontWeight.bold,
                                      color: AppTheme.accentGreen,
                                    ),
                                  ),
                                ),
                              ],
                            ],
                          ),
                          Text(
                            tech.vehicleWarehouseName ?? 'Veículo Operacional',
                            style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              if (isAssigned)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: AppTheme.accentGreen.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.accentGreen),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: const [
                      Icon(Icons.check_circle_rounded, size: 14, color: AppTheme.accentGreen),
                      SizedBox(width: 6),
                      Text(
                        'Técnico Designado',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppTheme.accentGreen),
                      ),
                    ],
                  ),
                )
              else if (isDemandDispatched)
                OutlinedButton.icon(
                  onPressed: isDispatching
                      ? null
                      : () => _confirmDispatch(context, tech, demand, notifier, isReassignment: true),
                  icon: const Icon(Icons.swap_horiz_rounded, size: 14),
                  label: const Text('Reatribuir', style: TextStyle(fontSize: 12)),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppTheme.primaryBlue,
                    side: const BorderSide(color: AppTheme.primaryBlue),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                  ),
                )
              else
                ElevatedButton.icon(
                  onPressed: isDispatching
                      ? null
                      : () => _confirmDispatch(context, tech, demand, notifier),
                  icon: const Icon(Icons.send_rounded, size: 14),
                  label: const Text('Despachar', style: TextStyle(fontSize: 12)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryBlue,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 6,
            children: [
              _buildBadge(
                label: tech.hasCompleteKit ? 'Kit Completo no Veículo' : 'Falta Insumos no Veículo',
                color: tech.hasCompleteKit ? AppTheme.accentGreen : AppTheme.accentWarning,
                icon: tech.hasCompleteKit ? Icons.check_circle : Icons.warning_amber,
              ),
              _buildBadge(
                label: 'Drop: ${tech.dropCableBalanceMeters}m',
                color: tech.hasDropCable ? AppTheme.primaryBlue : AppTheme.accentError,
                icon: Icons.cable,
              ),
              if (tech.distanceKmToCustomer != null)
                _buildBadge(
                  label: '${tech.distanceKmToCustomer!.toStringAsFixed(1)} km do cliente',
                  color: AppTheme.textSecondary,
                  icon: Icons.navigation_outlined,
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBadge({
    required String label,
    required Color color,
    required IconData icon,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: color),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: color),
          ),
        ],
      ),
    );
  }

  void _confirmDispatch(
    BuildContext context,
    TechnicianCandidateModel tech,
    InstallationDemandModel demand,
    DispatchNotifier notifier, {
    bool isReassignment = false,
  }) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          title: Text(isReassignment ? 'Confirmar Reatribuição de O.S.' : 'Confirmar Despacho de O.S.'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                isReassignment
                    ? 'Deseja reatribuir esta O.S. para o técnico ${tech.technicianName}?'
                    : 'Deseja despachar esta instalação para o técnico ${tech.technicianName}?',
              ),
              const SizedBox(height: 12),
              Text('Cliente: ${demand.customerName}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
              Text('Endereço: ${demand.customerAddress}', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              Text('Insumo ONU: ${demand.onuModelRequired}', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              Text('Cabo Drop: ${demand.estimatedDropMeters}m', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              const SizedBox(height: 12),
              Text(
                isReassignment
                    ? 'Ao confirmar, a O.S. será transferida e os materiais serão realocados no veículo de ${tech.technicianName}.'
                    : 'Ao confirmar, a O.S. será agendada e os materiais serão alocados no estoque do veículo.',
                style: const TextStyle(fontSize: 11, color: AppTheme.accentGreen),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                Navigator.pop(ctx);
                final success = await notifier.dispatchToTechnician(tech.technicianId);
                if (context.mounted && success) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      backgroundColor: AppTheme.accentGreen,
                      behavior: SnackBarBehavior.floating,
                      duration: const Duration(seconds: 4),
                      content: Row(
                        children: [
                          const Icon(Icons.check_circle_rounded, color: Colors.white, size: 20),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              'O.S. despachada com sucesso para ${tech.technicianName}! Movida para Alocadas / Em Campo.',
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }
              },
              child: Text(isReassignment ? 'Confirmar Reatribuição' : 'Confirmar & Despachar'),
            ),
          ],
        );
      },
    );
  }

  Color _getStatusColor(MaterialDemandStatus status) {
    switch (status) {
      case MaterialDemandStatus.pendingAllocation:
        return AppTheme.accentWarning;
      case MaterialDemandStatus.allocatedVehicle:
      case MaterialDemandStatus.allocatedCentral:
        return AppTheme.accentGreen;
      case MaterialDemandStatus.consumedInField:
        return AppTheme.primaryBlue;
      case MaterialDemandStatus.cancelled:
        return AppTheme.accentError;
    }
  }

  List<ProcessLifecycleStep> _buildInstallationSteps(InstallationDemandModel demand) {
    final isPending = demand.status == MaterialDemandStatus.pendingAllocation;
    final isDispatched = demand.status == MaterialDemandStatus.allocatedVehicle ||
        demand.status == MaterialDemandStatus.allocatedCentral;
    final isCompleted = demand.status == MaterialDemandStatus.consumedInField;

    return [
      ProcessLifecycleStep(
        id: 'step_sale',
        title: '1. Venda & Contrato',
        subtitle: demand.contractNumber != null ? 'Contrato ${demand.contractNumber}' : 'Contrato Formalizado',
        responsibleRoleName: 'Vendas',
        allowedRoles: const ['SALES', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.assignment_turned_in_rounded,
        popGuideTitle: 'POP-VEN-01: Formalização e Assinatura de Contrato',
        popGuideContent:
            '1. Objetivo: Garantir a captação correta dos dados cadastrais, endereço com viabilidade técnica e plano escolhido.\n\n'
            '2. Entradas: Documento de identificação, comprovante de residência e assinatura do contrato.\n\n'
            '3. Saída: Contrato formalizado e emissão automática da Ordem de Serviço de Instalação no sistema.\n\n'
            '4. Regra de Governança: Somente contratos formalizados liberam a triagem técnica de insumos.',
      ),
      ProcessLifecycleStep(
        id: 'step_triage',
        title: '2. Triagem FTTH',
        subtitle: demand.ctoName != null
            ? 'CTO ${demand.ctoName} (${demand.estimatedDropMeters}m)'
            : '${demand.estimatedDropMeters}m Cabo Drop',
        responsibleRoleName: 'Torre de Controle',
        allowedRoles: const ['SUPPORT_ANALYST', 'SUPPORT_N2', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.cable_rounded,
        popGuideTitle: 'POP-ENG-02: Dimensionamento de Materiais FTTH',
        popGuideContent:
            '1. Objetivo: Calcular a metragem necessária de cabo drop autossustentado e identificar a CTO mais próxima.\n\n'
            '2. Critério de Cálculo: Distância geodésica com 20% de folga técnica para curvas e ancoragem no poste.\n\n'
            '3. Equipamentos: Modelo de ONU compatível com a velocidade contratada e kit de conectores rápidos SC-APC.',
      ),
      ProcessLifecycleStep(
        id: 'step_dispatch',
        title: '3. Despacho & Veículo',
        subtitle: demand.allocatedTechnicianName != null
            ? 'Técnico: ${demand.allocatedTechnicianName}'
            : 'Aguardando Alocação',
        responsibleRoleName: 'Torre de Controle',
        allowedRoles: const ['SUPPORT_ANALYST', 'SUPPORT_N2', 'ADMIN'],
        isCompleted: isDispatched || isCompleted,
        isActive: isPending,
        assignedPersonName: demand.allocatedTechnicianName,
        icon: Icons.local_shipping_rounded,
        popGuideTitle: 'POP-DSP-03: Despacho e Custódia de Estoque Veicular',
        popGuideContent:
            '1. Objetivo: Alocar a O.S. para um técnico em campo que possua kit completo (drop, ONU e conectores) no veículo operacional.\n\n'
            '2. Verificações: Priorizar técnicos com menor distância GPS até o imóvel e saldo positivo de insumos no veículo.\n\n'
            '3. Ação: Selecionar o técnico candidato recomendado e confirmar o despacho para agendamento imediato.\n\n'
            '4. Exceção: Se nenhum veículo tiver insumos completos, confirmar separação no Almoxarifado Central.',
      ),
      ProcessLifecycleStep(
        id: 'step_field',
        title: '4. Instalação em Campo',
        subtitle: isCompleted
            ? 'Instalação Concluída'
            : (isDispatched ? 'Em Atendimento no Imóvel' : 'Aguardando Despacho'),
        responsibleRoleName: 'Técnico de Campo',
        allowedRoles: const ['TECHNICIAN', 'ADMIN'],
        isCompleted: isCompleted,
        isActive: isDispatched,
        assignedPersonName: demand.allocatedTechnicianName,
        icon: Icons.engineering_rounded,
        popGuideTitle: 'POP-CAM-04: Conectorização e Ancoragem FTTH',
        popGuideContent:
            '1. Objetivo: Lançamento do drop óptico, conectorização na CTO e instalação da roseta PTO na residência.\n\n'
            '2. Validação Óptica: Medição no Power Meter no ponto de terminação (potência permitida entre -15 dBm e -25 dBm).\n\n'
            '3. Finalização: Leitura do serial/MAC da ONU e coleta da confirmação de entrega do serviço.',
      ),
      ProcessLifecycleStep(
        id: 'step_activation',
        title: '5. Ativação & Faturamento',
        subtitle: isCompleted ? 'Cliente Navegando' : 'Aguardando Conclusão',
        responsibleRoleName: 'Financeiro / Sistema',
        allowedRoles: const ['FINANCIAL', 'ADMIN'],
        isCompleted: isCompleted,
        isActive: false,
        icon: Icons.verified_user_rounded,
        popGuideTitle: 'POP-FIN-05: Ativação de Acesso e Ciclo de Faturamento',
        popGuideContent:
            '1. Objetivo: Provisionamento automático na OLT via TR-069/RADIUS e início da régua de faturamento no Xingubit Pay.\n\n'
            '2. Validação: Checagem do status PPPoE/IPoE ativo e geração do primeiro ciclo proporcional.',
      ),
    ];
  }

  List<ProcessLifecycleStep> _buildMaintenanceSteps(InstallationDemandModel demand) {
    final isPending = demand.status == MaterialDemandStatus.pendingAllocation;
    final isDispatched = demand.status == MaterialDemandStatus.allocatedVehicle ||
        demand.status == MaterialDemandStatus.allocatedCentral;
    final isCompleted = demand.status == MaterialDemandStatus.consumedInField;

    return [
      ProcessLifecycleStep(
        id: 'step_man_triage',
        title: '1. Abertura & Triagem N1',
        subtitle: demand.maintenanceReason ?? 'Incidente Reportado via SAC',
        responsibleRoleName: 'Suporte N1',
        allowedRoles: const ['SUPPORT', 'SUPPORT_ANALYST', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.headset_mic_rounded,
        popGuideTitle: 'POP-MAN-01: Triagem de Incidente e Teste Inicial',
        popGuideContent:
            '1. Objetivo: Validar ausência de corte massivo na região, verificar alimentação elétrica da ONU e estado dos LEDs.\n\n'
            '2. Testes Remotos: Teste de ping e consulta de logs no concentrador RADIUS.\n\n'
            '3. Ação: Se problema persistir no enlace óptico, encaminhar chamado para o N2 com histórico de atenuação.',
      ),
      ProcessLifecycleStep(
        id: 'step_man_diag',
        title: '2. Diagnóstico Remoto N2',
        subtitle: demand.ctoName != null
            ? 'Porta OLT / CTO ${demand.ctoName}'
            : 'Análise de Flap e Potência Óptica',
        responsibleRoleName: 'NOC / Suporte N2',
        allowedRoles: const ['SUPPORT_N2', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.router_rounded,
        popGuideTitle: 'POP-NOC-02: Diagnóstico Avançado de Atenuação Óptica',
        popGuideContent:
            '1. Objetivo: Consultar níveis de potência óptica recebida (Rx Power) e enviada (Tx Power) na OLT via SNMP/TR-069.\n\n'
            '2. Critérios de Alarme:\n- Sinal abaixo de -27 dBm: Alta atenuação (curvatura, conector sujo ou splitter avariado).\n- Sinal = -40 dBm ou LOS: Rompimento total de fibra.\n\n'
            '3. Decisão: Emitir Ordem de Serviço de Reparo Físico e definir materiais prioritários.',
      ),
      ProcessLifecycleStep(
        id: 'step_man_dispatch',
        title: '3. Despacho & Ferramental',
        subtitle: demand.allocatedTechnicianName != null
            ? 'Técnico: ${demand.allocatedTechnicianName}'
            : 'Aguardando Alocação',
        responsibleRoleName: 'Torre de Controle',
        allowedRoles: const ['SUPPORT_ANALYST', 'SUPPORT_N2', 'ADMIN'],
        isCompleted: isDispatched || isCompleted,
        isActive: isPending,
        assignedPersonName: demand.allocatedTechnicianName,
        icon: Icons.build_circle_rounded,
        popGuideTitle: 'POP-DSP-03: Alocação de Equipe com Ferramental de Manutenção',
        popGuideContent:
            '1. Objetivo: Despachar a O.S. para um técnico em trânsito com máquina de fusão, clivador e Power Meter calibrado no veículo.\n\n'
            '2. Verificação de Saldo: Garantir saldo positivo de conectores rápidos e drop de reposição no veículo.\n\n'
            '3. Roteamento: Priorizar equipe mais próxima geograficamente para cumprir o SLA contratual de restabelecimento.',
      ),
      ProcessLifecycleStep(
        id: 'step_man_field',
        title: '4. Atendimento & Medição Óptica',
        subtitle: isCompleted
            ? 'Reparo Executado e Atestado'
            : (isDispatched ? 'Técnico em Campo no Imóvel' : 'Aguardando Despacho'),
        responsibleRoleName: 'Técnico de Campo',
        allowedRoles: const ['TECHNICIAN', 'ADMIN'],
        isCompleted: isCompleted,
        isActive: isDispatched,
        assignedPersonName: demand.allocatedTechnicianName,
        icon: Icons.engineering_rounded,
        popGuideTitle: 'POP-CAM-04: Procedimento de Reparo Físico e Medição de Potência',
        popGuideContent:
            '1. Medição Inicial: Conectar o Power Meter na ponta do drop e aferir o sinal óptico na entrada do imóvel.\n\n'
            '2. Troca/Refação: Se sinal atenuado, clivar e refazer o conector SC-APC ou fundir nova ponta. Se fibra rompida no vão, lançar novo trecho de drop com ancoragem reforçada.\n\n'
            '3. Padrão de Homologação: O sinal no conector da ONU DEVE estar entre -15.0 dBm e -24.0 dBm.\n\n'
            '4. Coleta de Evidências: Fotografar a medição do Power Meter e colher assinatura do cliente no aplicativo.',
      ),
      ProcessLifecycleStep(
        id: 'step_man_closing',
        title: '5. Fechamento & Homologação',
        subtitle: isCompleted ? 'Conexão Estável & Normalizada' : 'Aguardando Validação',
        responsibleRoleName: 'Sistema / Atendimento',
        allowedRoles: const ['SUPPORT', 'SUPPORT_ANALYST', 'ADMIN'],
        isCompleted: isCompleted,
        isActive: false,
        icon: Icons.verified_rounded,
        popGuideTitle: 'POP-SAC-05: Encerramento do Incidente e Pesquisa de Satisfação',
        popGuideContent:
            '1. Objetivo: Verificar status de conexão PPPoE online no concentrador NAS e ausência de perda de pacotes.\n\n'
            '2. Baixa de Insumos: Registrar conectores ou metragem de cabo utilizados no reparo para baixa no Kardex do veículo.\n\n'
            '3. Notificação ao Cliente: Disparar mensagem automática via WhatsApp comunicando a conclusão do chamado e solicitando nota de avaliação do atendimento (NPS / CSAT).',
      ),
    ];
  }

  List<ProcessLifecycleStep> _buildRemovalSteps(InstallationDemandModel demand) {
    final isPending = demand.status == MaterialDemandStatus.pendingAllocation;
    final isDispatched = demand.status == MaterialDemandStatus.allocatedVehicle ||
        demand.status == MaterialDemandStatus.allocatedCentral;
    final isCompleted = demand.status == MaterialDemandStatus.consumedInField;

    return [
      ProcessLifecycleStep(
        id: 'step_rem_retention',
        title: '1. Rescisão & Retenção',
        subtitle: demand.contractNumber != null ? 'Contrato ${demand.contractNumber}' : 'Rescisão Contratual',
        responsibleRoleName: 'Retenção / SAC',
        allowedRoles: const ['SALES', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.cancel_presentation_rounded,
        popGuideTitle: 'POP-RET-01: Homologação de Rescisão e Tentativa de Retenção',
        popGuideContent:
            '1. Objetivo: Investigar a causa-raiz do cancelamento e ofertar alternativas viáveis de retenção.\n\n'
            '2. Homologação: Caso o cliente confirme o cancelamento, registrar a rescisão e gerar a solicitação de logística reversa de comodato.',
      ),
      ProcessLifecycleStep(
        id: 'step_rem_audit',
        title: '2. Auditoria de Comodato',
        subtitle: '${demand.onuModelRequired} + Acessórios',
        responsibleRoleName: 'Financeiro / Almoxarifado',
        allowedRoles: const ['FINANCIAL', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.inventory_2_outlined,
        popGuideTitle: 'POP-FIN-02: Levantamento Patrimonial de Ativos em Comodato',
        popGuideContent:
            '1. Objetivo: Identificar no contrato os seriais da ONU e Roteador Wi-Fi cedidos em comodato ao cliente.\n\n'
            '2. Saída: Geração da Ordem de Serviço de Recolhimento com lista dos equipamentos a resgatar.',
      ),
      ProcessLifecycleStep(
        id: 'step_rem_dispatch',
        title: '3. Despacho de Recolha',
        subtitle: demand.allocatedTechnicianName != null
            ? 'Técnico: ${demand.allocatedTechnicianName}'
            : 'Aguardando Alocação',
        responsibleRoleName: 'Torre de Controle',
        allowedRoles: const ['SUPPORT_ANALYST', 'SUPPORT_N2', 'ADMIN'],
        isCompleted: isDispatched || isCompleted,
        isActive: isPending,
        assignedPersonName: demand.allocatedTechnicianName,
        icon: Icons.local_shipping_rounded,
        popGuideTitle: 'POP-DSP-03: Agendamento e Despacho de Coleta',
        popGuideContent:
            '1. Objetivo: Despachar a rota de recolhimento para equipe técnica ou motorista operacional.\n\n'
            '2. Roteamento: Agrupar coletas no mesmo bairro das instalações para economia de combustível.',
      ),
      ProcessLifecycleStep(
        id: 'step_rem_field',
        title: '4. Coleta & Termo de Devolução',
        subtitle: isCompleted
            ? 'Equipamentos Coletados'
            : (isDispatched ? 'Em Atendimento no Imóvel' : 'Aguardando Coleta'),
        responsibleRoleName: 'Técnico de Campo',
        allowedRoles: const ['TECHNICIAN', 'ADMIN'],
        isCompleted: isCompleted,
        isActive: isDispatched,
        assignedPersonName: demand.allocatedTechnicianName,
        icon: Icons.assignment_turned_in_outlined,
        popGuideTitle: 'POP-CAM-04: Coleta de Equipamentos e Termo de Entrega',
        popGuideContent:
            '1. Objetivo: Recolher a ONU, fonte de alimentação, cabo de rede e roteador Wi-Fi.\n\n'
            '2. Assinatura: Colher o Termo de Devolução assinado pelo cliente ou responsável maior de idade.',
      ),
      ProcessLifecycleStep(
        id: 'step_rem_warehouse',
        title: '5. Triagem & Recondicionamento',
        subtitle: isCompleted ? 'Ativo Reintegrado ao Kardex' : 'Aguardando Entrada no Central',
        responsibleRoleName: 'Almoxarifado',
        allowedRoles: const ['SUPPORT_ANALYST', 'ADMIN'],
        isCompleted: isCompleted,
        isActive: false,
        icon: Icons.check_circle_outline_rounded,
        popGuideTitle: 'POP-ALM-05: Teste de Bancada, Reset e Reintegração ao Estoque',
        popGuideContent:
            '1. Teste: Conectar na bancada de testes, efetuar o reset de fábrica e checar portas LAN e Wi-Fi.\n\n'
            '2. Recondicionamento: Higienizar, colocar nova embalagem e reincorporar o ativo no Kardex como "Disponível para Instalação".',
      ),
    ];
  }

  Color _getTypeColor(String type) {
    switch (type.toUpperCase().trim()) {
      case 'MANUTENCAO':
        return AppTheme.accentWarning;
      case 'RETIRADA':
        return Colors.purpleAccent;
      case 'INSTALACAO':
      default:
        return AppTheme.primaryBlue;
    }
  }

  IconData _getTypeIcon(String type) {
    switch (type.toUpperCase().trim()) {
      case 'MANUTENCAO':
        return Icons.build_rounded;
      case 'RETIRADA':
        return Icons.assignment_return_rounded;
      case 'INSTALACAO':
      default:
        return Icons.add_circle_outline_rounded;
    }
  }

  String _getTypeLabel(String type) {
    switch (type.toUpperCase().trim()) {
      case 'MANUTENCAO':
        return 'REPARO / MANUTENÇÃO';
      case 'RETIRADA':
        return 'LOGÍSTICA REVERSA';
      case 'INSTALACAO':
      default:
        return 'NOVA INSTALAÇÃO';
    }
  }
}
