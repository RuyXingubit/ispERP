import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/dispatch_models.dart';
import '../data/dispatch_notifier.dart';

/// Torre de Controle de Despacho Técnico e Triagem de Estoque de Materiais.
class DispatchControlTowerScreen extends ConsumerWidget {
  const DispatchControlTowerScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(dispatchProvider);
    final notifier = ref.read(dispatchProvider.notifier);

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

                // Abas de Filtro da Fila de Instalação
                Wrap(
                  spacing: 10,
                  runSpacing: 8,
                  children: [
                    _buildFilterChip(
                      context,
                      label: 'Aguardando Triagem / Despacho (${state.demands.where((d) => d.status == MaterialDemandStatus.pendingAllocation).length})',
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
                      label: 'Concluídas & Ativadas (${state.demands.where((d) => d.status == MaterialDemandStatus.consumedInField).length})',
                      isSelected: state.selectedFilterTab == 2,
                      onSelected: () => notifier.setFilterTab(2),
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
                                  child: _buildDemandDetails(context, state, notifier),
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
                                  _buildDemandDetails(context, state, notifier),
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
  }) {
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
      selectedColor: AppTheme.primaryBlue,
      backgroundColor: AppTheme.darkSurface,
      side: BorderSide(color: isSelected ? AppTheme.primaryBlue : AppTheme.darkBorder),
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
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppTheme.darkCard.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: tech.hasCompleteKit ? AppTheme.accentGreen.withValues(alpha: 0.3) : AppTheme.darkBorder,
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
                      backgroundColor: AppTheme.primaryBlue.withValues(alpha: 0.2),
                      child: Text(
                        tech.technicianName.isNotEmpty ? tech.technicianName[0] : 'T',
                        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppTheme.primaryBlue),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            tech.technicianName,
                            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                            overflow: TextOverflow.ellipsis,
                          ),
                          Text(
                            tech.vehicleWarehouseName ?? 'Veículo Operacional',
                            style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
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
    DispatchNotifier notifier,
  ) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          title: const Text('Confirmar Despacho de O.S.'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Deseja despachar esta instalação para o técnico ${tech.technicianName}?'),
              const SizedBox(height: 12),
              Text('Cliente: ${demand.customerName}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
              Text('Endereço: ${demand.customerAddress}', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              Text('Insumo ONU: ${demand.onuModelRequired}', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              Text('Cabo Drop: ${demand.estimatedDropMeters}m', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              const SizedBox(height: 12),
              const Text(
                'Ao confirmar, a O.S. será agendada e os materiais serão alocados no estoque do veículo.',
                style: TextStyle(fontSize: 11, color: AppTheme.accentGreen),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.pop(ctx);
                notifier.dispatchToTechnician(tech.technicianId);
              },
              child: const Text('Confirmar & Despachar'),
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
}
