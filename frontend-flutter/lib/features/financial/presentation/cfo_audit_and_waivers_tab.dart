import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../data/cash_custody_repository.dart';

/// Aba de Auditoria de Caixa (Depósitos Bancários) & Alçadas de Taxa de O.S. (Onda C - Etapa 1).
class CfoAuditAndWaiversTab extends ConsumerStatefulWidget {
  const CfoAuditAndWaiversTab({super.key});

  @override
  ConsumerState<CfoAuditAndWaiversTab> createState() => _CfoAuditAndWaiversTabState();
}

class _CfoAuditAndWaiversTabState extends ConsumerState<CfoAuditAndWaiversTab> {
  static const String _fallbackAuditorId = '0195f8a0-0000-7000-8000-000000000001';

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  String _resolveAuditorId(List<CashCustodyModel> custodies, String? email) {
    if (email != null && email.isNotEmpty) {
      final matched = custodies
          .where((c) => c.userEmail.toLowerCase() == email.toLowerCase())
          .firstOrNull;
      if (matched != null && matched.userId.isNotEmpty) {
        return matched.userId;
      }
    }
    if (custodies.isNotEmpty && custodies.first.userId.isNotEmpty) {
      return custodies.first.userId;
    }
    return _fallbackAuditorId;
  }

  void _refreshAll() {
    ref.invalidate(allCashCustodiesProvider);
    ref.invalidate(pendingBankDepositsProvider);
    ref.invalidate(pendingWorkOrderFeeWaiversProvider);
  }

  Future<void> _showAssignFeeDialog(BuildContext context) async {
    final woIdCtrl = TextEditingController();
    final amountCtrl = TextEditingController(text: '150.00');

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Vincular Taxa a Ordem de Serviço'),
        content: SizedBox(
          width: 420,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: woIdCtrl,
                decoration: const InputDecoration(
                  labelText: 'UUID da Ordem de Serviço (workOrderId)',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: amountCtrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Valor da Taxa (R\$)',
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogCtx).pop(),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () async {
              final woId = woIdCtrl.text.trim();
              final amount = double.tryParse(amountCtrl.text.replaceAll(',', '.')) ?? 0.0;
              if (woId.isEmpty || amount <= 0) return;

              try {
                final res = await ref
                    .read(cashCustodyRepositoryProvider)
                    .assignStandardWorkOrderFee(workOrderId: woId, amount: amount);
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (res != null) {
                  _refreshAll();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Taxa financeira vinculada à Ordem de Serviço!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Erro ao vincular taxa: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Vincular Taxa'),
          ),
        ],
      ),
    );
  }

  Future<void> _showWaiverRequestDialog(BuildContext context, String userId) async {
    final userIdCtrl = TextEditingController(text: userId);
    final woIdCtrl = TextEditingController();
    final reasonCtrl = TextEditingController();

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Solicitar Isenção de Taxa de O.S.'),
        content: SizedBox(
          width: 440,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: userIdCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID do Solicitante (X-User-Id)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: woIdCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID da Ordem de Serviço (workOrderId)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: reasonCtrl,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: 'Justificativa comercial / técnica para isenção',
                  ),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogCtx).pop(),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () async {
              final woId = woIdCtrl.text.trim();
              final reason = reasonCtrl.text.trim();
              final reqUser = userIdCtrl.text.trim().isEmpty ? userId : userIdCtrl.text.trim();
              if (woId.isEmpty || reason.isEmpty) return;

              try {
                final res = await ref
                    .read(cashCustodyRepositoryProvider)
                    .requestWorkOrderFeeWaiver(
                      userId: reqUser,
                      workOrderId: woId,
                      waiverReason: reason,
                    );
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (res != null) {
                  _refreshAll();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Pedido de isenção enviado para alçada do CFO!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Erro ao solicitar isenção: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Enviar para Alçada'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final custodiesAsync = ref.watch(allCashCustodiesProvider);
    final pendingDepositsAsync = ref.watch(pendingBankDepositsProvider);
    final pendingWaiversAsync = ref.watch(pendingWorkOrderFeeWaiversProvider);

    final custodies = custodiesAsync.valueOrNull ?? const <CashCustodyModel>[];
    final auditorId = _resolveAuditorId(custodies, authState.email);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Barra de Ações de Alçada e Taxas de O.S.
        Wrap(
          spacing: 10,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            ElevatedButton.icon(
              onPressed: () => _showAssignFeeDialog(context),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
              ),
              icon: const Icon(Icons.request_quote_outlined, size: 16),
              label: const Text('Vincular Taxa a O.S.'),
            ),
            OutlinedButton.icon(
              onPressed: () => _showWaiverRequestDialog(context, auditorId),
              icon: const Icon(Icons.money_off_csred_outlined, size: 16),
              label: const Text('Solicitar Isenção de Taxa'),
            ),
            IconButton(
              onPressed: _refreshAll,
              tooltip: 'Atualizar Filas de Auditoria e Alçadas',
              icon: const Icon(Icons.refresh, color: AppTheme.primaryBlue),
            ),
          ],
        ),
        const SizedBox(height: 20),

        // Seção 1: Auditoria de Depósitos Bancários na Boca do Caixa
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Conferência de Depósitos na Boca do Caixa (Segregação de Funções)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Somente após confirmação no extrato bancário pelo CFO o saldo em espécie retido no CPF do colaborador é baixado',
                  style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 16),
                pendingDepositsAsync.when(
                  loading: () => const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (err, _) => Text(
                    'Erro ao carregar depósitos pendentes: $err',
                    style: const TextStyle(color: AppTheme.accentError),
                  ),
                  data: (deposits) {
                    if (deposits.isEmpty) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 20),
                        child: Center(
                          child: Text(
                            'Nenhum depósito bancário aguardando auditoria do CFO.',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                          ),
                        ),
                      );
                    }

                    return Column(
                      children: deposits
                          .map(
                            (dep) => Container(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: AppTheme.darkBg,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(color: AppTheme.accentWarning.withValues(alpha: 0.4)),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      const Icon(Icons.account_balance_rounded,
                                          color: AppTheme.accentWarning, size: 22),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              '${dep.bankName} — Depositante: ${dep.depositorUserName}',
                                              style: const TextStyle(
                                                fontWeight: FontWeight.bold,
                                                fontSize: 13,
                                              ),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                            Text(
                                              'Comprovante: ${dep.receiptFileUrl} • Status: ${dep.status}',
                                              style: const TextStyle(
                                                color: AppTheme.textSecondary,
                                                fontSize: 11,
                                              ),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ),
                                      ),
                                      Text(
                                        _formatCurrency(dep.amount),
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 15,
                                          color: AppTheme.accentWarning,
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 10),
                                  Wrap(
                                    spacing: 8,
                                    runSpacing: 8,
                                    alignment: WrapAlignment.end,
                                    children: [
                                      OutlinedButton.icon(
                                        onPressed: () async {
                                          final res = await ref
                                              .read(cashCustodyRepositoryProvider)
                                              .auditBankDeposit(
                                                auditorUserId: auditorId,
                                                depositId: dep.id,
                                                approved: false,
                                                rejectionReason:
                                                    'Comprovante divergente ou valor não localizado no extrato bancário.',
                                              );
                                          if (!context.mounted) return;
                                          if (res != null) {
                                            _refreshAll();
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(
                                                content: Text('Depósito rejeitado! O valor permanece na custódia do colaborador.'),
                                                backgroundColor: AppTheme.accentWarning,
                                              ),
                                            );
                                          }
                                        },
                                        icon: const Icon(Icons.close, size: 16, color: AppTheme.accentError),
                                        label: const Text('Rejeitar Depósito'),
                                      ),
                                      ElevatedButton.icon(
                                        onPressed: () async {
                                          final res = await ref
                                              .read(cashCustodyRepositoryProvider)
                                              .auditBankDeposit(
                                                auditorUserId: auditorId,
                                                depositId: dep.id,
                                                approved: true,
                                                notes: 'Confirmado no extrato bancário pelo CFO.',
                                              );
                                          if (!context.mounted) return;
                                          if (res != null) {
                                            _refreshAll();
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(
                                                content: Text('Depósito conciliado! Custódia do colaborador baixada.'),
                                                backgroundColor: AppTheme.accentGreen,
                                              ),
                                            );
                                          }
                                        },
                                        style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentGreen),
                                        icon: const Icon(Icons.verified_rounded, size: 16, color: Color(0xFF0A0F1D)),
                                        label: const Text(
                                          'Aprovar Conciliação',
                                          style: TextStyle(color: Color(0xFF0A0F1D), fontWeight: FontWeight.bold),
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          )
                          .toList(),
                    );
                  },
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 20),

        // Seção 2: Alçadas de Isenção de Taxa de Ordem de Serviço
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Alçadas de Aprovação de Isenção de Taxa de O.S.',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Prevenção de vazamento de receita: pedidos de cortesia técnica exigem aprovação explícita do gestor financeiro',
                  style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 16),
                pendingWaiversAsync.when(
                  loading: () => const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (err, _) => Text(
                    'Erro ao carregar pedidos de isenção: $err',
                    style: const TextStyle(color: AppTheme.accentError),
                  ),
                  data: (waivers) {
                    if (waivers.isEmpty) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 20),
                        child: Center(
                          child: Text(
                            'Nenhum pedido de isenção de taxa de O.S. pendente de alçada.',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                          ),
                        ),
                      );
                    }

                    return Column(
                      children: waivers
                          .map(
                            (w) => Container(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: AppTheme.darkBg,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.4)),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      const Icon(Icons.build_circle_outlined,
                                          color: AppTheme.primaryBlue, size: 22),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              'Protocolo ${w.protocol} • ${w.customerName} (${w.serviceType})',
                                              style: const TextStyle(
                                                fontWeight: FontWeight.bold,
                                                fontSize: 13,
                                              ),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                            Text(
                                              'Solicitado por: ${w.waiverRequestedByName ?? "Atendente"} • Motivo: ${w.waiverReason ?? "-"}',
                                              style: const TextStyle(
                                                color: AppTheme.textSecondary,
                                                fontSize: 11,
                                              ),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ),
                                      ),
                                      Text(
                                        _formatCurrency(w.standardFeeAmount),
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 15,
                                          color: AppTheme.primaryBlue,
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 10),
                                  Wrap(
                                    spacing: 8,
                                    runSpacing: 8,
                                    alignment: WrapAlignment.end,
                                    children: [
                                      OutlinedButton.icon(
                                        onPressed: () async {
                                          final res = await ref
                                              .read(cashCustodyRepositoryProvider)
                                              .auditWorkOrderFeeWaiver(
                                                auditorUserId: auditorId,
                                                workOrderId: w.workOrderId,
                                                approved: false,
                                                notes: 'Isenção negada pela controladoria. Manter cobrança.',
                                              );
                                          if (!context.mounted) return;
                                          if (res != null) {
                                            _refreshAll();
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(
                                                content: Text('Isenção negada! A taxa permanece faturável (BILLABLE).'),
                                                backgroundColor: AppTheme.accentWarning,
                                              ),
                                            );
                                          }
                                        },
                                        icon: const Icon(Icons.gavel_rounded, size: 16, color: AppTheme.accentError),
                                        label: const Text('Negar Isenção (Cobrar)'),
                                      ),
                                      ElevatedButton.icon(
                                        onPressed: () async {
                                          final res = await ref
                                              .read(cashCustodyRepositoryProvider)
                                              .auditWorkOrderFeeWaiver(
                                                auditorUserId: auditorId,
                                                workOrderId: w.workOrderId,
                                                approved: true,
                                                notes: 'Cortesia técnica aprovada pela controladoria.',
                                              );
                                          if (!context.mounted) return;
                                          if (res != null) {
                                            _refreshAll();
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(
                                                content: Text('Isenção de taxa de O.S. aprovada com sucesso!'),
                                                backgroundColor: AppTheme.accentGreen,
                                              ),
                                            );
                                          }
                                        },
                                        style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentGreen),
                                        icon: const Icon(Icons.check_circle_outline, size: 16, color: Color(0xFF0A0F1D)),
                                        label: const Text(
                                          'Aprovar Isenção',
                                          style: TextStyle(color: Color(0xFF0A0F1D), fontWeight: FontWeight.bold),
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          )
                          .toList(),
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
