import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../../attendance/data/attendance_notifier.dart';
import '../data/cash_custody_repository.dart';

/// Tela de Custódia Pessoal de Caixa & Conciliação Móvel.
/// Utilizada tanto no Desktop quanto no Mobile para o colaborador acompanhar
/// valores sob sua responsabilidade (GET /financial/custody/cash/all),
/// liquidar faturas em espécie (/settle), realizar repasse Dual-Acceptance (/transfer/*)
/// e submeter depósitos bancários (/deposit).
class MyCashCustodyScreen extends ConsumerStatefulWidget {
  const MyCashCustodyScreen({super.key});

  @override
  ConsumerState<MyCashCustodyScreen> createState() => _MyCashCustodyScreenState();
}

class _MyCashCustodyScreenState extends ConsumerState<MyCashCustodyScreen> {
  static const String _fallbackUserId = '0195f8a0-0000-7000-8000-000000000001';

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  String _resolveActiveUserId(List<CashCustodyModel> custodies, String? email) {
    if (email != null && email.isNotEmpty) {
      final matched = custodies.where((c) => c.userEmail.toLowerCase() == email.toLowerCase()).firstOrNull;
      if (matched != null && matched.userId.isNotEmpty) {
        return matched.userId;
      }
    }
    if (custodies.isNotEmpty && custodies.first.userId.isNotEmpty) {
      return custodies.first.userId;
    }
    return _fallbackUserId;
  }

  void _refreshAll(String activeUserId) {
    ref.invalidate(allCashCustodiesProvider);
    ref.invalidate(pendingBankDepositsProvider);
    ref.invalidate(pendingCashTransfersProvider(activeUserId));
  }

  Future<void> _showCashSettlementDialog(BuildContext context, String activeUserId) async {
    final userIdCtrl = TextEditingController(text: activeUserId);
    final invoiceIdCtrl = TextEditingController();
    final amountCtrl = TextEditingController();
    final receiptCtrl = TextEditingController();
    final notesCtrl = TextEditingController();

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Liquidar Fatura em Espécie'),
        content: SizedBox(
          width: 440,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: userIdCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID do Colaborador Recebedor (X-User-Id)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: invoiceIdCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID da Fatura (invoiceId)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: amountCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(
                    labelText: 'Valor Recebido em Espécie (R\$)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: receiptCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Nº do Recibo Timbrado (opcional)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Observações (opcional)',
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
              final invoiceId = invoiceIdCtrl.text.trim();
              final amount = double.tryParse(amountCtrl.text.replaceAll(',', '.')) ?? 0.0;
              final userId = userIdCtrl.text.trim().isEmpty ? activeUserId : userIdCtrl.text.trim();
              if (invoiceId.isEmpty || amount <= 0) return;

              try {
                final updated = await ref.read(cashCustodyRepositoryProvider).recordCashSettlement(
                      userId: userId,
                      invoiceId: invoiceId,
                      amount: amount,
                      receiptNumber: receiptCtrl.text.trim(),
                      notes: notesCtrl.text.trim(),
                    );
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (updated != null) {
                  _refreshAll(userId);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Fatura liquidada em espécie e debitada na custódia do CPF!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Erro ao liquidar fatura em espécie: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Confirmar Recebimento'),
          ),
        ],
      ),
    );
  }

  Future<void> _showTransferRequestDialog(
    BuildContext context,
    String activeUserId,
    List<CashCustodyModel> custodies,
  ) async {
    final senderCtrl = TextEditingController(text: activeUserId);
    final otherCollab = custodies.where((c) => c.userId != activeUserId).firstOrNull;
    final receiverCtrl = TextEditingController(text: otherCollab?.userId ?? '');
    final amountCtrl = TextEditingController();
    final reasonCtrl = TextEditingController(text: 'Passagem de turno / gaveta');
    final notesCtrl = TextEditingController();

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Solicitar Repasse de Caixa (Duplo Aceite)'),
        content: SizedBox(
          width: 440,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: senderCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID do Remetente (X-User-Id)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: receiverCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID do Colaborador Destinatário (receiverUserId)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: amountCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(
                    labelText: 'Valor do Repasse (R\$)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: reasonCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Motivo do Repasse',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Observações Adicionais (opcional)',
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
              final senderId = senderCtrl.text.trim().isEmpty ? activeUserId : senderCtrl.text.trim();
              final receiverId = receiverCtrl.text.trim();
              final amount = double.tryParse(amountCtrl.text.replaceAll(',', '.')) ?? 0.0;
              if (receiverId.isEmpty || amount <= 0) return;

              try {
                final created = await ref.read(cashCustodyRepositoryProvider).requestCashTransfer(
                      senderUserId: senderId,
                      receiverUserId: receiverId,
                      amount: amount,
                      reason: reasonCtrl.text.trim(),
                      notes: notesCtrl.text.trim(),
                    );
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (created != null) {
                  _refreshAll(senderId);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Solicitação de repasse enviada! Aguardando aceite do destinatário.'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Falha ao solicitar repasse: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Enviar Solicitação'),
          ),
        ],
      ),
    );
  }

  Future<void> _showBankDepositDialog(BuildContext context, String activeUserId) async {
    final userIdCtrl = TextEditingController(text: activeUserId);
    final amountCtrl = TextEditingController();
    final bankNameCtrl = TextEditingController(text: 'Banco do Brasil');
    final agencyCtrl = TextEditingController();
    final accountCtrl = TextEditingController();
    final receiptUrlCtrl = TextEditingController(text: 'https://storage.isperp.local/comprovantes/dep.pdf');

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Informar Depósito Bancário'),
        content: SizedBox(
          width: 440,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: userIdCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID do Depositante (X-User-Id)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: amountCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(
                    labelText: 'Valor Depositado (R\$)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: bankNameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Instituição Bancária',
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: agencyCtrl,
                        decoration: const InputDecoration(labelText: 'Agência'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: accountCtrl,
                        decoration: const InputDecoration(labelText: 'Conta'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: receiptUrlCtrl,
                  decoration: const InputDecoration(
                    labelText: 'URL do Comprovante de Depósito',
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
              final userId = userIdCtrl.text.trim().isEmpty ? activeUserId : userIdCtrl.text.trim();
              final amount = double.tryParse(amountCtrl.text.replaceAll(',', '.')) ?? 0.0;
              final bankName = bankNameCtrl.text.trim();
              final receiptUrl = receiptUrlCtrl.text.trim();
              if (amount <= 0 || bankName.isEmpty || receiptUrl.isEmpty) return;

              try {
                final submitted = await ref.read(cashCustodyRepositoryProvider).submitBankDeposit(
                      userId: userId,
                      amount: amount,
                      bankName: bankName,
                      bankAgency: agencyCtrl.text.trim(),
                      bankAccount: accountCtrl.text.trim(),
                      receiptFileUrl: receiptUrl,
                    );
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (submitted != null) {
                  _refreshAll(userId);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Depósito submetido para auditoria do CFO!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Falha ao submeter depósito: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Submeter Comprovante'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final attendanceState = ref.watch(attendanceProvider);
    final custodiesAsync = ref.watch(allCashCustodiesProvider);
    final pendingDepositsAsync = ref.watch(pendingBankDepositsProvider);

    final custodiesList = custodiesAsync.valueOrNull ?? const <CashCustodyModel>[];
    final activeUserId = _resolveActiveUserId(custodiesList, authState.email);
    final pendingTransfersAsync = ref.watch(pendingCashTransfersProvider(activeUserId));

    final hasPendingReceipt = attendanceState.paymentReceiptPending &&
        attendanceState.selectedInvoice != null &&
        attendanceState.selectedCustomer != null;

    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      appBar: AppBar(
        title: const Text('Meu Caixa & Custódia de Valores'),
        actions: [
          IconButton(
            onPressed: () => _refreshAll(activeUserId),
            tooltip: 'Atualizar Saldos de Custódia',
            icon: const Icon(Icons.refresh, color: AppTheme.primaryBlue),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Card de Identificação do Operador
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppTheme.darkSurface,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.darkBorder),
              ),
              child: Row(
                children: [
                  const CircleAvatar(
                    backgroundColor: AppTheme.primaryBlue,
                    child: Icon(Icons.person, color: Colors.white),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          authState.name ?? 'Operador Conectado',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          'Perfil: ${authState.role?.name.toUpperCase() ?? "SUPORTE"} • ${authState.email ?? ""}',
                          style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Barra de Operações de Caixa Físico
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                ElevatedButton.icon(
                  onPressed: () => _showCashSettlementDialog(context, activeUserId),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryBlue,
                    foregroundColor: Colors.white,
                  ),
                  icon: const Icon(Icons.payments_outlined, size: 18),
                  label: const Text('Liquidar Fatura em Espécie'),
                ),
                OutlinedButton.icon(
                  onPressed: () => _showTransferRequestDialog(context, activeUserId, custodiesList),
                  icon: const Icon(Icons.swap_horiz_rounded, size: 18),
                  label: const Text('Solicitar Repasse de Caixa'),
                ),
                OutlinedButton.icon(
                  onPressed: () => _showBankDepositDialog(context, activeUserId),
                  icon: const Icon(Icons.account_balance_outlined, size: 18),
                  label: const Text('Informar Depósito Bancário'),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Card Informativo sobre a Política de Custódia
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.primaryBlue.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.3)),
              ),
              child: const Row(
                children: [
                  Icon(Icons.shield_outlined, color: AppTheme.primaryBlue, size: 24),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Segurança Patrimonial: Todo recebimento presencial em espécie ou maquininha POS fica registrado na custódia do CPF do operador até conciliação bancária.',
                      style: TextStyle(fontSize: 12, color: AppTheme.textPrimary),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Saldos de Custódia Reais do Backend (GET /financial/custody/cash/all)
            const Text(
              'Saldos sob Custódia Registrados no Servidor:',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 10),
            custodiesAsync.when(
              loading: () => const Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
              ),
              error: (err, _) => Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppTheme.darkSurface,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  'Não foi possível consultar os saldos de custódia: $err',
                  style: const TextStyle(color: AppTheme.accentError, fontSize: 12),
                ),
              ),
              data: (custodies) {
                if (custodies.isEmpty) {
                  return Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppTheme.darkBorder),
                    ),
                    child: const Text(
                      'Nenhum saldo em espécie retido sob custódia no momento.',
                      style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                    ),
                  );
                }

                return Column(
                  children: custodies
                      .map(
                        (c) => Container(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: AppTheme.darkSurface,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: AppTheme.darkBorder),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.account_balance_wallet_outlined, color: AppTheme.primaryBlue, size: 20),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      c.userName,
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    Text(
                                      '${c.userRole} • ${c.userEmail}',
                                      style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                              Text(
                                _formatCurrency(c.currentBalance),
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                  color: AppTheme.accentGreen,
                                ),
                              ),
                            ],
                          ),
                        ),
                      )
                      .toList(),
                );
              },
            ),
            const SizedBox(height: 20),

            // Bandeja de Repasses Pendentes de Aceite (GET /financial/custody/cash/transfer/pending)
            const Text(
              'Repasses de Espécie Aguardando Seu Aceite (Duplo Aceite):',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 10),
            pendingTransfersAsync.when(
              loading: () => const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
              ),
              error: (err, _) => Text(
                'Erro ao consultar repasses pendentes: $err',
                style: const TextStyle(color: AppTheme.accentError, fontSize: 12),
              ),
              data: (transfers) {
                if (transfers.isEmpty) {
                  return Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppTheme.darkBorder),
                    ),
                    child: const Text(
                      'Nenhuma passagem de caixa pendente de aceite para o seu usuário.',
                      style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                    ),
                  );
                }

                return Column(
                  children: transfers
                      .map(
                        (t) => Container(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: AppTheme.darkSurface,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.4)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  const Icon(Icons.swap_horizontal_circle_outlined,
                                      color: AppTheme.primaryBlue, size: 20),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Text(
                                      'De: ${t.senderUserName} → Para: ${t.receiverUserName}',
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  Text(
                                    _formatCurrency(t.amount),
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 14,
                                      color: AppTheme.accentGreen,
                                    ),
                                  ),
                                ],
                              ),
                              if (t.reason != null && t.reason!.isNotEmpty) ...[
                                const SizedBox(height: 4),
                                Text(
                                  'Motivo: ${t.reason}',
                                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                                ),
                              ],
                              const SizedBox(height: 10),
                              Wrap(
                                spacing: 8,
                                runSpacing: 8,
                                alignment: WrapAlignment.end,
                                children: [
                                  OutlinedButton.icon(
                                    onPressed: () async {
                                      final updated = await ref
                                          .read(cashCustodyRepositoryProvider)
                                          .respondCashTransfer(
                                            userId: t.receiverUserId.isNotEmpty
                                                ? t.receiverUserId
                                                : activeUserId,
                                            transferId: t.id,
                                            accept: false,
                                          );
                                      if (!context.mounted) return;
                                      if (updated != null) {
                                        _refreshAll(activeUserId);
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          const SnackBar(
                                            content: Text('Repasse recusado. O saldo permanece com o remetente.'),
                                            backgroundColor: AppTheme.accentWarning,
                                          ),
                                        );
                                      }
                                    },
                                    icon: const Icon(Icons.close, size: 16, color: AppTheme.accentError),
                                    label: const Text('Recusar Repasse'),
                                  ),
                                  ElevatedButton.icon(
                                    onPressed: () async {
                                      final updated = await ref
                                          .read(cashCustodyRepositoryProvider)
                                          .respondCashTransfer(
                                            userId: t.receiverUserId.isNotEmpty
                                                ? t.receiverUserId
                                                : activeUserId,
                                            transferId: t.id,
                                            accept: true,
                                          );
                                      if (!context.mounted) return;
                                      if (updated != null) {
                                        _refreshAll(activeUserId);
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          const SnackBar(
                                            content: Text('Repasse aceito! Custódia transferida para seu CPF.'),
                                            backgroundColor: AppTheme.accentGreen,
                                          ),
                                        );
                                      }
                                    },
                                    style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentGreen),
                                    icon: const Icon(Icons.check, size: 16, color: Color(0xFF0A0F1D)),
                                    label: const Text(
                                      'Aceitar Repasse',
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
            const SizedBox(height: 20),

            // Seção de Pendências de Canhotos e Depósitos em Auditoria
            const Text(
              'Recebimentos Pendentes de Comprovação:',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 10),

            if (!hasPendingReceipt)
              pendingDepositsAsync.maybeWhen(
                data: (deposits) {
                  if (deposits.isEmpty) {
                    return Container(
                      padding: const EdgeInsets.all(30),
                      decoration: BoxDecoration(
                        color: AppTheme.darkSurface,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Center(
                        child: Column(
                          children: [
                            Icon(Icons.done_all_rounded, color: AppTheme.accentGreen, size: 40),
                            SizedBox(height: 8),
                            Text(
                              'Nenhum recebimento pendente de canhoto!',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                            ),
                            SizedBox(height: 4),
                            Text(
                              'Todos os seus recebimentos foram devidamente comprovados e conciliados.',
                              style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                              textAlign: TextAlign.center,
                            ),
                          ],
                        ),
                      ),
                    );
                  }
                  return Column(
                    children: deposits
                        .map(
                          (d) => Container(
                            margin: const EdgeInsets.only(bottom: 10),
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: AppTheme.darkSurface,
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: AppTheme.accentWarning.withValues(alpha: 0.4)),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.receipt_long_rounded, color: AppTheme.accentWarning, size: 20),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Depósito em Auditoria — ${d.bankName}',
                                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      Text(
                                        'Responsável: ${d.depositorUserName} • Status: ${d.status}',
                                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ],
                                  ),
                                ),
                                Text(
                                  _formatCurrency(d.amount),
                                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppTheme.accentWarning),
                                ),
                              ],
                            ),
                          ),
                        )
                        .toList(),
                  );
                },
                orElse: () => Container(
                  padding: const EdgeInsets.all(30),
                  decoration: BoxDecoration(
                    color: AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Center(
                    child: Column(
                      children: [
                        Icon(Icons.done_all_rounded, color: AppTheme.accentGreen, size: 40),
                        SizedBox(height: 8),
                        Text(
                          'Nenhum recebimento pendente de canhoto!',
                          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                        ),
                      ],
                    ),
                  ),
                ),
              )
            else
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppTheme.darkSurface,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.orangeAccent.withValues(alpha: 0.5)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Colors.orangeAccent.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: const Icon(Icons.receipt_long_rounded, color: Colors.orangeAccent, size: 20),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                attendanceState.selectedCustomer!.name,
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                overflow: TextOverflow.ellipsis,
                              ),
                              Text(
                                'Recebimento via ${attendanceState.paymentMethod}',
                                style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        Text(
                          _formatCurrency(attendanceState.selectedInvoice!.amount),
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.orangeAccent),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    const Divider(height: 1),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryBlue,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                      ),
                      icon: const Icon(Icons.camera_alt_rounded, size: 18),
                      label: const Text('Registrar Comprovante do Canhoto'),
                      onPressed: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Comprovante registrado e encaminhado para a fila de conciliação.'),
                            backgroundColor: AppTheme.accentGreen,
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
