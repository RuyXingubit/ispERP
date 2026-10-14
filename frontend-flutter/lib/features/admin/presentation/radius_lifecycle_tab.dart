import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/radius_ipam_marcocivil_repository.dart';

/// Aba 3 de `/admin/nas`: Políticas de Auto-Corte, Ciclo de Vida RADIUS e Auditoria de Bloqueios/Desbloqueios.
class RadiusLifecycleTab extends ConsumerStatefulWidget {
  const RadiusLifecycleTab({super.key});

  @override
  ConsumerState<RadiusLifecycleTab> createState() => _RadiusLifecycleTabState();
}

class _RadiusLifecycleTabState extends ConsumerState<RadiusLifecycleTab> {
  bool _runningEngine = false;

  Future<void> _handleRunNow() async {
    setState(() => _runningEngine = true);
    try {
      final msg =
          await ref.read(radiusIpamMarcoCivilRepositoryProvider).runAutoBlockNow();
      ref.invalidate(radiusLifecycleSummaryProvider);
      ref.invalidate(radiusLifecycleLogsProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(msg),
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
      if (mounted) setState(() => _runningEngine = false);
    }
  }

  void _showEditPolicyDialog(RadiusPolicyConfigModel current) {
    bool autoBlockEnabled = current.autoBlockEnabled;
    String blockMode = current.blockMode;
    bool unblockOnPayment = current.unblockOnPayment;
    bool sendPodOnBlock = current.sendPodOnBlock;
    bool allowFriday = current.allowBlockOnFriday;
    bool protectHolidays = current.protectEveOfHolidays;
    final tolCtrl = TextEditingController(text: '${current.toleranceDays}');
    final downCtrl =
        TextEditingController(text: '${current.reducedDownloadKbps}');
    final upCtrl = TextEditingController(text: '${current.reducedUploadKbps}');
    final startCtrl = TextEditingController(text: '${current.blockStartHour}');
    final endCtrl = TextEditingController(text: '${current.blockEndHour}');

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Configurar Política de Auto-Corte RADIUS'),
          content: SizedBox(
            width: 500,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Motor de Auto-Corte Automático Ativo'),
                    value: autoBlockEnabled,
                    onChanged: (v) =>
                        setModalState(() => autoBlockEnabled = v),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: tolCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'Dias de Tolerância *'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        flex: 2,
                        child: DropdownButtonFormField<String>(
                          initialValue: blockMode,
                          decoration: const InputDecoration(
                              labelText: 'Modo de Bloqueio no BNG'),
                          items: const [
                            DropdownMenuItem(
                              value: 'CAPTIVE_PORTAL',
                              child: Text('CAPTIVE_PORTAL (Mural)'),
                            ),
                            DropdownMenuItem(
                              value: 'BANDWIDTH_REDUCTION',
                              child: Text('BANDWIDTH_REDUCTION'),
                            ),
                            DropdownMenuItem(
                              value: 'COMPLETE_DISCONNECT',
                              child: Text('COMPLETE_DISCONNECT'),
                            ),
                          ],
                          onChanged: (v) {
                            if (v != null) setModalState(() => blockMode = v);
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: downCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'Download Reduzido (Kbps)'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: upCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'Upload Reduzido (Kbps)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: startCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'Hora Inicial (0-23h)'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: endCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'Hora Final (0-23h)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Desbloquear automaticamente na baixa bancária/PIX'),
                    value: unblockOnPayment,
                    onChanged: (v) =>
                        setModalState(() => unblockOnPayment = v ?? true),
                  ),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Enviar PoD imediato ao aplicar bloqueio/desbloqueio'),
                    value: sendPodOnBlock,
                    onChanged: (v) =>
                        setModalState(() => sendPodOnBlock = v ?? true),
                  ),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Permitir cortes às sextas-feiras'),
                    value: allowFriday,
                    onChanged: (v) =>
                        setModalState(() => allowFriday = v ?? false),
                  ),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Proteger vésperas de feriado (Anatel)'),
                    value: protectHolidays,
                    onChanged: (v) =>
                        setModalState(() => protectHolidays = v ?? true),
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
                try {
                  await ref
                      .read(radiusIpamMarcoCivilRepositoryProvider)
                      .updateLifecyclePolicy(
                        autoBlockEnabled: autoBlockEnabled,
                        toleranceDays: int.tryParse(tolCtrl.text.trim()) ?? 5,
                        blockMode: blockMode,
                        reducedDownloadKbps:
                            int.tryParse(downCtrl.text.trim()) ?? 1024,
                        reducedUploadKbps:
                            int.tryParse(upCtrl.text.trim()) ?? 512,
                        unblockOnPayment: unblockOnPayment,
                        sendPodOnBlock: sendPodOnBlock,
                        sendPodOnUnblock: sendPodOnBlock,
                        blockStartHour:
                            int.tryParse(startCtrl.text.trim()) ?? 8,
                        blockEndHour: int.tryParse(endCtrl.text.trim()) ?? 18,
                        allowBlockOnFriday: allowFriday,
                        protectEveOfHolidays: protectHolidays,
                      );
                  ref.invalidate(radiusLifecyclePolicyProvider);
                  ref.invalidate(radiusLifecycleSummaryProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content:
                            Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Salvar Política'),
            ),
          ],
        ),
      ),
    );
  }

  void _showManualActionDialog() {
    final contractCtrl = TextEditingController();
    final reasonCtrl = TextEditingController(text: 'Ação manual via NOC/Financeiro');
    String action = 'BLOCK';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Executar Bloqueio / Desbloqueio Manual RADIUS'),
          content: SizedBox(
            width: 440,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: contractCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID do Contrato *',
                    hintText: 'Ex: 01944883-9b48-7b89-9a2c-15a4b7f83b1c',
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: action,
                  decoration: const InputDecoration(labelText: 'Ação no RADIUS'),
                  items: const [
                    DropdownMenuItem(
                        value: 'BLOCK', child: Text('BLOCK (Bloquear Contrato)')),
                    DropdownMenuItem(
                        value: 'UNBLOCK',
                        child: Text('UNBLOCK (Liberar Contrato)')),
                  ],
                  onChanged: (v) {
                    if (v != null) setModalState(() => action = v);
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: reasonCtrl,
                  decoration: const InputDecoration(labelText: 'Justificativa'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                final contractId = contractCtrl.text.trim();
                if (contractId.isEmpty) return;
                try {
                  final res = await ref
                      .read(radiusIpamMarcoCivilRepositoryProvider)
                      .executeManualAction(
                        contractId: contractId,
                        action: action,
                        reason: reasonCtrl.text.trim(),
                      );
                  ref.invalidate(radiusLifecycleSummaryProvider);
                  ref.invalidate(radiusLifecycleLogsProvider);
                  if (ctx.mounted) {
                    Navigator.of(ctx).pop();
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('${res.username}: ${res.message}'),
                        backgroundColor: AppTheme.accentGreen,
                      ),
                    );
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content:
                            Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Aplicar Ação'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final summaryAsync = ref.watch(radiusLifecycleSummaryProvider);
    final policyAsync = ref.watch(radiusLifecyclePolicyProvider);
    final logsAsync = ref.watch(radiusLifecycleLogsProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
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
                    'Motor de Auto-Corte & Ciclo de Vida RADIUS',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Automação de corte por inadimplência, desbloqueio em confiança, janelas de proteção e auditoria de PoD',
                    style:
                        TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                ],
              ),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  OutlinedButton.icon(
                    onPressed: _showManualActionDialog,
                    icon: const Icon(Icons.gavel_outlined, size: 16),
                    label: const Text('Ação Manual Contrato'),
                  ),
                  ElevatedButton.icon(
                    onPressed: _runningEngine ? null : _handleRunNow,
                    icon: const Icon(Icons.play_arrow, size: 18),
                    label: Text(_runningEngine
                        ? 'Executando...'
                        : 'Disparar Varredura Auto-Corte'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Resumo estatístico
          summaryAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (s) => Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _buildKpiCard(
                  'Total PPPoE',
                  '${s.totalPppoeUsers}',
                  'Ativos: ${s.totalActiveUsers}',
                  AppTheme.primaryBlue,
                ),
                _buildKpiCard(
                  'Bloqueados no RADIUS',
                  '${s.totalBlockedUsers}',
                  'Auto-cortes hoje: ${s.todayAutoBlocksCount}',
                  s.totalBlockedUsers > 0
                      ? AppTheme.accentError
                      : AppTheme.accentGreen,
                ),
                _buildKpiCard(
                  'Desbloqueio Confiança',
                  '${s.totalTrustUnblocked}',
                  'Liberações hoje: ${s.todayUnblocksCount}',
                  AppTheme.accentWarning,
                ),
                _buildKpiCard(
                  'Política de Tolerância',
                  '${s.toleranceDays} dias',
                  s.autoBlockEnabled ? 'Motor Automático ON' : 'Motor Pausado',
                  AppTheme.accentGreen,
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Card de Política Atual
          policyAsync.when(
            loading: () => const SizedBox.shrink(),
            error: (_, _) => const SizedBox.shrink(),
            data: (policy) => Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    const Icon(Icons.policy_outlined,
                        color: AppTheme.primaryBlue),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Política Vigente: ${policy.blockMode} • Tolerância: ${policy.toleranceDays} dias • Janela: ${policy.blockStartHour}h às ${policy.blockEndHour}h',
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, fontSize: 13),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Banda Reduzida: ↓${policy.reducedDownloadKbps} Kbps / ↑${policy.reducedUploadKbps} Kbps • Proteger Véspera Feriado: ${policy.protectEveOfHolidays ? "Sim" : "Não"}',
                            style: const TextStyle(
                                fontSize: 12, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                    ),
                    OutlinedButton.icon(
                      onPressed: () => _showEditPolicyDialog(policy),
                      icon: const Icon(Icons.edit_outlined, size: 16),
                      label: const Text('Editar Política'),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Logs de Auditoria do Ciclo de Vida RADIUS
          const Text(
            'Trilha de Auditoria do Ciclo de Vida RADIUS',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          logsAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (logs) {
              if (logs.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: Text(
                      'Nenhum evento registrado no histórico de auto-corte.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }
              return Column(
                children: logs.map((l) {
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(
                      dense: true,
                      leading: Icon(
                        l.success
                            ? Icons.check_circle_outline
                            : Icons.error_outline,
                        color: l.success
                            ? AppTheme.accentGreen
                            : AppTheme.accentError,
                      ),
                      title: Text(
                        '${l.actionType} • ${l.username} (${l.customerName ?? "Contrato ${l.contractId}"})',
                        style: const TextStyle(
                            fontSize: 13, fontWeight: FontWeight.bold),
                      ),
                      subtitle: Text(
                        '${l.reason ?? "-"} • NAS: ${l.nasIp ?? "-"} • ${l.createdAt}',
                        style: const TextStyle(
                            fontSize: 11, color: AppTheme.textSecondary),
                      ),
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
      String title, String value, String sub, Color accentColor) {
    return Container(
      width: 220,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title,
              style:
                  const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
          const SizedBox(height: 6),
          Text(value,
              style:
                  const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Text(sub, style: TextStyle(fontSize: 11, color: accentColor)),
        ],
      ),
    );
  }
}
