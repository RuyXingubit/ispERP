import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../../dispatch/data/dispatch_models.dart';
import '../data/inventory_models.dart';
import '../data/inventory_notifier.dart';

class InventoryScreen extends ConsumerWidget {
  const InventoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(inventoryProvider);
    final notifier = ref.read(inventoryProvider.notifier);

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
      child: Row(
        children: [
          _buildTabButton(0, 'Triagem & Saídas por O.S.', Icons.assignment_outlined, state.selectedTab == 0, notifier),
          _buildTabButton(1, 'Saldos & Entradas', Icons.inventory_outlined, state.selectedTab == 1, notifier),
          _buildTabButton(2, 'Transferências & Trânsito', Icons.sync_alt, state.selectedTab == 2, notifier),
          _buildTabButton(3, 'Auditoria de Divergências', Icons.fact_check_outlined, state.selectedTab == 3, notifier),
        ],
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
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Fila de Triagem de Materiais para O.S. de Instalação',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Confirme a separação no Depósito Central para liberar o agendamento pelo Supervisor Técnico.',
                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                ],
              ),
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
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Saldos Físicos no Almoxarifado',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Controle dinâmico de insumos a granel, bobinas de fibra e equipamentos serializados.',
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
                return Container(
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
                                Text(
                                  item.name,
                                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
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
  // MODAL DE ENTRADA DE MATERIAL / COMPRA
  // ---------------------------------------------------------------------------
  void _showStockEntryModal(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    final codeCtrl = TextEditingController(text: 'BOB-FIBRA-12FO-AS80');
    final nameCtrl = TextEditingController(text: 'Bobina de Fibra 12FO AS80 2000m');
    final catCtrl = TextEditingController(text: 'CABO_FIBRA');
    final qtyCtrl = TextEditingController(text: '10');
    final unitCtrl = TextEditingController(text: 'BOB');
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
  // MODAL DE NOVA TRANSFERÊNCIA INTER-BASES
  // ---------------------------------------------------------------------------
  void _showCreateTransferModal(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    String? destWarehouseId = state.warehouses.length > 1 ? state.warehouses[1].id : null;
    final carrierNameCtrl = TextEditingController(text: 'João Técnico');
    final carrierDocCtrl = TextEditingController(text: '123.456.789-00');
    final notesCtrl = TextEditingController(text: 'Transporte de bobinas para ponto de apoio');

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Nova Transferência Inter-Bases', style: TextStyle(color: AppTheme.textPrimary)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  dropdownColor: AppTheme.darkSurface,
                  initialValue: destWarehouseId,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(labelText: 'Depósito de Destino', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                  items: state.warehouses.where((w) => w.id != state.selectedWarehouse?.id).map((w) {
                    return DropdownMenuItem(value: w.id, child: Text('${w.name} (${w.city})'));
                  }).toList(),
                  onChanged: (val) => setLocalState(() => destWarehouseId = val),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: carrierNameCtrl,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(labelText: 'Nome do Portador / Motorista', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: carrierDocCtrl,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(labelText: 'CPF do Portador', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(labelText: 'Observações / Lacre', labelStyle: TextStyle(color: AppTheme.textSecondary)),
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
              onPressed: destWarehouseId == null
                  ? null
                  : () async {
                      final ok = await notifier.createTransfer(
                        destinationWarehouseId: destWarehouseId!,
                        carrierName: carrierNameCtrl.text.trim(),
                        carrierDocument: carrierDocCtrl.text.trim(),
                        notes: notesCtrl.text.trim(),
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
  // MODAL DE RETIRADA VINCULADA À O.S. (REGRA DE OURO)
  // ---------------------------------------------------------------------------
  void _showCheckoutModal(BuildContext context, InventoryState state, InventoryNotifier notifier) {
    String? selectedWoId = state.demands.isNotEmpty ? state.demands.first.workOrderId : null;
    final techIdCtrl = TextEditingController(text: '01a0674f-01cc-7b04-b81b-87a0b9335d7d');
    final qtyCtrl = TextEditingController(text: '2000');
    final beforePhotoCtrl = TextEditingController(text: 'https://isperp.fotos.com/metro-inicial-2000.jpg');
    final notesCtrl = TextEditingController(text: 'Retirada de bobina 2000m');

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
                  decoration: const InputDecoration(labelText: 'Ordem de Serviço Obrigatória', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                  items: state.demands.map((d) {
                    return DropdownMenuItem(value: d.workOrderId, child: Text('${d.customerName} (${d.workOrderId.substring(0, 8)})'));
                  }).toList(),
                  onChanged: (val) => setLocalState(() => selectedWoId = val),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: qtyCtrl,
                  keyboardType: TextInputType.number,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(labelText: 'Metragem ou Quantidade Retirada', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: beforePhotoCtrl,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(
                    labelText: 'URL da Foto Inicial (Hodômetro / Marcação)',
                    labelStyle: TextStyle(color: AppTheme.textSecondary),
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  style: const TextStyle(color: AppTheme.textPrimary),
                  decoration: const InputDecoration(labelText: 'Observações da Saída', labelStyle: TextStyle(color: AppTheme.textSecondary)),
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
              onPressed: selectedWoId == null
                  ? null
                  : () async {
                      final ok = await notifier.checkoutMaterial(
                        MaterialCheckoutPayload(
                          workOrderId: selectedWoId!,
                          technicianUserId: techIdCtrl.text.trim(),
                          warehouseId: state.selectedWarehouse?.id,
                          quantityOrMeters: int.tryParse(qtyCtrl.text) ?? 2000,
                          beforePhotoUrl: beforePhotoCtrl.text.trim(),
                          notes: notesCtrl.text.trim(),
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
    final initialCtrl = TextEditingController(text: '2000');
    final consumedCtrl = TextEditingController(text: '500');
    final remainingCtrl = TextEditingController(text: '1500');
    final beforePhotoCtrl = TextEditingController(text: 'https://isperp.fotos.com/metro-inicial-2000.jpg');
    final installedPhotoCtrl = TextEditingController(text: 'https://isperp.fotos.com/instalado-poste-pto.jpg');
    final returnPhotoCtrl = TextEditingController(text: 'https://isperp.fotos.com/metro-final-1500.jpg');
    final notesCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) {
          final initVal = int.tryParse(initialCtrl.text) ?? 2000;
          final consVal = int.tryParse(consumedCtrl.text) ?? 500;
          final remVal = int.tryParse(remainingCtrl.text) ?? 1500;
          final expected = initVal - consVal;
          final hasDiv = remVal != expected;

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
                    decoration: const InputDecoration(labelText: 'Ordem de Serviço Executada', labelStyle: TextStyle(color: AppTheme.textSecondary)),
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
                          decoration: const InputDecoration(labelText: 'Metragem Inicial', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                          onChanged: (_) => setLocalState(() {}),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: consumedCtrl,
                          keyboardType: TextInputType.number,
                          style: const TextStyle(color: AppTheme.textPrimary),
                          decoration: const InputDecoration(labelText: 'Consumo na O.S.', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                          onChanged: (_) => setLocalState(() {}),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: remainingCtrl,
                          keyboardType: TextInputType.number,
                          style: const TextStyle(color: AppTheme.textPrimary),
                          decoration: const InputDecoration(labelText: 'Saldo Devolvido', labelStyle: TextStyle(color: AppTheme.textSecondary)),
                          onChanged: (_) => setLocalState(() {}),
                        ),
                      ),
                    ],
                  ),
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
}
