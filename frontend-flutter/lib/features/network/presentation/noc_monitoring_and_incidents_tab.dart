import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/network_ftth_repository.dart';

/// Aba 1: Monitoramento NOC, Portas PON & Correlação Proativa de Incidentes FTTH.
class NocMonitoringAndIncidentsTab extends ConsumerStatefulWidget {
  const NocMonitoringAndIncidentsTab({super.key});

  @override
  ConsumerState<NocMonitoringAndIncidentsTab> createState() =>
      _NocMonitoringAndIncidentsTabState();
}

class _NocMonitoringAndIncidentsTabState
    extends ConsumerState<NocMonitoringAndIncidentsTab> {
  bool _isPolling = false;
  bool _onlyActiveIncidents = false;

  Future<void> _handleForcePoll() async {
    setState(() => _isPolling = true);
    try {
      await ref.read(networkFtthRepositoryProvider).forcePollNow();
      ref.invalidate(nocMonitoringSummaryProvider);
      ref.invalidate(oltPonPortsProvider);
      ref.invalidate(ftthIncidentsProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
                'Ciclo de varredura SNMP/SSH e correlação NOC concluído com sucesso.'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isPolling = false);
    }
  }

  void _showCreatePonDialog(List<NetworkDeviceModel> devices, List<FtthCableModel> cables) {
    if (devices.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Cadastre ao menos uma OLT na aba "OLTs & ONUs" antes de mapear uma porta PON.'),
          backgroundColor: AppTheme.accentWarning,
        ),
      );
      return;
    }

    String selectedDeviceId = devices.first.id;
    String? selectedCableId;
    final ponNameCtrl = TextEditingController(text: 'GPON 0/1/1');
    final slotCtrl = TextEditingController(text: '0');
    final portCtrl = TextEditingController(text: '1');
    final txPowerCtrl = TextEditingController(text: '4.5');

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Mapear Nova Porta PON em OLT'),
          content: SizedBox(
            width: 480,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  DropdownButtonFormField<String>(
                    initialValue: selectedDeviceId,
                    decoration: const InputDecoration(labelText: 'OLT / Concentrador *'),
                    items: devices
                        .map((d) => DropdownMenuItem(
                              value: d.id,
                              child: Text('${d.name} (${d.ipAddress})'),
                            ))
                        .toList(),
                    onChanged: (val) {
                      if (val != null) {
                        setModalState(() => selectedDeviceId = val);
                      }
                    },
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: ponNameCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Identificação da Porta PON *',
                      hintText: 'Ex: GPON 0/1/4',
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: slotCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Slot'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: portCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Porta'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: txPowerCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: 'TX Power (dBm)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String?>(
                    initialValue: selectedCableId,
                    decoration: const InputDecoration(
                      labelText: 'Cabo Troncal Alimentador (Opcional)',
                    ),
                    items: [
                      const DropdownMenuItem<String?>(
                        value: null,
                        child: Text('Nenhum cabo vinculado'),
                      ),
                      ...cables.map(
                        (c) => DropdownMenuItem<String?>(
                          value: c.id,
                          child: Text('${c.name} (${c.fiberCount} FO)'),
                        ),
                      ),
                    ],
                    onChanged: (val) => setModalState(() => selectedCableId = val),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                final ponName = ponNameCtrl.text.trim();
                if (ponName.isEmpty) return;
                try {
                  await ref.read(networkFtthRepositoryProvider).createPonPort(
                        networkDeviceId: selectedDeviceId,
                        slotNumber: int.tryParse(slotCtrl.text.trim()) ?? 0,
                        portNumber: int.tryParse(portCtrl.text.trim()) ?? 1,
                        ponName: ponName,
                        connectedCableId: selectedCableId,
                        txPowerDbm: double.tryParse(txPowerCtrl.text.trim()),
                      );
                  ref.invalidate(oltPonPortsProvider);
                  ref.invalidate(nocMonitoringSummaryProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content: Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Salvar Porta PON'),
            ),
          ],
        ),
      ),
    );
  }

  void _showDispatchIncidentDialog(FtthIncidentModel incident) {
    final techIdCtrl = TextEditingController();
    final notesCtrl = TextEditingController(
      text: 'Acionamento de equipe com máquina de fusão para ${incident.title}',
    );

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Despachar Equipe de Campo — NOC'),
        content: SizedBox(
          width: 460,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                incident.title,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: techIdCtrl,
                decoration: const InputDecoration(
                  labelText: 'UUID do Técnico Responsável (Opcional)',
                  hintText: 'Ex: 01944883-9b48-7b89-9a2c-15a4b7f83b1a',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: notesCtrl,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Instruções de Despacho / Ponto de Corte Estimado',
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancelar'),
          ),
          ElevatedButton.icon(
            onPressed: () async {
              try {
                await ref.read(networkFtthRepositoryProvider).dispatchIncident(
                      incident.id,
                      technicianId: techIdCtrl.text.trim(),
                      notes: notesCtrl.text.trim(),
                    );
                ref.invalidate(ftthIncidentsProvider);
                ref.invalidate(nocMonitoringSummaryProvider);
                if (ctx.mounted) Navigator.of(ctx).pop();
              } catch (e) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content: Text(e.toString().replaceFirst('Exception: ', '')),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              }
            },
            icon: const Icon(Icons.engineering_outlined, size: 16),
            label: const Text('Confirmar Despacho'),
          ),
        ],
      ),
    );
  }

  void _showResolveIncidentDialog(FtthIncidentModel incident) {
    final rootCauseCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Encerrar Incidente Massivo FTTH'),
        content: SizedBox(
          width: 460,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                incident.title,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: rootCauseCtrl,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Causa Raiz e Solução Aplicada (RFO) *',
                  hintText:
                      'Ex: Rompimento de cabo de distribuição por caminhão. Refeitas 4 fusões na CEO-01.',
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancelar'),
          ),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentGreen,
            ),
            onPressed: () async {
              final notes = rootCauseCtrl.text.trim();
              if (notes.isEmpty) return;
              try {
                await ref.read(networkFtthRepositoryProvider).resolveIncident(
                      incident.id,
                      rootCauseNotes: notes,
                    );
                ref.invalidate(ftthIncidentsProvider);
                ref.invalidate(nocMonitoringSummaryProvider);
                if (ctx.mounted) Navigator.of(ctx).pop();
              } catch (e) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content: Text(e.toString().replaceFirst('Exception: ', '')),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              }
            },
            icon: const Icon(Icons.check_circle_outline, size: 16),
            label: const Text('Concluir Incidente'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final summaryAsync = ref.watch(nocMonitoringSummaryProvider);
    final ponPortsAsync = ref.watch(oltPonPortsProvider);
    final incidentsAsync = ref.watch(ftthIncidentsProvider);
    final devicesAsync = ref.watch(networkDevicesProvider);
    final cablesAsync = ref.watch(ftthCablesProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header com botões de Varredura e Mapeamento PON
          Wrap(
            spacing: 12,
            runSpacing: 12,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Central de Monitoramento NOC & Correlação Proativa PON',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Telemetria SNMP/SSH de OLTs, detecção de rompimento óptico (LOS) vs queda de energia (Dying Gasp)',
                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                ],
              ),
              Wrap(
                spacing: 10,
                runSpacing: 8,
                children: [
                  OutlinedButton.icon(
                    onPressed: () {
                      final devices = devicesAsync.valueOrNull ?? [];
                      final cables = cablesAsync.valueOrNull ?? [];
                      _showCreatePonDialog(devices, cables);
                    },
                    icon: const Icon(Icons.add_link, size: 18),
                    label: const Text('Mapear Porta PON'),
                  ),
                  ElevatedButton.icon(
                    onPressed: _isPolling ? null : _handleForcePoll,
                    icon: _isPolling
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.radar, size: 18),
                    label: Text(_isPolling
                        ? 'Executando Varredura...'
                        : 'Forçar Ciclo de Telemetria PON'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 20),

          // Cards de Resumo NOC
          summaryAsync.when(
            loading: () => const Center(
              child: Padding(
                padding: EdgeInsets.all(24.0),
                child: CircularProgressIndicator(),
              ),
            ),
            error: (err, _) => Card(
              color: AppTheme.accentError.withValues(alpha: 0.1),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    const Icon(Icons.error_outline, color: AppTheme.accentError),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        err.toString().replaceFirst('Exception: ', ''),
                        style: const TextStyle(color: AppTheme.accentError),
                      ),
                    ),
                    TextButton(
                      onPressed: () => ref.invalidate(nocMonitoringSummaryProvider),
                      child: const Text('Tentar Novamente'),
                    ),
                  ],
                ),
              ),
            ),
            data: (summary) => Wrap(
              spacing: 14,
              runSpacing: 14,
              children: [
                _buildKpiCard(
                  'Saúde Global FTTH',
                  '${summary.globalHealthPercentage.toStringAsFixed(1)}%',
                  '${summary.totalOlts} OLTs | ${summary.activePonPorts}/${summary.totalPonPorts} PONs UP',
                  Icons.health_and_safety_outlined,
                  summary.globalHealthPercentage >= 95
                      ? AppTheme.accentGreen
                      : AppTheme.accentWarning,
                ),
                _buildKpiCard(
                  'ONUs Online',
                  '${summary.onlineOnus} / ${summary.totalOnus}',
                  'Offline total: ${summary.offlineOnus}',
                  Icons.check_circle_outline,
                  AppTheme.primaryBlue,
                ),
                _buildKpiCard(
                  'Rompimento / LOS',
                  '${summary.losOnus} ONUs',
                  'Perda de sinal óptico',
                  Icons.cable_outlined,
                  summary.losOnus > 0 ? AppTheme.accentError : AppTheme.accentGreen,
                ),
                _buildKpiCard(
                  'Dying Gasp (Energia)',
                  '${summary.dyingGaspOnus} ONUs',
                  'Queda elétrica no cliente',
                  Icons.bolt_outlined,
                  AppTheme.accentWarning,
                ),
                _buildKpiCard(
                  'Incidentes Ativos NOC',
                  '${summary.activeIncidentsCount}',
                  'Críticos: ${summary.criticalIncidentsCount}',
                  Icons.warning_amber_rounded,
                  summary.criticalIncidentsCount > 0
                      ? AppTheme.accentError
                      : AppTheme.primaryIndigo,
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // Seção de Incidentes do NOC
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Incidentes Massivos & Alarmes Correlacionados',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ),
              FilterChip(
                label: const Text('Apenas Incidentes Ativos'),
                selected: _onlyActiveIncidents,
                onSelected: (val) => setState(() => _onlyActiveIncidents = val),
              ),
            ],
          ),
          const SizedBox(height: 12),

          incidentsAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (incidents) {
              final filtered = _onlyActiveIncidents
                  ? incidents.where((i) => i.status != 'RESOLVED').toList()
                  : incidents;
              if (filtered.isEmpty) {
                return Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Row(
                      children: const [
                        Icon(Icons.verified_outlined, color: AppTheme.accentGreen),
                        SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            'Nenhum incidente massivo registrado no filtro selecionado. Rede operando dentro da normalidade.',
                            style: TextStyle(color: AppTheme.textSecondary),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }
              return Column(
                children: filtered.map((inc) {
                  final isResolved = inc.status == 'RESOLVED';
                  final isCritical = inc.severity == 'CRITICAL';
                  return Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: (isCritical
                                          ? AppTheme.accentError
                                          : AppTheme.accentWarning)
                                      .withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  '${inc.severity} • ${inc.status}',
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                    color: isCritical
                                        ? AppTheme.accentError
                                        : AppTheme.accentWarning,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  inc.title,
                                  style: const TextStyle(
                                      fontSize: 14, fontWeight: FontWeight.bold),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              if (!isResolved) ...[
                                TextButton.icon(
                                  onPressed: () =>
                                      _showDispatchIncidentDialog(inc),
                                  icon: const Icon(Icons.local_shipping_outlined,
                                      size: 16),
                                  label: const Text('Despachar Equipe'),
                                ),
                                const SizedBox(width: 8),
                                ElevatedButton.icon(
                                  onPressed: () =>
                                      _showResolveIncidentDialog(inc),
                                  icon: const Icon(Icons.check, size: 16),
                                  label: const Text('Resolver'),
                                ),
                              ],
                            ],
                          ),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 16,
                            runSpacing: 6,
                            children: [
                              Text(
                                'Tipo: ${inc.incidentTypeDescription} (${inc.incidentType})',
                                style: const TextStyle(
                                    fontSize: 12, color: AppTheme.textSecondary),
                              ),
                              Text(
                                'Clientes Afetados: ${inc.affectedCustomersCount}',
                                style: const TextStyle(
                                    fontSize: 12, fontWeight: FontWeight.w600),
                              ),
                              if (inc.oltName != null)
                                Text(
                                  'OLT: ${inc.oltName} (${inc.ponName ?? "-"})',
                                  style: const TextStyle(
                                      fontSize: 12,
                                      color: AppTheme.textSecondary),
                                ),
                              if (inc.affectedCableName != null)
                                Text(
                                  'Cabo: ${inc.affectedCableName}',
                                  style: const TextStyle(
                                      fontSize: 12,
                                      color: AppTheme.textSecondary),
                                ),
                              if (inc.workOrderProtocol != null)
                                Text(
                                  'O.S.: ${inc.workOrderProtocol}',
                                  style: const TextStyle(
                                      fontSize: 12, color: AppTheme.primaryBlue),
                                ),
                            ],
                          ),
                          if (inc.estimatedCutDetails != null &&
                              inc.estimatedCutDetails!.isNotEmpty) ...[
                            const SizedBox(height: 6),
                            Text(
                              'Ponto de Corte Estimado: ${inc.estimatedCutDetails}',
                              style: const TextStyle(
                                  fontSize: 12, color: AppTheme.accentWarning),
                            ),
                          ],
                          if (inc.rootCauseNotes != null &&
                              inc.rootCauseNotes!.isNotEmpty) ...[
                            const SizedBox(height: 6),
                            Text(
                              'RFO / Causa Raiz: ${inc.rootCauseNotes}',
                              style: const TextStyle(
                                  fontSize: 12, color: AppTheme.accentGreen),
                            ),
                          ],
                        ],
                      ),
                    ),
                  );
                }).toList(),
              );
            },
          ),
          const SizedBox(height: 24),

          // Seção de Portas PON Monitoradas
          const Text(
            'Portas PON Monitoradas nas OLTs',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),

          ponPortsAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (pons) {
              if (pons.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Text(
                      'Nenhuma porta PON mapeada até o momento. Clique em "Mapear Porta PON" para iniciar o monitoramento por porta.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }
              return Wrap(
                spacing: 12,
                runSpacing: 12,
                children: pons.map((pon) {
                  final healthy = pon.healthPercentage >= 90;
                  return Container(
                    width: 300,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: healthy
                            ? AppTheme.darkCard
                            : AppTheme.accentError.withValues(alpha: 0.5),
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
                                '${pon.ponName} • ${pon.oltName ?? "OLT"}',
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold, fontSize: 14),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 8, vertical: 2),
                              decoration: BoxDecoration(
                                color: (healthy
                                        ? AppTheme.accentGreen
                                        : AppTheme.accentError)
                                    .withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                '${pon.healthPercentage.toStringAsFixed(1)}%',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                  color: healthy
                                      ? AppTheme.accentGreen
                                      : AppTheme.accentError,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'TX: ${pon.txPowerDbm.toStringAsFixed(2)} dBm • Temp: ${pon.temperatureCelsius.toStringAsFixed(1)} °C',
                          style: const TextStyle(
                              fontSize: 12, color: AppTheme.textSecondary),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          'ONUs: ${pon.onlineOnus}/${pon.totalOnus} Online • LOS: ${pon.losOnus} • DG: ${pon.dyingGaspOnus}',
                          style: const TextStyle(fontSize: 12),
                        ),
                        if (pon.connectedCableName != null) ...[
                          const SizedBox(height: 4),
                          Text(
                            'Cabo: ${pon.connectedCableName}',
                            style: const TextStyle(
                                fontSize: 11, color: AppTheme.primaryBlue),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildKpiCard(
    String title,
    String value,
    String subtitle,
    IconData icon,
    Color color,
  ) {
    return Container(
      width: 225,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                      fontSize: 12, color: AppTheme.textSecondary),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Icon(icon, size: 18, color: color),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: TextStyle(fontSize: 11, color: color),
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
