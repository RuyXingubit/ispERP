import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../data/technician_repository.dart';

/// Painel Operacional do Técnico de Campo.
/// Gestão de ordens de serviço, rotas e checklist de ativação FTTH alimentados por GET /work-orders.
class TechnicianDashboardScreen extends ConsumerStatefulWidget {
  const TechnicianDashboardScreen({super.key});

  @override
  ConsumerState<TechnicianDashboardScreen> createState() => _TechnicianDashboardScreenState();
}

class _TechnicianDashboardScreenState extends ConsumerState<TechnicianDashboardScreen> {
  int _selectedFilter = 0;

  List<TechnicianWorkOrderModel> _applyFilter(List<TechnicianWorkOrderModel> all) {
    switch (_selectedFilter) {
      case 1:
        return all.where((wo) => wo.type.toUpperCase() == 'INSTALACAO').toList();
      case 2:
        return all.where((wo) => wo.type.toUpperCase() == 'MANUTENCAO' || wo.type.toUpperCase() == 'REPARO_FIBRA').toList();
      case 3:
        return all.where((wo) => wo.type.toUpperCase() == 'RETIRADA' || wo.type.toUpperCase() == 'MUDANCA_ENDERECO').toList();
      case 0:
      default:
        return all;
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final workOrdersAsync = ref.watch(technicianWorkOrdersProvider);

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
                    children: [
                      Text(
                        'Painel do Técnico de Campo — ${authState.name ?? "Técnico"}',
                        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Ordens de Serviço atribuídas, checklist de drop óptico e ativação de ONU',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                IconButton(
                  onPressed: () => ref.invalidate(technicianWorkOrdersProvider),
                  tooltip: 'Atualizar Fila de O.S.',
                  icon: const Icon(Icons.refresh, color: AppTheme.primaryBlue),
                ),
              ],
            ),
            const SizedBox(height: 20),

            // Chips de Filtro
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildFilterChip(0, 'Todas as O.S.'),
                _buildFilterChip(1, 'Instalação FTTH'),
                _buildFilterChip(2, 'Manutenção / Sinal'),
                _buildFilterChip(3, 'Retirada / Mudança'),
              ],
            ),
            const SizedBox(height: 24),

            // Card de Status do Técnico e Fila Real de O.S.
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: const Icon(Icons.build_circle_outlined, color: AppTheme.primaryBlue, size: 22),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Fila Operacional de Campo',
                                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Técnico conectado: ${authState.email ?? ""} • Servidor: ${authState.serverUrl ?? "Local"}',
                                style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    const Divider(),
                    const SizedBox(height: 16),
                    workOrdersAsync.when(
                      loading: () => const Padding(
                        padding: EdgeInsets.symmetric(vertical: 24),
                        child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
                      ),
                      error: (err, _) => Padding(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                'Não foi possível carregar as Ordens de Serviço: $err',
                                style: const TextStyle(fontSize: 13, color: AppTheme.accentError),
                              ),
                            ),
                            OutlinedButton.icon(
                              onPressed: () => ref.invalidate(technicianWorkOrdersProvider),
                              icon: const Icon(Icons.refresh, size: 16),
                              label: const Text('Tentar Novamente'),
                            ),
                          ],
                        ),
                      ),
                      data: (allWorkOrders) {
                        final filtered = _applyFilter(allWorkOrders);
                        if (filtered.isEmpty) {
                          return Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Expanded(
                                child: Text(
                                  'Nenhuma O.S. pendente atribuída a este usuário no momento.',
                                  style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                                ),
                              ),
                              const SizedBox(width: 12),
                              OutlinedButton.icon(
                                onPressed: () => ref.invalidate(technicianWorkOrdersProvider),
                                icon: const Icon(Icons.refresh, size: 16),
                                label: const Text('Atualizar Fila', style: TextStyle(fontSize: 12)),
                              ),
                            ],
                          );
                        }

                        return Column(
                          children: filtered.map((wo) => _buildWorkOrderTile(context, wo)).toList(),
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

  Widget _buildWorkOrderTile(BuildContext context, TechnicianWorkOrderModel wo) {
    final isCompleted = wo.status.toUpperCase() == 'COMPLETED';
    final statusColor = isCompleted ? AppTheme.accentGreen : AppTheme.primaryBlue;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppTheme.darkBorder),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Text(
                      'O.S. #${wo.shortId} • ${wo.typeLabel}',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: statusColor.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        wo.statusLabel,
                        style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: statusColor),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  'Contrato: ${wo.contractId.length > 8 ? wo.contractId.substring(0, 8) : wo.contractId}'
                  '${wo.scheduledDate != null ? " • Agenda: ${wo.scheduledDate} (${wo.scheduledPeriod ?? "Comercial"})" : ""}',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  overflow: TextOverflow.ellipsis,
                ),
                if (wo.onuSerial != null || wo.fiberSignalDbm != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    'ONU: ${wo.onuSerial ?? "-"} • Sinal: ${wo.fiberSignalDbm != null ? "${wo.fiberSignalDbm} dBm" : "-"}',
                    style: const TextStyle(fontSize: 11, color: AppTheme.textMuted),
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 12),
          if (!isCompleted)
            OutlinedButton.icon(
              onPressed: () => _openChecklistDialog(context, wo),
              icon: const Icon(Icons.checklist, size: 16),
              label: const Text('Executar O.S.', style: TextStyle(fontSize: 12)),
            ),
        ],
      ),
    );
  }

  Widget _buildFilterChip(int index, String label) {
    final isSelected = _selectedFilter == index;
    return ChoiceChip(
      label: Text(label, style: TextStyle(fontSize: 12, color: isSelected ? const Color(0xFF0A0F1D) : AppTheme.textSecondary)),
      selected: isSelected,
      selectedColor: AppTheme.primaryBlue,
      backgroundColor: AppTheme.darkSurface,
      side: BorderSide(color: isSelected ? AppTheme.primaryBlue : AppTheme.darkCard),
      onSelected: (val) {
        if (val) setState(() => _selectedFilter = index);
      },
    );
  }

  void _openChecklistDialog(BuildContext context, TechnicianWorkOrderModel wo) {
    final serialCtrl = TextEditingController(text: wo.onuSerial ?? '');
    final macCtrl = TextEditingController(text: wo.onuMac ?? '');
    final signalCtrl = TextEditingController(text: wo.fiberSignalDbm?.toString() ?? '');
    final notesCtrl = TextEditingController(text: wo.notes ?? '');

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.darkSurface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (ctx) {
        bool isSubmitting = false;
        String? errorMsg;

        return StatefulBuilder(
          builder: (modalCtx, setModalState) {
            return Padding(
              padding: EdgeInsets.only(
                left: 20,
                right: 20,
                top: 20,
                bottom: MediaQuery.of(modalCtx).viewInsets.bottom + 24,
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('Execução Técnica — O.S. #${wo.shortId}', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    Text('Tipo: ${wo.typeLabel} • Contrato: ${wo.contractId}', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary), overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 16),
                    TextField(
                      controller: serialCtrl,
                      decoration: const InputDecoration(
                        labelText: 'Serial PON da ONU (ex: ZTEGC89A12B3) *',
                        prefixIcon: Icon(Icons.qr_code_scanner, size: 18),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: macCtrl,
                      decoration: const InputDecoration(
                        labelText: 'MAC Address da ONU (opcional)',
                        prefixIcon: Icon(Icons.router_outlined, size: 18),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: signalCtrl,
                      keyboardType: const TextInputType.numberWithOptions(signed: true, decimal: true),
                      decoration: const InputDecoration(
                        labelText: 'Potência Óptica Medida (dBm, ex: -19.5)',
                        prefixIcon: Icon(Icons.speed_outlined, size: 18),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: notesCtrl,
                      decoration: const InputDecoration(
                        labelText: 'Observações Técnicas de Campo',
                        prefixIcon: Icon(Icons.notes_outlined, size: 18),
                      ),
                    ),
                    if (errorMsg != null) ...[
                      const SizedBox(height: 10),
                      Text(errorMsg!, style: const TextStyle(color: AppTheme.accentError, fontSize: 12)),
                    ],
                    const SizedBox(height: 20),
                    ElevatedButton.icon(
                      onPressed: isSubmitting
                          ? null
                          : () async {
                              if (serialCtrl.text.trim().isEmpty) {
                                setModalState(() => errorMsg = 'Informe o Serial PON da ONU para concluir.');
                                return;
                              }
                              setModalState(() {
                                isSubmitting = true;
                                errorMsg = null;
                              });
                              try {
                                await ref.read(technicianRepositoryProvider).completeFieldExecution(
                                      workOrderId: wo.id,
                                      onuSerial: serialCtrl.text.trim(),
                                      onuMac: macCtrl.text.trim(),
                                      fiberSignalDbm: double.tryParse(signalCtrl.text.trim()),
                                      notes: notesCtrl.text.trim(),
                                    );
                                ref.invalidate(technicianWorkOrdersProvider);
                                if (modalCtx.mounted) Navigator.pop(modalCtx);
                              } catch (e) {
                                setModalState(() {
                                  isSubmitting = false;
                                  errorMsg = 'Falha ao concluir O.S.: $e';
                                });
                              }
                            },
                      icon: const Icon(Icons.check, size: 18),
                      label: Text(isSubmitting ? 'Processando...' : 'Concluir O.S. e Provisionar na OLT'),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }
}
