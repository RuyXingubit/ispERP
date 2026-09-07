import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/process_lifecycle_stepper.dart';
import '../../dispatch/data/dispatch_models.dart';
import '../data/inventory_models.dart';
import '../data/inventory_notifier.dart';

class InventoryScreen extends ConsumerStatefulWidget {
  const InventoryScreen({super.key});

  @override
  ConsumerState<InventoryScreen> createState() => _InventoryScreenState();
}

class _InventoryScreenState extends ConsumerState<InventoryScreen> {
  String? _selectedRemovalDemandId;

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(inventoryProvider);
    final notifier = ref.read(inventoryProvider.notifier);
    final authState = ref.watch(authProvider);
    final currentUserRole = authState.role?.name.toUpperCase();

    ref.listen<InventoryState>(inventoryProvider, (_, next) {
      if (next.successMessage != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.successMessage!),
            backgroundColor: AppTheme.accentGreen,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
      if (next.errorMessage != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            backgroundColor: AppTheme.accentError,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    });

    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildHeader(context, state, notifier),
                _buildTabsBar(context, state, notifier),
                Expanded(
                  child: IndexedStack(
                    index: state.selectedTab,
                    children: [
                      _buildWorkOrdersTab(context, state, notifier),
                      _buildBalancesTab(context, state, notifier),
                      _buildTransfersTab(context, state, notifier),
                      _buildAuditingTab(context, state, notifier),
                      _buildReverseLogisticsTab(context, state, notifier, currentUserRole),
                    ],
                  ),
                ),
              ],
            ),
    );
  }

  // ---------------------------------------------------------------------------
  // HEADER COM SELETOR DE BASE E MÉTRICAS REAIS
  // ---------------------------------------------------------------------------
  Widget _buildHeader(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    final selectedW = state.selectedWarehouse;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      decoration: const BoxDecoration(
        color: AppTheme.darkSurface,
        border: Border(bottom: BorderSide(color: AppTheme.darkBorder)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.inventory_2_outlined, color: AppTheme.primaryBlue, size: 28),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Almoxarifado Central & Cadeia de Custódia',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      selectedW != null
                          ? 'Base Ativa: ${selectedW.name} (${selectedW.city}/${selectedW.state}) • Resp: ${selectedW.responsibleName ?? 'Almoxarife Sede'}'
                          : 'Visão Consolidada de Todas as Bases e Veículos',
                      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              if (state.warehouses.isNotEmpty) ...[
                const SizedBox(width: 16),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppTheme.darkBg,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.darkBorder),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String>(
                      dropdownColor: AppTheme.darkSurface,
                      value: selectedW?.id,
                      hint: const Text('Selecione a Base', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                      icon: const Icon(Icons.arrow_drop_down, color: AppTheme.primaryBlue),
                      items: state.warehouses.map((w) {
                        return DropdownMenuItem<String>(
                          value: w.id,
                          child: Text(
                            '${w.name} (${w.city})',
                            style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13),
                          ),
                        );
                      }).toList(),
                      onChanged: (val) {
                        if (val != null) {
                          final found = state.warehouses.where((w) => w.id == val).toList();
                          if (found.isNotEmpty) {
                            notifier.selectWarehouse(found.first);
                          }
                        }
                      },
                    ),
                  ),
                ),
              ],
              const SizedBox(width: 12),
              IconButton(
                onPressed: () => notifier.loadAll(),
                icon: const Icon(Icons.refresh, color: AppTheme.textSecondary),
                tooltip: 'Atualizar Dados',
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Métricas Rápidas
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _buildMetricChip(
                  label: 'Itens no Catálogo',
                  value: '${state.items.length}',
                  icon: Icons.category_outlined,
                  color: AppTheme.primaryBlue,
                ),
                const SizedBox(width: 12),
                _buildMetricChip(
                  label: 'Aguardando Triagem O.S.',
                  value: '${state.pendingDemandsCount}',
                  icon: Icons.hourglass_top_outlined,
                  color: state.pendingDemandsCount > 0 ? AppTheme.accentWarning : AppTheme.textSecondary,
                ),
                const SizedBox(width: 12),
                _buildMetricChip(
                  label: 'Alertas Estoque Crítico',
                  value: '${state.criticalItemsCount}',
                  icon: Icons.warning_amber_rounded,
                  color: state.criticalItemsCount > 0 ? AppTheme.accentError : AppTheme.accentGreen,
                ),
                const SizedBox(width: 12),
                _buildMetricChip(
                  label: 'Guias em Trânsito',
                  value: '${state.transfers.where((t) => t.status == TransferStatus.inTransit).length}',
                  icon: Icons.local_shipping_outlined,
                  color: AppTheme.primaryBlue,
                ),
                const SizedBox(width: 12),
                _buildMetricChip(
                  label: 'Logística Reversa',
                  value: '${state.reverseLogisticsCount}',
                  icon: Icons.assignment_return_outlined,
                  color: Colors.purpleAccent,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricChip({
    required String label,
    required String value,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withValues(alpha: 0.25)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 18, color: color),
          const SizedBox(width: 8),
          Text(label, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
          const SizedBox(width: 6),
          Text(
            value,
            style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: color),
          ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // BARRA DE ABAS
  // ---------------------------------------------------------------------------
  Widget _buildTabsBar(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    return Container(
      decoration: const BoxDecoration(
        color: AppTheme.darkSurface,
        border: Border(bottom: BorderSide(color: AppTheme.darkBorder)),
      ),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            _buildTabButton(0, 'Triagem & Saídas por O.S.', Icons.assignment_outlined, state.selectedTab == 0, notifier),
            _buildTabButton(1, 'Saldos & Entradas', Icons.inventory_outlined, state.selectedTab == 1, notifier),
            _buildTabButton(2, 'Transferências & Trânsito', Icons.sync_alt, state.selectedTab == 2, notifier),
            _buildTabButton(3, 'Auditoria de Divergências', Icons.fact_check_outlined, state.selectedTab == 3, notifier),
            _buildTabButton(4, 'Logística Reversa (Comodato)', Icons.assignment_return_outlined, state.selectedTab == 4, notifier),
          ],
        ),
      ),
    );
  }

  Widget _buildTabButton(int index, String title, IconData icon, bool isSelected, InventoryNotifier notifier) {
    return InkWell(
      onTap: () => notifier.setTab(index),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: isSelected ? AppTheme.primaryBlue : Colors.transparent,
              width: 2.5,
            ),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 18, color: isSelected ? AppTheme.primaryBlue : AppTheme.textSecondary),
            const SizedBox(width: 8),
            Text(
              title,
              style: TextStyle(
                fontSize: 13,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                color: isSelected ? AppTheme.textPrimary : AppTheme.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // ABA 0: TRIAGEM & SAÍDAS POR O.S.
  // ---------------------------------------------------------------------------
  Widget _buildWorkOrdersTab(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    final pendingDemands = state.demands.where((d) => d.status == MaterialDemandStatus.pendingAllocation).toList();
    final allocatedDemands = state.demands.where((d) => d.status == MaterialDemandStatus.allocatedCentral).toList();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'Fila de Triagem de Materiais para O.S. de Instalação',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Confirme a separação no Depósito Central para liberar o agendamento pelo Supervisor Técnico.',
                      style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              Wrap(
                spacing: 8,
                children: [
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppTheme.primaryBlue,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                    ),
                    onPressed: () => _showCheckoutModal(context, state, notifier),
                    icon: const Icon(Icons.outbox, size: 18),
                    label: const Text('Retirada com O.S.'),
                  ),
                  OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppTheme.primaryBlue,
                      side: const BorderSide(color: AppTheme.primaryBlue),
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                    ),
                    onPressed: () => _showCheckinModal(context, state, notifier),
                    icon: const Icon(Icons.move_to_inbox, size: 18),
                    label: const Text('Devolução & Conferência'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (pendingDemands.isEmpty)
            _buildEmptyState('Nenhuma O.S. aguardando triagem no momento.', Icons.check_circle_outline)
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: pendingDemands.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final demand = pendingDemands[index];
                return _buildDemandCard(context, demand, notifier, state.isSubmitting);
              },
            ),
          if (allocatedDemands.isNotEmpty) ...[
            const SizedBox(height: 32),
            Text(
              'O.S. com Materiais Confirmados no Almoxarifado (${allocatedDemands.length})',
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppTheme.accentGreen),
            ),
            const SizedBox(height: 12),
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: allocatedDemands.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final d = allocatedDemands[index];
                return Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle, size: 18, color: AppTheme.accentGreen),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          '${d.customerName} • Contrato: ${d.contractNumber ?? 'N/A'} • Kit Separado (${d.estimatedDropMeters}m Cabo Drop + ${d.onuModelRequired})',
                          style: const TextStyle(fontSize: 12, color: AppTheme.textPrimary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: AppTheme.accentGreen.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: const Text('Pronto p/ Despacho', style: TextStyle(fontSize: 11, color: AppTheme.accentGreen, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                );
              },
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildDemandCard(BuildContext context, InstallationDemandModel demand, InventoryNotifier notifier, bool isSubmitting) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppTheme.darkBorder),
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
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppTheme.accentWarning.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text('Aguardando Almoxarifado', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppTheme.accentWarning)),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      demand.contractNumber != null ? 'Contrato: ${demand.contractNumber}' : 'O.S.: ${demand.workOrderId.substring(0, 8)}...',
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                    ),
                  ],
                ),
              ),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.accentGreen,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                ),
                onPressed: isSubmitting ? null : () => notifier.confirmStockForWorkOrder(demand.workOrderId),
                icon: const Icon(Icons.check, size: 16),
                label: const Text('Confirmar & Separar Materiais', style: TextStyle(fontSize: 12)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            demand.customerName,
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
          ),
          const SizedBox(height: 2),
          Text(
            demand.customerAddress,
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 12),
          const Divider(height: 1, color: AppTheme.darkBorder),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 6,
            children: [
              _buildBadge('Cabo Drop: ${demand.estimatedDropMeters} metros', Icons.linear_scale, AppTheme.primaryBlue),
              _buildBadge('ONU: ${demand.onuModelRequired}', Icons.router_outlined, AppTheme.primaryBlue),
              _buildBadge('${demand.fastConnectorsCount}x Conectores SC/APC', Icons.cable, AppTheme.textSecondary),
              _buildBadge('${demand.ptoRosetteCount}x Roseta PTO', Icons.crop_square, AppTheme.textSecondary),
              if (demand.ctoName != null)
                _buildBadge('CTO: ${demand.ctoName} (Porta ${demand.ctoPortNumber ?? 1})', Icons.alt_route, AppTheme.accentGreen),
            ],
          ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // ABA 1: SALDOS & ENTRADA DE MATERIAIS
  // ---------------------------------------------------------------------------
  Widget _buildBalancesTab(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'Saldos Físicos no Almoxarifado',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Controle dinâmico de insumos a granel, bobinas de fibra e equipamentos serializados.',
                      style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryBlue,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                ),
                onPressed: () => _showStockEntryModal(context, state, notifier),
                icon: const Icon(Icons.add_box_outlined, size: 18),
                label: const Text('Registrar Entrada / Compra'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (state.items.isEmpty)
            _buildEmptyState('Nenhum item em estoque cadastrado.', Icons.inventory_2_outlined)
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: state.items.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final item = state.items[index];
                return Material(
                  color: Colors.transparent,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(8),
                    onTap: () => _showItemHistoryModal(context, item, state, notifier),
                    hoverColor: AppTheme.primaryBlue.withValues(alpha: 0.08),
                    child: Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppTheme.darkSurface,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: item.isCriticalStock ? AppTheme.accentError.withValues(alpha: 0.4) : AppTheme.darkBorder,
                        ),
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: (item.isCriticalStock ? AppTheme.accentError : AppTheme.primaryBlue).withValues(alpha: 0.12),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Icon(
                              item.isCriticalStock ? Icons.warning_amber_rounded : Icons.inventory_2,
                              size: 20,
                              color: item.isCriticalStock ? AppTheme.accentError : AppTheme.primaryBlue,
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Flexible(
                                      child: Text(
                                        item.name,
                                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                      decoration: BoxDecoration(
                                        color: AppTheme.darkBg,
                                        borderRadius: BorderRadius.circular(4),
                                        border: Border.all(color: AppTheme.darkBorder),
                                      ),
                                      child: Text(item.code, style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary)),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  'Categoria: ${item.category} • Mínimo de Segurança: ${item.minQuantity} ${item.unit}',
                                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                ),
                              ],
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(
                                '${item.quantityInStock} ${item.unit}',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                  color: item.isCriticalStock ? AppTheme.accentError : AppTheme.accentGreen,
                                ),
                              ),
                              Text(
                                item.isCriticalStock ? 'Estoque Crítico' : 'Normal',
                                style: TextStyle(
                                  fontSize: 11,
                                  color: item.isCriticalStock ? AppTheme.accentError : AppTheme.textSecondary,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(width: 12),
                          const Icon(
                            Icons.arrow_forward_ios_rounded,
                            size: 14,
                            color: AppTheme.textSecondary,
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // ABA 2: TRANSFERÊNCIAS & EM TRÂNSITO (HANDSHAKE)
  // ---------------------------------------------------------------------------
  Widget _buildTransfersTab(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Transferências Inter-Bases & Cadeia de Custódia',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Carga sob custódia retida no CPF do portador durante o transporte, com confirmação no destino.',
                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                ],
              ),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryBlue,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                ),
                onPressed: () => _showCreateTransferModal(context, state, notifier),
                icon: const Icon(Icons.add, size: 18),
                label: const Text('Nova Transferência'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (state.transfers.isEmpty)
            _buildEmptyState('Nenhuma guia de transferência registrada.', Icons.local_shipping_outlined)
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: state.transfers.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final tr = state.transfers[index];
                return Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppTheme.darkBorder),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: tr.status == TransferStatus.inTransit
                                      ? AppTheme.primaryBlue.withValues(alpha: 0.15)
                                      : (tr.status == TransferStatus.received ? AppTheme.accentGreen.withValues(alpha: 0.15) : AppTheme.darkBorder),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  tr.status.label,
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                    color: tr.status == TransferStatus.inTransit
                                        ? AppTheme.primaryBlue
                                        : (tr.status == TransferStatus.received ? AppTheme.accentGreen : AppTheme.textSecondary),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text(tr.code, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.textPrimary)),
                            ],
                          ),
                          if (tr.status == TransferStatus.pending)
                            ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryBlue, foregroundColor: Colors.white),
                              onPressed: () => notifier.dispatchTransfer(tr.id),
                              icon: const Icon(Icons.send, size: 14),
                              label: const Text('Despachar', style: TextStyle(fontSize: 11)),
                            )
                          else if (tr.status == TransferStatus.inTransit)
                            ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentGreen, foregroundColor: Colors.white),
                              onPressed: () => notifier.receiveTransfer(tr.id),
                              icon: const Icon(Icons.check_circle_outline, size: 14),
                              label: const Text('Confirmar Recebimento', style: TextStyle(fontSize: 11)),
                            ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      Text(
                        'Portador: ${tr.carrierName} • CPF: ${tr.carrierDocument}',
                        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                      ),
                      if (tr.notes != null) ...[
                        const SizedBox(height: 4),
                        Text(tr.notes!, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
                      ],
                    ],
                  ),
                );
              },
            ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // ABA 3: AUDITORIA DE DIVERGÊNCIAS & CUSTÓDIA
  // ---------------------------------------------------------------------------
  Widget _buildAuditingTab(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    final result = state.lastCheckinResult;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Auditoria de Divergências de Materiais & Evidências Visuais',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
          ),
          const SizedBox(height: 4),
          const Text(
            'Materialidade probatória: fotos antes do uso, na instalação e na devolução garantem transparência sem acusações subjetivas.',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 20),
          if (result != null) ...[
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: (result.hasDivergence ? AppTheme.accentError : AppTheme.accentGreen).withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(
                  color: (result.hasDivergence ? AppTheme.accentError : AppTheme.accentGreen).withValues(alpha: 0.4),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        result.hasDivergence ? Icons.report_problem : Icons.verified,
                        color: result.hasDivergence ? AppTheme.accentError : AppTheme.accentGreen,
                        size: 22,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        result.hasDivergence ? 'RESSALVA DE DIVERGÊNCIA REGISTRADA' : 'DEVOLUÇÃO 100% CONFORME',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: result.hasDivergence ? AppTheme.accentError : AppTheme.accentGreen,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text('O.S. Auditada: ${result.workOrderId}', style: const TextStyle(fontSize: 12, color: AppTheme.textPrimary)),
                  Text('Saldo Esperado: ${result.expectedRemaining} | Saldo Apurado na Devolução: ${result.actualRemaining}',
                      style: const TextStyle(fontSize: 12, color: AppTheme.textPrimary)),
                  if (result.hasDivergence)
                    Text('Divergência: ${result.divergenceQuantity} un/m',
                        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppTheme.accentError)),
                  if (result.notes != null) ...[
                    const SizedBox(height: 6),
                    Text(result.notes!, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 20),
          ],
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Instruções de Auditoria aos Supervisores:',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.textPrimary)),
                const SizedBox(height: 8),
                const Text('1. Quando houver divergência, uma O.S. de verificação é aberta automaticamente pelo Responsável Geral.',
                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
                const SizedBox(height: 4),
                const Text('2. O supervisor deve inspecionar as fotos anexadas das 3 fases e, se necessário, medir os vãos de poste.',
                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
                const SizedBox(height: 4),
                const Text('3. Caso o desvio seja justificado pelo terreno, o saldo é homologado sem prejuízo ao colaborador.',
                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // MODAL DE DETALHES, RASTREABILIDADE & KARDEX DO ITEM
  // ---------------------------------------------------------------------------
  void _showItemHistoryModal(
    BuildContext context,
    InventoryItemModel item,
    InventoryState state,
    InventoryNotifier notifier,
  ) {
    showDialog(
      context: context,
      builder: (ctx) {
        return Dialog(
          backgroundColor: AppTheme.darkSurface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 860, maxHeight: 720),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: FutureBuilder<List<StockMovementModel>>(
                future: notifier.fetchItemMovements(item.id),
                builder: (context, snapshot) {
                  final movements = snapshot.data ?? [];
                  final isLoading = snapshot.connectionState == ConnectionState.waiting;

                  int totalEntries = 0;
                  int totalExits = 0;
                  for (final m in movements) {
                    if (m.isEntry) {
                      totalEntries += m.quantity;
                    } else if (m.isExit) {
                      totalExits += m.quantity.abs();
                    }
                  }

                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Header
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: (item.isCriticalStock ? AppTheme.accentError : AppTheme.primaryBlue).withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Icon(
                              item.isCriticalStock ? Icons.warning_amber_rounded : Icons.inventory_2,
                              size: 28,
                              color: item.isCriticalStock ? AppTheme.accentError : AppTheme.primaryBlue,
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Flexible(
                                      child: Text(
                                        item.name,
                                        style: const TextStyle(
                                          fontSize: 18,
                                          fontWeight: FontWeight.bold,
                                          color: AppTheme.textPrimary,
                                        ),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                    const SizedBox(width: 10),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                      decoration: BoxDecoration(
                                        color: AppTheme.darkBg,
                                        borderRadius: BorderRadius.circular(6),
                                        border: Border.all(color: AppTheme.darkBorder),
                                      ),
                                      child: Text(
                                        item.code,
                                        style: const TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w600,
                                          color: AppTheme.primaryBlue,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Text(
                                  'Categoria: ${item.category} • Unidade: ${item.unit} • Saldo em Estoque: ${item.quantityInStock} ${item.unit}',
                                  style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                                ),
                              ],
                            ),
                          ),
                          ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppTheme.primaryBlue,
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                            ),
                            onPressed: () {
                              Navigator.pop(ctx);
                              _showStockEntryModal(context, state, notifier, prefillItem: item);
                            },
                            icon: const Icon(Icons.add_box_outlined, size: 16),
                            label: const Text('Nova Entrada deste Item'),
                          ),
                          const SizedBox(width: 8),
                          IconButton(
                            onPressed: () => Navigator.pop(ctx),
                            icon: const Icon(Icons.close, color: AppTheme.textSecondary),
                            tooltip: 'Fechar',
                          ),
                        ],
                      ),
                      const SizedBox(height: 20),

                      // Cards Resumo de Saldo e Movimentação
                      Wrap(
                        spacing: 12,
                        runSpacing: 12,
                        children: [
                          _buildKardexKpiCard(
                            title: 'Saldo Físico Atual',
                            value: '${item.quantityInStock} ${item.unit}',
                            subtitle: item.isCriticalStock ? 'Abaixo do Mínimo' : 'Nível Normal',
                            color: item.isCriticalStock ? AppTheme.accentError : AppTheme.accentGreen,
                            icon: Icons.account_balance_wallet_outlined,
                          ),
                          _buildKardexKpiCard(
                            title: 'Entradas Acumuladas',
                            value: '+$totalEntries ${item.unit}',
                            subtitle: 'Compras e Lotes',
                            color: AppTheme.accentGreen,
                            icon: Icons.call_received_rounded,
                          ),
                          _buildKardexKpiCard(
                            title: 'Saídas / Consumo O.S.',
                            value: '-$totalExits ${item.unit}',
                            subtitle: 'Baixas para Ordens',
                            color: AppTheme.accentError,
                            icon: Icons.call_made_rounded,
                          ),
                          _buildKardexKpiCard(
                            title: 'Mínimo de Segurança',
                            value: '${item.minQuantity} ${item.unit}',
                            subtitle: 'Ponto de Pedido',
                            color: AppTheme.textSecondary,
                            icon: Icons.shield_outlined,
                          ),
                        ],
                      ),
                      const SizedBox(height: 20),

                      // Título da Seção do Extrato
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Row(
                            children: [
                              Icon(Icons.history, size: 18, color: AppTheme.primaryBlue),
                              SizedBox(width: 8),
                              Text(
                                'Extrato Cronológico & Rastreabilidade (Kardex)',
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.bold,
                                  color: AppTheme.textPrimary,
                                ),
                              ),
                            ],
                          ),
                          Text(
                            '${movements.length} registro(s)',
                            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      const Divider(color: AppTheme.darkBorder, height: 1),
                      const SizedBox(height: 12),

                      // Lista / Timeline de Movimentações
                      Expanded(
                        child: isLoading
                            ? const Center(
                                child: Column(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    CircularProgressIndicator(),
                                    SizedBox(height: 12),
                                    Text('Carregando extrato de movimentações...', style: TextStyle(color: AppTheme.textSecondary)),
                                  ],
                                ),
                              )
                            : movements.isEmpty
                                ? Center(
                                    child: Column(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        Icon(Icons.receipt_long_outlined, size: 48, color: AppTheme.textSecondary.withValues(alpha: 0.5)),
                                        const SizedBox(height: 12),
                                        const Text(
                                          'Nenhuma movimentação registrada no histórico deste item.',
                                          style: TextStyle(fontSize: 14, color: AppTheme.textSecondary),
                                        ),
                                      ],
                                    ),
                                  )
                                : ListView.separated(
                                    itemCount: movements.length,
                                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                                    itemBuilder: (context, idx) {
                                      final m = movements[idx];
                                      return _buildMovementRow(m, item.unit);
                                    },
                                  ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildKardexKpiCard({
    required String title,
    required String value,
    required String subtitle,
    required Color color,
    required IconData icon,
  }) {
    return Container(
      width: 190,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppTheme.darkBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
              ),
              Icon(icon, size: 16, color: color),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            subtitle,
            style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary),
          ),
        ],
      ),
    );
  }

  Widget _buildMovementRow(StockMovementModel m, String unit) {
    Color badgeColor;
    IconData badgeIcon;

    if (m.eventType == 'STOCK_ENTRY') {
      badgeColor = AppTheme.accentGreen;
      badgeIcon = Icons.call_received_rounded;
    } else if (m.eventType == 'MATERIAL_CHECKOUT_OS') {
      badgeColor = AppTheme.accentError;
      badgeIcon = Icons.call_made_rounded;
    } else if (m.eventType.contains('DIVERGENT')) {
      badgeColor = Colors.orange;
      badgeIcon = Icons.warning_amber_rounded;
    } else if (m.eventType.contains('CONFORMANT')) {
      badgeColor = AppTheme.primaryBlue;
      badgeIcon = Icons.assignment_turned_in_outlined;
    } else {
      badgeColor = AppTheme.textSecondary;
      badgeIcon = Icons.swap_horiz_rounded;
    }

    final dateStr = DateFormat('dd/MM/yyyy HH:mm').format(m.createdAt);

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppTheme.darkBorder),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: badgeColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(badgeIcon, size: 18, color: badgeColor),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        m.eventDescription,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: badgeColor,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Text(
                      dateStr,
                      style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Wrap(
                  spacing: 12,
                  runSpacing: 4,
                  children: [
                    if (m.warehouseName != null && m.warehouseName!.isNotEmpty)
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.warehouse_outlined, size: 12, color: AppTheme.textSecondary),
                          const SizedBox(width: 4),
                          Text(m.warehouseName!, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                        ],
                      ),
                    if (m.userName != null && m.userName!.isNotEmpty)
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.person_outline, size: 12, color: AppTheme.textSecondary),
                          const SizedBox(width: 4),
                          Text(m.userName!, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                        ],
                      ),
                    if (m.workOrderId != null && m.workOrderId!.isNotEmpty)
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.build_circle_outlined, size: 12, color: AppTheme.textSecondary),
                          const SizedBox(width: 4),
                          Text('O.S. #${m.workOrderId!.length > 8 ? m.workOrderId!.substring(0, 8) : m.workOrderId}',
                              style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                        ],
                      ),
                  ],
                ),
                if (m.notes != null && m.notes!.trim().isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    m.notes!,
                    style: const TextStyle(fontSize: 11, fontStyle: FontStyle.italic, color: AppTheme.textPrimary),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 14),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '${m.formattedQuantity} $unit',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: m.isEntry ? AppTheme.accentGreen : (m.isExit ? AppTheme.accentError : AppTheme.textPrimary),
                ),
              ),
              const SizedBox(height: 2),
              Text(
                'Saldo após: ${m.balanceAfter} $unit',
                style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // MODAL DE ENTRADA DE MATERIAL / COMPRA
  // ---------------------------------------------------------------------------
  void _showStockEntryModal(
    BuildContext context,
    InventoryState state,
    InventoryNotifier notifier, {
    InventoryItemModel? prefillItem,
  }) {
    final codeCtrl = TextEditingController(text: prefillItem?.code ?? '');
    final nameCtrl = TextEditingController(text: prefillItem?.name ?? '');
    final catCtrl = TextEditingController(text: prefillItem?.category ?? '');
    final qtyCtrl = TextEditingController(text: '');
    final unitCtrl = TextEditingController(text: prefillItem?.unit ?? 'UN');
    final notesCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Registrar Entrada de Material / Lote', style: TextStyle(color: AppTheme.textPrimary)),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: codeCtrl,
                style: const TextStyle(color: AppTheme.textPrimary),
                decoration: const InputDecoration(labelText: 'Código do Item', labelStyle: TextStyle(color: AppTheme.textSecondary)),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: nameCtrl,
                style: const TextStyle(color: AppTheme.textPrimary),
                decoration: const InputDecoration(labelText: 'Nome do Item', labelStyle: TextStyle(color: AppTheme.textSecondary)),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: qtyCtrl,
                      keyboardType: TextInputType.number,
                      style: const TextStyle(color: AppTheme.textPrimary),
                      decoration: const InputDecoration(labelText: 'Quantidade', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextField(
                      controller: unitCtrl,
                      style: const TextStyle(color: AppTheme.textPrimary),
                      decoration: const InputDecoration(labelText: 'Unidade (UN, METROS, BOB)', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              TextField(
                controller: notesCtrl,
                style: const TextStyle(color: AppTheme.textPrimary),
                decoration: const InputDecoration(labelText: 'Nota Fiscal / Fornecedor', labelStyle: TextStyle(color: AppTheme.textSecondary)),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar', style: TextStyle(color: AppTheme.textSecondary)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryBlue, foregroundColor: Colors.white),
            onPressed: () async {
              final qty = int.tryParse(qtyCtrl.text.trim()) ?? 1;
              final ok = await notifier.registerStockEntry(
                StockEntryPayload(
                  warehouseId: state.selectedWarehouse?.id,
                  itemCode: codeCtrl.text.trim(),
                  itemName: nameCtrl.text.trim(),
                  category: catCtrl.text.trim(),
                  quantity: qty,
                  unit: unitCtrl.text.trim(),
                  notes: notesCtrl.text.trim(),
                ),
              );
              if (ctx.mounted && ok) Navigator.pop(ctx);
            },
            child: const Text('Salvar Entrada'),
          ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // MODAL DE NOVA TRANSFERÊNCIA INTER-BASES COM SELEÇÃO DE COLABORADOR
  // ---------------------------------------------------------------------------
  void _showCreateTransferModal(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    final availableWarehouses = state.warehouses.where((w) => w.id != state.selectedWarehouse?.id).toList();
    String? destWarehouseId = availableWarehouses.isNotEmpty ? availableWarehouses.first.id : null;

    CollaboratorModel? selectedCarrier = state.collaborators.isNotEmpty ? state.collaborators.first : null;
    final notesCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Row(
            children: const [
              Icon(Icons.local_shipping_outlined, color: AppTheme.primaryBlue),
              SizedBox(width: 8),
              Expanded(
                child: Text('Nova Transferência Inter-Bases', style: TextStyle(color: AppTheme.textPrimary, fontSize: 18)),
              ),
            ],
          ),
          content: SingleChildScrollView(
            child: SizedBox(
              width: 500,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  DropdownButtonFormField<String>(
                    dropdownColor: AppTheme.darkSurface,
                    initialValue: destWarehouseId,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    decoration: const InputDecoration(
                      labelText: 'Depósito de Destino *',
                      labelStyle: TextStyle(color: AppTheme.textSecondary),
                      prefixIcon: Icon(Icons.warehouse_outlined, size: 20),
                    ),
                    items: availableWarehouses.map((w) {
                      return DropdownMenuItem(value: w.id, child: Text('${w.name} (${w.city}/${w.state})'));
                    }).toList(),
                    onChanged: (val) => setLocalState(() => destWarehouseId = val),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Portador Responsável *',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, fontWeight: FontWeight.w600),
                      ),
                      TextButton.icon(
                        icon: const Icon(Icons.person_add_alt_1, size: 16, color: AppTheme.primaryBlue),
                        label: const Text('Cadastrar Terceiro', style: TextStyle(fontSize: 12, color: AppTheme.primaryBlue)),
                        style: TextButton.styleFrom(visualDensity: VisualDensity.compact),
                        onPressed: () {
                          _showCreateCollaboratorDialog(context, notifier, (CollaboratorModel newCarrier) {
                            setLocalState(() {
                              selectedCarrier = newCarrier;
                            });
                          });
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  if (state.collaborators.isEmpty)
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.accentWarning.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppTheme.accentWarning.withValues(alpha: 0.4)),
                      ),
                      child: Row(
                        children: const [
                          Icon(Icons.warning_amber_rounded, color: AppTheme.accentWarning, size: 20),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Nenhum colaborador encontrado. Clique em "Cadastrar Terceiro" para cadastrar o motorista responsável.',
                              style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                            ),
                          ),
                        ],
                      ),
                    )
                  else
                    DropdownButtonFormField<String>(
                      dropdownColor: AppTheme.darkSurface,
                      initialValue: selectedCarrier?.id,
                      style: const TextStyle(color: AppTheme.textPrimary),
                      isExpanded: true,
                      decoration: const InputDecoration(
                        labelText: 'Selecionar Colaborador Cadastrado',
                        labelStyle: TextStyle(color: AppTheme.textSecondary),
                        prefixIcon: Icon(Icons.badge_outlined, size: 20),
                      ),
                      items: state.collaborators.map((c) {
                        return DropdownMenuItem(
                          value: c.id,
                          child: Text(
                            c.displayNameWithCpf,
                            overflow: TextOverflow.ellipsis,
                          ),
                        );
                      }).toList(),
                      onChanged: (val) {
                        final found = state.collaborators.where((c) => c.id == val).toList();
                        if (found.isNotEmpty) {
                          setLocalState(() => selectedCarrier = found.first);
                        }
                      },
                    ),
                  if (selectedCarrier != null) ...[
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.darkBg,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.3)),
                      ),
                      child: Row(
                        children: [
                          CircleAvatar(
                            radius: 18,
                            backgroundColor: AppTheme.primaryBlue.withValues(alpha: 0.2),
                            child: const Icon(Icons.person, color: AppTheme.primaryBlue, size: 18),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  selectedCarrier!.name,
                                  style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.textPrimary, fontSize: 13),
                                  overflow: TextOverflow.ellipsis,
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  'CPF: ${selectedCarrier!.cpf ?? "Não informado"} • E-mail: ${selectedCarrier!.email}',
                                  style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppTheme.accentGreen.withValues(alpha: 0.2),
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: const Text(
                              'Vinculado',
                              style: TextStyle(color: AppTheme.accentGreen, fontSize: 10, fontWeight: FontWeight.bold),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 16),
                  TextField(
                    controller: notesCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    maxLines: 2,
                    decoration: const InputDecoration(
                      labelText: 'Observações / Lacre / Rastreamento',
                      hintText: 'Ex: Lacre nº 88219 - Transporte de 4 bobinas',
                      labelStyle: TextStyle(color: AppTheme.textSecondary),
                      prefixIcon: Icon(Icons.notes_outlined, size: 20),
                    ),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar', style: TextStyle(color: AppTheme.textSecondary)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryBlue, foregroundColor: Colors.white),
              onPressed: (destWarehouseId == null || selectedCarrier == null)
                  ? null
                  : () async {
                      final isTerceiro = selectedCarrier!.role.toUpperCase() == 'TERCEIRO' ||
                          selectedCarrier!.role.toUpperCase() == 'CLIENT' ||
                          selectedCarrier!.email.contains('terceiro');
                      final ok = await notifier.createTransfer(
                        destinationWarehouseId: destWarehouseId!,
                        carrierUserId: selectedCarrier!.id,
                        carrierName: selectedCarrier!.name,
                        carrierDocument: selectedCarrier!.cpf ?? '000.000.000-00',
                        carrierType: isTerceiro ? 'TERCEIRO' : 'COLABORADOR',
                        notes: notesCtrl.text.trim().isEmpty ? null : notesCtrl.text.trim(),
                      );
                      if (ctx.mounted && ok) Navigator.pop(ctx);
                    },
              child: const Text('Emitir Guia'),
            ),
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // DIÁLOGO DE CADASTRO DE TERCEIRO / MOTORISTA DE TRANSPORTE
  // ---------------------------------------------------------------------------
  void _showCreateCollaboratorDialog(
    BuildContext context,
    InventoryNotifier notifier,
    void Function(CollaboratorModel newCollab) onCreated,
  ) {
    final nameCtrl = TextEditingController();
    final cpfCtrl = TextEditingController();
    final emailCtrl = TextEditingController();
    final passwordCtrl = TextEditingController(text: 'Mudar@123');
    bool isSaving = false;
    String? localError;

    showDialog(
      context: context,
      builder: (dlgCtx) => StatefulBuilder(
        builder: (dlgCtx, setDlgState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Row(
            children: const [
              Icon(Icons.person_add_alt_1, color: AppTheme.primaryBlue),
              SizedBox(width: 8),
              Expanded(
                child: Text('Cadastrar Terceiro / Motorista', style: TextStyle(color: AppTheme.textPrimary, fontSize: 16)),
              ),
            ],
          ),
          content: SingleChildScrollView(
            child: SizedBox(
              width: 420,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'Cadastre a pessoa responsável pelo transporte para vincular à cadeia de custódia do ERP.',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                  ),
                  const SizedBox(height: 14),
                  if (localError != null) ...[
                    Container(
                      padding: const EdgeInsets.all(8),
                      margin: const EdgeInsets.only(bottom: 12),
                      decoration: BoxDecoration(
                        color: AppTheme.accentError.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: AppTheme.accentError),
                      ),
                      child: Text(localError!, style: const TextStyle(color: AppTheme.accentError, fontSize: 12)),
                    ),
                  ],
                  TextField(
                    controller: nameCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    decoration: const InputDecoration(
                      labelText: 'Nome Completo *',
                      hintText: 'Ex: Carlos Alberto da Silva',
                      prefixIcon: Icon(Icons.badge_outlined, size: 20),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: cpfCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'CPF do Portador *',
                      hintText: '000.000.000-00',
                      prefixIcon: Icon(Icons.pin_outlined, size: 20),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: emailCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(
                      labelText: 'E-mail de Contato *',
                      hintText: 'motorista@exemplo.com',
                      prefixIcon: Icon(Icons.email_outlined, size: 20),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: passwordCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    obscureText: true,
                    decoration: const InputDecoration(
                      labelText: 'Senha de Acesso (Mín. 6 dígitos) *',
                      prefixIcon: Icon(Icons.lock_outline, size: 20),
                    ),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: isSaving ? null : () => Navigator.pop(dlgCtx),
              child: const Text('Cancelar', style: TextStyle(color: AppTheme.textSecondary)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
              ),
              onPressed: isSaving
                  ? null
                  : () async {
                      final name = nameCtrl.text.trim();
                      final cpf = cpfCtrl.text.trim();
                      final email = emailCtrl.text.trim();
                      final pwd = passwordCtrl.text.trim();

                      if (name.isEmpty) {
                        setDlgState(() => localError = 'Informe o nome completo do portador.');
                        return;
                      }
                      if (cpf.isEmpty) {
                        setDlgState(() => localError = 'O CPF é obrigatório para a cadeia de custódia.');
                        return;
                      }
                      if (email.isEmpty || !email.contains('@')) {
                        setDlgState(() => localError = 'Informe um e-mail válido para o cadastro.');
                        return;
                      }
                      if (pwd.length < 6) {
                        setDlgState(() => localError = 'A senha deve conter no mínimo 6 caracteres.');
                        return;
                      }

                      setDlgState(() {
                        isSaving = true;
                        localError = null;
                      });

                      final created = await notifier.registerCollaborator(
                        name: name,
                        email: email,
                        password: pwd,
                        cpf: cpf,
                        role: 'USER',
                      );

                      if (dlgCtx.mounted) {
                        setDlgState(() => isSaving = false);
                        if (created != null) {
                          Navigator.pop(dlgCtx);
                          onCreated(created);
                        }
                      }
                    },
              child: isSaving
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Salvar e Selecionar'),
            ),
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // MODAL DE RETIRADA VINCULADA À O.S. (REGRA DE OURO)
  // ---------------------------------------------------------------------------
  void _showCheckoutModal(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    String? selectedWoId = state.demands.isNotEmpty ? state.demands.first.workOrderId : null;
    String? selectedTechId = state.collaborators.isNotEmpty ? state.collaborators.first.id : null;
    final qtyCtrl = TextEditingController();
    final beforePhotoCtrl = TextEditingController();
    final notesCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Retirada de Material para O.S.', style: TextStyle(color: AppTheme.textPrimary)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  dropdownColor: AppTheme.darkSurface,
                  initialValue: selectedWoId,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(labelText: 'Ordem de Serviço Obrigatória *', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                  items: state.demands.map((d) {
                    return DropdownMenuItem(value: d.workOrderId, child: Text('${d.customerName} (${d.workOrderId.substring(0, 8)})'));
                  }).toList(),
                  onChanged: (val) => setLocalState(() => selectedWoId = val),
                ),
                const SizedBox(height: 10),
                if (state.collaborators.isNotEmpty)
                  DropdownButtonFormField<String>(
                    dropdownColor: AppTheme.darkSurface,
                    initialValue: selectedTechId,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Técnico Responsável *',
                      labelStyle: TextStyle(color: AppTheme.textSecondary),
                      prefixIcon: Icon(Icons.engineering_outlined, size: 20),
                    ),
                    items: state.collaborators.map((c) {
                      return DropdownMenuItem(value: c.id, child: Text(c.displayNameWithCpf, overflow: TextOverflow.ellipsis));
                    }).toList(),
                    onChanged: (val) => setLocalState(() => selectedTechId = val),
                  ),
                const SizedBox(height: 10),
                TextField(
                  controller: qtyCtrl,
                  keyboardType: TextInputType.number,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(
                    labelText: 'Metragem ou Quantidade Retirada *',
                    hintText: 'Ex: 2000',
                    labelStyle: TextStyle(color: AppTheme.textSecondary),
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: beforePhotoCtrl,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(
                    labelText: 'URL da Foto Inicial (Hodômetro / Marcação)',
                    hintText: 'https://exemplo.com/odometro-inicial.jpg',
                    labelStyle: TextStyle(color: AppTheme.textSecondary),
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(
                    labelText: 'Observações da Saída',
                    hintText: 'Observações do estado do material',
                    labelStyle: TextStyle(color: AppTheme.textSecondary),
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar', style: TextStyle(color: AppTheme.textSecondary)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryBlue, foregroundColor: Colors.white),
              onPressed: (selectedWoId == null || selectedTechId == null)
                  ? null
                  : () async {
                      final qty = int.tryParse(qtyCtrl.text.trim()) ?? 0;
                      if (qty <= 0) {
                        return;
                      }
                      final ok = await notifier.checkoutMaterial(
                        MaterialCheckoutPayload(
                          workOrderId: selectedWoId!,
                          technicianUserId: selectedTechId!,
                          warehouseId: state.selectedWarehouse?.id,
                          quantityOrMeters: qty,
                          beforePhotoUrl: beforePhotoCtrl.text.trim().isEmpty ? null : beforePhotoCtrl.text.trim(),
                          notes: notesCtrl.text.trim().isEmpty ? null : notesCtrl.text.trim(),
                        ),
                      );
                      if (ctx.mounted && ok) Navigator.pop(ctx);
                    },
              child: const Text('Autorizar Retirada'),
            ),
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // MODAL DE DEVOLUÇÃO & CONFERÊNCIA DE METRAGEM COM 3 FOTOS
  // ---------------------------------------------------------------------------
  void _showCheckinModal(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    String? selectedWoId = state.demands.isNotEmpty ? state.demands.first.workOrderId : null;
    final initialCtrl = TextEditingController();
    final consumedCtrl = TextEditingController();
    final remainingCtrl = TextEditingController();
    final beforePhotoCtrl = TextEditingController();
    final installedPhotoCtrl = TextEditingController();
    final returnPhotoCtrl = TextEditingController();
    final notesCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) {
          final initVal = int.tryParse(initialCtrl.text) ?? 0;
          final consVal = int.tryParse(consumedCtrl.text) ?? 0;
          final remVal = int.tryParse(remainingCtrl.text) ?? 0;
          final expected = initVal - consVal;
          final hasDiv = initVal > 0 && remVal != expected;

          return AlertDialog(
            backgroundColor: AppTheme.darkSurface,
            title: const Text('Devolução de Sobras & Auditoria Visual', style: TextStyle(color: AppTheme.textPrimary)),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  DropdownButtonFormField<String>(
                    dropdownColor: AppTheme.darkSurface,
                    initialValue: selectedWoId,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    decoration: const InputDecoration(labelText: 'Ordem de Serviço Executada *', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                    items: state.demands.map((d) {
                      return DropdownMenuItem(value: d.workOrderId, child: Text('${d.customerName} (${d.workOrderId.substring(0, 8)})'));
                    }).toList(),
                    onChanged: (val) => setLocalState(() => selectedWoId = val),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: initialCtrl,
                          keyboardType: TextInputType.number,
                          style: const TextStyle(color: AppTheme.textPrimary),
                          decoration: const InputDecoration(
                            labelText: 'Metragem Inicial *',
                            hintText: 'Ex: 2000',
                            labelStyle: TextStyle(color: AppTheme.textSecondary),
                          ),
                          onChanged: (_) => setLocalState(() {}),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: consumedCtrl,
                          keyboardType: TextInputType.number,
                          style: const TextStyle(color: AppTheme.textPrimary),
                          decoration: const InputDecoration(
                            labelText: 'Consumo na O.S. *',
                            hintText: 'Ex: 500',
                            labelStyle: TextStyle(color: AppTheme.textSecondary),
                          ),
                          onChanged: (_) => setLocalState(() {}),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: remainingCtrl,
                          keyboardType: TextInputType.number,
                          style: const TextStyle(color: AppTheme.textPrimary),
                          decoration: const InputDecoration(
                            labelText: 'Saldo Devolvido *',
                            hintText: 'Ex: 1500',
                            labelStyle: TextStyle(color: AppTheme.textSecondary),
                          ),
                          onChanged: (_) => setLocalState(() {}),
                        ),
                      ),
                    ],
                  ),
                  if (initVal > 0) ...[
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: (hasDiv ? AppTheme.accentError : AppTheme.accentGreen).withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        hasDiv
                            ? 'DIVERGÊNCIA DETECTADA: Esperava $expected m, mas foi medido $remVal m (${remVal - expected} m).'
                            : 'CONFORME: Metragem esperada bate exatamente com o saldo devolvido ($expected m).',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: hasDiv ? AppTheme.accentError : AppTheme.accentGreen,
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 12),
                  const Text('Evidências Visuais da O.S. (3 Fases):', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppTheme.textPrimary)),
                  const SizedBox(height: 6),
                  TextField(
                    controller: beforePhotoCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary, fontSize: 12),
                    decoration: const InputDecoration(labelText: '1. Foto Antes do Uso', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                  ),
                  const SizedBox(height: 6),
                  TextField(
                    controller: installedPhotoCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary, fontSize: 12),
                    decoration: const InputDecoration(labelText: '2. Foto Instalado no Cliente', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                  ),
                  const SizedBox(height: 6),
                  TextField(
                    controller: returnPhotoCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary, fontSize: 12),
                    decoration: const InputDecoration(labelText: '3. Foto Metragem Restante', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: notesCtrl,
                    style: const TextStyle(color: AppTheme.textPrimary),
                    decoration: const InputDecoration(labelText: 'Observações da Devolução', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text('Cancelar', style: TextStyle(color: AppTheme.textSecondary)),
              ),
              ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: hasDiv ? AppTheme.accentWarning : AppTheme.accentGreen,
                  foregroundColor: Colors.white,
                ),
                onPressed: selectedWoId == null
                    ? null
                    : () async {
                        final res = await notifier.checkinMaterial(
                          MaterialCheckinPayload(
                            workOrderId: selectedWoId!,
                            technicianUserId: '01a0674f-01cc-7b04-b81b-87a0b9335d7d',
                            warehouseId: state.selectedWarehouse?.id,
                            initialMetersOrQty: initVal,
                            consumedMetersOrQty: consVal,
                            actualRemainingMetersOrQty: remVal,
                            beforePhotoUrl: beforePhotoCtrl.text.trim(),
                            installedPhotoUrl: installedPhotoCtrl.text.trim(),
                            returnPhotoUrl: returnPhotoCtrl.text.trim(),
                            notes: notesCtrl.text.trim(),
                          ),
                        );
                        if (ctx.mounted && res != null) {
                          Navigator.pop(ctx);
                          notifier.setTab(3); // Vai para a aba de Auditoria de Divergências
                        }
                      },
                child: Text(hasDiv ? 'Receber com Ressalva' : 'Confirmar Devolução'),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildBadge(String text, IconData icon, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withValues(alpha: 0.25)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 5),
          Text(text, style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }

  Widget _buildEmptyState(String message, IconData icon) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 40),
        child: Column(
          children: [
            Icon(icon, size: 40, color: AppTheme.textSecondary.withValues(alpha: 0.4)),
            const SizedBox(height: 12),
            Text(message, style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary)),
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // ABA 4: LOGÍSTICA REVERSA & OFFBOARDING DE COMODATO (ESTEIRA DE 6 ETAPAS)
  // ---------------------------------------------------------------------------
  Widget _buildReverseLogisticsTab(
    BuildContext context,
    InventoryState state,
    InventoryNotifier notifier,
    String? currentUserRole,
  ) {
    final removalDemands = state.reverseLogisticsDemands;

    if (_selectedRemovalDemandId == null && removalDemands.isNotEmpty) {
      _selectedRemovalDemandId = removalDemands.first.id;
    }

    final selectedDemand = removalDemands.where((d) => d.id == _selectedRemovalDemandId).firstOrNull ??
        (removalDemands.isNotEmpty ? removalDemands.first : null);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header da Aba
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'Logística Reversa & Offboarding de Comodato',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Esteira de desinstalação, entrada em quarentena no almoxarifado, reteste óptico de bancada e reclassificação de ativos.',
                      style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              OutlinedButton.icon(
                onPressed: () => notifier.loadAll(),
                icon: const Icon(Icons.refresh, size: 16),
                label: const Text('Atualizar Coletas'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppTheme.primaryBlue,
                  side: const BorderSide(color: AppTheme.primaryBlue),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),

          if (removalDemands.isEmpty)
            _buildEmptyState(
              'Nenhuma ordem de recolhimento de comodato pendente no momento.',
              Icons.assignment_turned_in_outlined,
            )
          else ...[
            // Painel da Esteira Operacional para a Coleta Selecionada
            if (selectedDemand != null) ...[
              Container(
                decoration: BoxDecoration(
                  color: AppTheme.darkSurface,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.darkBorder),
                ),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Row(
                            children: [
                              const Icon(Icons.inventory_2_rounded, color: Colors.purpleAccent, size: 22),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Auditoria de Custódia: O.S. ${selectedDemand.workOrderId.length > 8 ? selectedDemand.workOrderId.substring(0, 8) : selectedDemand.workOrderId} • ${selectedDemand.customerName}',
                                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      'Equipamentos: ${selectedDemand.onuModelRequired} + Roteador Wi-Fi • Endereço: ${selectedDemand.customerAddress}',
                                      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.purpleAccent.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: Colors.purpleAccent.withValues(alpha: 0.4)),
                          ),
                          child: Text(
                            selectedDemand.status.label,
                            style: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: Colors.purpleAccent,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),

                    // Componente ProcessLifecycleStepper Reutilizável com POPs
                    ProcessLifecycleStepper(
                      processTitle: 'Esteira de Logística Reversa & Offboarding de Comodato',
                      steps: _buildReverseLogisticsSteps(selectedDemand),
                      currentUserRole: currentUserRole,
                    ),
                    const SizedBox(height: 14),

                    // Ações de Governança e Transição de Custódia
                    Wrap(
                      spacing: 10,
                      runSpacing: 8,
                      alignment: WrapAlignment.end,
                      children: [
                        if (currentUserRole == 'ADMIN' || currentUserRole == 'STOCKIST') ...[
                          ElevatedButton.icon(
                            onPressed: state.isSubmitting
                                ? null
                                : () => notifier.processReverseLogisticsCheckin(selectedDemand.workOrderId),
                            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryBlue),
                            icon: const Icon(Icons.move_to_inbox_rounded, size: 16, color: Colors.white),
                            label: const Text('Receber na Quarentena (Almoxarifado)', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                          ),
                        ],
                        if (currentUserRole == 'ADMIN' || currentUserRole == 'SUPPORT_N2' || currentUserRole == 'STOCKIST') ...[
                          ElevatedButton.icon(
                            onPressed: state.isSubmitting
                                ? null
                                : () {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(
                                        backgroundColor: AppTheme.accentGreen,
                                        content: Text('Reteste óptico concluído com sucesso: Potência RX -19.4 dBm. Firmware restaurado aos padrões de fábrica!'),
                                        duration: Duration(seconds: 3),
                                      ),
                                    );
                                  },
                            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentGreen),
                            icon: const Icon(Icons.check_circle_outline, size: 16, color: Color(0xFF0A0F1D)),
                            label: const Text('Aprovar Teste de Bancada & Reteste Óptico', style: TextStyle(color: Color(0xFF0A0F1D), fontWeight: FontWeight.bold)),
                          ),
                        ],
                        if (currentUserRole == 'ADMIN' || currentUserRole == 'STOCKIST') ...[
                          OutlinedButton.icon(
                            onPressed: state.isSubmitting
                                ? null
                                : () {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(
                                        backgroundColor: AppTheme.primaryBlue,
                                        content: Text('Ativo reintegrado com status DISPONÍVEL no estoque central para novas instalações!'),
                                        duration: Duration(seconds: 3),
                                      ),
                                    );
                                  },
                            icon: const Icon(Icons.assignment_return_rounded, size: 16),
                            label: const Text('Reintegrar ao Saldo Ativo'),
                          ),
                          OutlinedButton.icon(
                            onPressed: state.isSubmitting
                                ? null
                                : () {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(
                                        backgroundColor: AppTheme.accentError,
                                        content: Text('Laudo de avaria emitido. Equipamento baixado para descarte ecológico/sucata.'),
                                        duration: Duration(seconds: 3),
                                      ),
                                    );
                                  },
                            style: OutlinedButton.styleFrom(foregroundColor: AppTheme.accentError),
                            icon: const Icon(Icons.delete_outline, size: 16, color: AppTheme.accentError),
                            label: const Text('Baixa como Sucata / Descarte', style: TextStyle(color: AppTheme.accentError)),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
            ],

            // Lista de Ordens de Retirada
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Ordens de Serviço de Recolhimento de Comodato',
                              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              'Total: ${removalDemands.length} ordens de comodato registradas',
                              style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),

                    ListView.separated(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: removalDemands.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final d = removalDemands[index];
                        final isSelected = d.id == _selectedRemovalDemandId;

                        return InkWell(
                          onTap: () {
                            setState(() {
                              _selectedRemovalDemandId = d.id;
                            });
                          },
                          borderRadius: BorderRadius.circular(8),
                          child: Container(
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: isSelected ? AppTheme.primaryBlue.withValues(alpha: 0.12) : AppTheme.darkSurface,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: isSelected ? AppTheme.primaryBlue : AppTheme.darkBorder,
                                width: isSelected ? 1.5 : 1.0,
                              ),
                            ),
                            child: Row(
                              children: [
                                Icon(
                                  Icons.assignment_return_outlined,
                                  color: isSelected ? AppTheme.primaryBlue : AppTheme.textSecondary,
                                  size: 24,
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        d.customerName,
                                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppTheme.textPrimary),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                        'O.S.: ${d.workOrderId.length > 8 ? d.workOrderId.substring(0, 8) : d.workOrderId} • Endereço: ${d.customerAddress}',
                                        style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        'Equipamentos: ${d.onuModelRequired} + Roteador Wi-Fi (Técnico: ${d.allocatedTechnicianName ?? "Aguardando Despacho"})',
                                        style: const TextStyle(fontSize: 11, color: AppTheme.primaryBlue),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: AppTheme.darkBg,
                                    borderRadius: BorderRadius.circular(6),
                                    border: Border.all(color: AppTheme.darkBorder),
                                  ),
                                  child: Text(
                                    d.status.label,
                                    style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppTheme.textSecondary),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  List<ProcessLifecycleStep> _buildReverseLogisticsSteps(InstallationDemandModel demand) {
    final status = demand.status;

    return [
      const ProcessLifecycleStep(
        id: 'step_rev_churn',
        title: '1. Rescisão & Solicitação',
        subtitle: 'Contrato Cancelado / D+60',
        responsibleRoleName: 'Financeiro / Atendimento',
        allowedRoles: ['FINANCIAL', 'SALES', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.cancel_presentation_rounded,
        popGuideTitle: 'POP-REV-01: Formalização do Cancelamento e Abertura de Recolhimento',
        popGuideContent:
            '1. Verificação Contratual: Registro de encerramento contratual por pedido amigável ou rescisão após 60 dias de inadimplência.\n\n'
            '2. Inventário em Comodato: Identificação automática no ERP dos números de série da ONT/ONU e do Roteador Wi-Fi cedidos em comodato gratuito.\n\n'
            '3. Emissão da O.S.: O sistema emite automaticamente a Ordem de Retirada/Recolhimento para a Torre de Controle.',
      ),
      ProcessLifecycleStep(
        id: 'step_rev_dispatch',
        title: '2. Despacho & Rota de Coleta',
        subtitle: status == MaterialDemandStatus.pendingAllocation ? 'Aguardando Despacho' : 'Rota Atribuída',
        responsibleRoleName: 'Torre de Controle',
        allowedRoles: const ['SUPPORT_ANALYST', 'SUPPORT_N2', 'ADMIN'],
        isCompleted: status != MaterialDemandStatus.pendingAllocation,
        isActive: status == MaterialDemandStatus.pendingAllocation,
        icon: Icons.route_rounded,
        popGuideTitle: 'POP-REV-02: Roteirização e Alocação de Técnico de Coleta',
        popGuideContent:
            '1. Agrupamento Geográfico: A Torre de Controle agrupa as coletas em rotas conjuntas com atendimentos de reparo e instalação.\n\n'
            '2. Alocação: Atribuição ao técnico de campo com veículo apropriado para transporte seguro de equipamentos frágeis.\n\n'
            '3. SLA: Coleta executada em até 5 dias úteis após a rescisão.',
      ),
      ProcessLifecycleStep(
        id: 'step_rev_field_collection',
        title: '3. Coleta Domiciliar & Termo',
        subtitle: status == MaterialDemandStatus.allocatedVehicle
            ? 'Em Coleta com Técnico'
            : (status == MaterialDemandStatus.pendingAllocation ? 'Aguardando Coleta' : 'Equipamento Coletado'),
        responsibleRoleName: 'Técnico de Campo',
        allowedRoles: const ['TECHNICIAN', 'ADMIN'],
        isCompleted: status == MaterialDemandStatus.allocatedCentral || status == MaterialDemandStatus.consumedInField,
        isActive: status == MaterialDemandStatus.allocatedVehicle,
        icon: Icons.home_repair_service_rounded,
        popGuideTitle: 'POP-REV-03: Procedimento de Coleta Domiciliar e Assinatura de Termo',
        popGuideContent:
            '1. Conferência Visual: Checagem da integridade da carcaça, antenas, portas RJ45 e fonte de alimentação.\n\n'
            '2. Coleta dos Itens: ONU GPON, Roteador Wi-Fi, Fonte 12V e cabos de rede.\n\n'
            '3. Termo Digital: Assinatura do termo de devolução pelo assinante no app móvel com registro fotográfico do número de série (S/N) e MAC.',
      ),
      ProcessLifecycleStep(
        id: 'step_rev_quarantine',
        title: '4. Entrada em Quarentena',
        subtitle: status == MaterialDemandStatus.allocatedCentral
            ? 'Em Quarentena no Almoxarifado'
            : (status == MaterialDemandStatus.consumedInField ? 'Conferido no Almoxarifado' : 'Aguardando Chegada'),
        responsibleRoleName: 'Almoxarife / Estoquista',
        allowedRoles: const ['STOCKIST', 'ADMIN'],
        isCompleted: status == MaterialDemandStatus.consumedInField,
        isActive: status == MaterialDemandStatus.allocatedCentral,
        icon: Icons.inventory_2_rounded,
        popGuideTitle: 'POP-REV-04: Entrada em Quarentena e Conferência de MAC / Serial',
        popGuideContent:
            '1. Recebimento da Equipe de Campo: Conferência física dos itens entregues pelo técnico com a O.S.\n\n'
            '2. Bipagem de Código de Barras / MAC: Leitura ótica do MAC Address no sistema para dar baixa da custódia do veículo e entrada no lote de Quarentena do Almoxarifado Central.\n\n'
            '3. Encaminhamento para Laboratório: Alocação física na bancada de higienização e diagnóstico.',
      ),
      ProcessLifecycleStep(
        id: 'step_rev_lab_test',
        title: '5. Higienização & Reteste Óptico',
        subtitle: status == MaterialDemandStatus.consumedInField ? 'Aprovado em Bancada' : 'Aguardando Laboratório',
        responsibleRoleName: 'Laboratório / Suporte N2',
        allowedRoles: const ['SUPPORT_N2', 'STOCKIST', 'ADMIN'],
        isCompleted: status == MaterialDemandStatus.consumedInField,
        isActive: false,
        icon: Icons.cleaning_services_rounded,
        popGuideTitle: 'POP-REV-05: Higienização, Reset de Fábrica e Medição de Potência Óptica',
        popGuideContent:
            '1. Higienização: Limpeza ultrassônica externa da carcaça e álcool isopropílico nos conectores ópticos.\n\n'
            '2. Hard Reset: Restauração para os padrões de fábrica (Factory Default Firmware).\n\n'
            '3. Teste Óptico na OLT de Bancada: Verificação de sensibilidade do receptor RX/TX (limite aceitável: -8 dBm a -27 dBm) e porta Gigabit LAN.\n\n'
            '4. Reembalagem: Acondicionamento em caixa padrão ispERP com identificação de lote testado.',
      ),
      ProcessLifecycleStep(
        id: 'step_rev_restock',
        title: '6. Reintegração / Sucata',
        subtitle: status == MaterialDemandStatus.consumedInField ? 'Saldo Ativo Reintegrado' : 'Aguardando Homologação',
        responsibleRoleName: 'Almoxarife / Gerência',
        allowedRoles: const ['STOCKIST', 'ADMIN'],
        isCompleted: false,
        isActive: status == MaterialDemandStatus.consumedInField,
        icon: Icons.verified_rounded,
        popGuideTitle: 'POP-REV-06: Homologação Final, Retorno ao Estoque ou Baixa Patrimonial',
        popGuideContent:
            '1. Equipamento Aprovado: Reintegração imediata com status DISPONÍVEL no Almoxarifado Central para ser reutilizado em novas instalações.\n\n'
            '2. Equipamento Danificado / Inviável: Baixa definitiva com emissão de laudo de sucata/descarte ambiental conforme normas WEEE/Anatel.\n\n'
            '3. Cobrança de Equipamento Avariado: Caso constatado mau uso pelo cliente, lançamento de débito indenizatório no Contas a Receber.',
      ),
    ];
  }
}
