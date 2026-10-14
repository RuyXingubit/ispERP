import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../data/payables_and_accounts_repository.dart';

/// Aba de Contas a Pagar, Cronograma de Parcelas e Plano de Contas Hierárquico (Onda C - Etapa 1).
class PayablesAndAccountsTab extends ConsumerStatefulWidget {
  const PayablesAndAccountsTab({super.key});

  @override
  ConsumerState<PayablesAndAccountsTab> createState() => _PayablesAndAccountsTabState();
}

class _PayablesAndAccountsTabState extends ConsumerState<PayablesAndAccountsTab> {
  // 'INSTALLMENTS', 'INVOICES', 'CHART_OF_ACCOUNTS'
  String _activeSection = 'INSTALLMENTS';

  static const List<String> _accountTypes = [
    'REVENUE',
    'TAX',
    'DIRECT_COST',
    'OPEX',
    'CAPEX',
  ];

  static const List<String> _dreCategories = [
    'GROSS_REVENUE',
    'TAX_DEDUCTION',
    'DIRECT_COST_INTERCONNECTION',
    'OPEX_HR',
    'OPEX_FLEET',
    'OPEX_POLES',
    'OPEX_MARKETING',
    'OPEX_ADMIN',
    'CAPEX_NETWORK',
    'CAPEX_EQUIPMENT',
    'CAPEX_FLEET',
  ];

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  void _refreshAll() {
    ref.invalidate(pendingExpenseInstallmentsProvider);
    ref.invalidate(allPayableInvoicesProvider);
    ref.invalidate(chartOfAccountsTreeProvider);
    ref.invalidate(chartOfAccountsFlatProvider);
  }

  List<ChartOfAccountModel> _flattenTree(List<ChartOfAccountModel> nodes) {
    final result = <ChartOfAccountModel>[];
    for (final node in nodes) {
      result.add(node);
      if (node.children.isNotEmpty) {
        result.addAll(_flattenTree(node.children));
      }
    }
    return result;
  }

  Future<void> _showPayInstallmentDialog(
    BuildContext context,
    ExpenseInstallmentModel installment,
  ) async {
    final amountCtrl = TextEditingController(text: installment.amount.toStringAsFixed(2));
    final receiptCtrl = TextEditingController();
    String selectedMethod = 'PIX';

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Text('Baixar Parcela #${installment.installmentNumber}'),
          content: SizedBox(
            width: 420,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: amountCtrl,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: const InputDecoration(
                      labelText: 'Valor Pago (R\$)',
                    ),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    initialValue: selectedMethod,
                    decoration: const InputDecoration(labelText: 'Meio de Pagamento'),
                    items: const [
                      DropdownMenuItem(value: 'PIX', child: Text('PIX')),
                      DropdownMenuItem(
                        value: 'TRANSFERENCIA_BANCARIA',
                        child: Text('Transferência Bancária / TED'),
                      ),
                      DropdownMenuItem(value: 'BOLETO', child: Text('Boleto Bancário')),
                      DropdownMenuItem(value: 'DINHEIRO_ESPECIE', child: Text('Espécie / Caixa')),
                    ],
                    onChanged: (val) {
                      if (val != null) {
                        setDialogState(() => selectedMethod = val);
                      }
                    },
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: receiptCtrl,
                    decoration: const InputDecoration(
                      labelText: 'URL / Código do Comprovante (opcional)',
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
                final paidAmount =
                    double.tryParse(amountCtrl.text.replaceAll(',', '.')) ?? installment.amount;
                try {
                  final res = await ref
                      .read(payablesAndAccountsRepositoryProvider)
                      .payExpenseInstallment(
                        installmentId: installment.id,
                        paidAmount: paidAmount,
                        paymentMethod: selectedMethod,
                        receiptUrl: receiptCtrl.text.trim(),
                      );
                  if (!context.mounted) return;
                  Navigator.of(dialogCtx).pop();
                  if (res != null) {
                    _refreshAll();
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Parcela liquidada com sucesso!'),
                        backgroundColor: AppTheme.accentGreen,
                      ),
                    );
                  }
                } catch (e) {
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Erro ao baixar parcela: $e'),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              },
              child: const Text('Confirmar Pagamento'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showCreatePayableDialog(
    BuildContext context,
    List<ChartOfAccountModel> flatAccounts,
  ) async {
    final supplierCtrl = TextEditingController();
    final docCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    final nfCtrl = TextEditingController();
    final totalCtrl = TextEditingController();
    final installmentsCtrl = TextEditingController(text: '1');
    final issueDateCtrl = TextEditingController(text: '2026-10-14');
    final firstDueCtrl = TextEditingController(text: '2026-11-14');
    final projectIdCtrl = TextEditingController();
    final accountIdCtrl = TextEditingController(
      text: flatAccounts.isNotEmpty ? flatAccounts.first.id : '',
    );

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Lançar Nova Conta a Pagar'),
        content: SizedBox(
          width: 480,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: supplierCtrl,
                  decoration: const InputDecoration(labelText: 'Fornecedor / Credor *'),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: docCtrl,
                        decoration: const InputDecoration(labelText: 'CNPJ / CPF'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: nfCtrl,
                        decoration: const InputDecoration(labelText: 'Nº Nota Fiscal'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                if (flatAccounts.isNotEmpty)
                  DropdownButtonFormField<String>(
                    initialValue: accountIdCtrl.text.isNotEmpty ? accountIdCtrl.text : null,
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: 'Conta Contábil (Plano de Contas) *'),
                    items: flatAccounts
                        .map(
                          (a) => DropdownMenuItem(
                            value: a.id,
                            child: Text(
                              '${a.code} - ${a.name}',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (val) {
                      if (val != null) accountIdCtrl.text = val;
                    },
                  )
                else
                  TextField(
                    controller: accountIdCtrl,
                    decoration: const InputDecoration(
                      labelText: 'UUID da Conta Contábil (chartOfAccountId) *',
                    ),
                  ),
                const SizedBox(height: 10),
                TextField(
                  controller: descCtrl,
                  decoration: const InputDecoration(labelText: 'Descrição da Despesa *'),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: totalCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'Valor Total (R\$) *'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: installmentsCtrl,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(labelText: 'Nº de Parcelas *'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: issueDateCtrl,
                        decoration: const InputDecoration(labelText: 'Data Emissão (YYYY-MM-DD)'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: firstDueCtrl,
                        decoration: const InputDecoration(labelText: '1º Vencimento (YYYY-MM-DD)'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: projectIdCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID Projeto FTTH (opcional - alocação CAPEX)',
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
              final supplier = supplierCtrl.text.trim();
              final chartId = accountIdCtrl.text.trim();
              final desc = descCtrl.text.trim();
              final total = double.tryParse(totalCtrl.text.replaceAll(',', '.')) ?? 0.0;
              final count = int.tryParse(installmentsCtrl.text.trim()) ?? 1;
              if (supplier.isEmpty || chartId.isEmpty || desc.isEmpty || total <= 0) return;

              try {
                final created = await ref
                    .read(payablesAndAccountsRepositoryProvider)
                    .createPayableInvoice(
                      supplierName: supplier,
                      supplierDocument: docCtrl.text.trim(),
                      chartOfAccountId: chartId,
                      description: desc,
                      invoiceNumber: nfCtrl.text.trim(),
                      totalAmount: total,
                      issueDate: issueDateCtrl.text.trim(),
                      installmentsCount: count,
                      firstDueDate: firstDueCtrl.text.trim(),
                      projectId: projectIdCtrl.text.trim(),
                    );
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (created != null) {
                  _refreshAll();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Despesa e cronograma de parcelas lançados com sucesso!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Erro ao lançar despesa: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Salvar Conta a Pagar'),
          ),
        ],
      ),
    );
  }

  Future<void> _showChartAccountDialog(
    BuildContext context, {
    ChartOfAccountModel? existing,
  }) async {
    final codeCtrl = TextEditingController(text: existing?.code ?? '');
    final nameCtrl = TextEditingController(text: existing?.name ?? '');
    final parentIdCtrl = TextEditingController(text: existing?.parentId ?? '');
    String selectedType = existing?.accountType ?? _accountTypes[3]; // OPEX
    String selectedDre = existing?.dreCategory ?? _dreCategories[7]; // OPEX_ADMIN
    bool acceptsEntries = existing?.acceptsEntries ?? true;
    bool active = existing?.active ?? true;

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Text(existing == null ? 'Nova Conta Contábil' : 'Editar Conta Contábil'),
          content: SizedBox(
            width: 440,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: codeCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Código Contábil (ex: 4.1.02)',
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: TextField(
                          controller: parentIdCtrl,
                          decoration: const InputDecoration(
                            labelText: 'UUID Conta Pai (opcional)',
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: nameCtrl,
                    decoration: const InputDecoration(labelText: 'Nome da Conta Contábil'),
                  ),
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: _accountTypes.contains(selectedType)
                        ? selectedType
                        : _accountTypes.first,
                    decoration: const InputDecoration(labelText: 'Natureza (accountType)'),
                    items: _accountTypes
                        .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                        .toList(),
                    onChanged: (val) {
                      if (val != null) setDialogState(() => selectedType = val);
                    },
                  ),
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: _dreCategories.contains(selectedDre)
                        ? selectedDre
                        : _dreCategories.first,
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: 'Linha do DRE (dreCategory)'),
                    items: _dreCategories
                        .map((c) => DropdownMenuItem(value: c, child: Text(c)))
                        .toList(),
                    onChanged: (val) {
                      if (val != null) setDialogState(() => selectedDre = val);
                    },
                  ),
                  const SizedBox(height: 8),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Aceita Lançamentos Diretos (Analítica)', style: TextStyle(fontSize: 13)),
                    value: acceptsEntries,
                    onChanged: (v) => setDialogState(() => acceptsEntries = v),
                  ),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Conta Ativa', style: TextStyle(fontSize: 13)),
                    value: active,
                    onChanged: (v) => setDialogState(() => active = v),
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
                final code = codeCtrl.text.trim();
                final name = nameCtrl.text.trim();
                if (code.isEmpty || name.isEmpty) return;

                try {
                  final repo = ref.read(payablesAndAccountsRepositoryProvider);
                  final res = existing == null
                      ? await repo.createChartOfAccount(
                          parentId: parentIdCtrl.text.trim(),
                          code: code,
                          name: name,
                          accountType: selectedType,
                          dreCategory: selectedDre,
                          acceptsEntries: acceptsEntries,
                          active: active,
                        )
                      : await repo.updateChartOfAccount(
                          id: existing.id,
                          parentId: parentIdCtrl.text.trim(),
                          code: code,
                          name: name,
                          accountType: selectedType,
                          dreCategory: selectedDre,
                          acceptsEntries: acceptsEntries,
                          active: active,
                        );
                  if (!context.mounted) return;
                  Navigator.of(dialogCtx).pop();
                  if (res != null) {
                    _refreshAll();
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(
                          existing == null
                              ? 'Conta contábil criada com sucesso!'
                              : 'Conta contábil atualizada com sucesso!',
                        ),
                        backgroundColor: AppTheme.accentGreen,
                      ),
                    );
                  }
                } catch (e) {
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Erro ao salvar conta contábil: $e'),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              },
              child: const Text('Salvar Conta'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final installmentsAsync = ref.watch(pendingExpenseInstallmentsProvider);
    final payablesAsync = ref.watch(allPayableInvoicesProvider);
    final treeAsync = ref.watch(chartOfAccountsTreeProvider);
    final flatAsync = ref.watch(chartOfAccountsFlatProvider);

    final flatAccounts = flatAsync.valueOrNull ?? const <ChartOfAccountModel>[];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Barra superior de alternância e ações rápidas
        Wrap(
          spacing: 10,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            ChoiceChip(
              label: const Text('Parcelas Pendentes'),
              selected: _activeSection == 'INSTALLMENTS',
              selectedColor: AppTheme.primaryBlue,
              onSelected: (_) => setState(() => _activeSection = 'INSTALLMENTS'),
            ),
            ChoiceChip(
              label: const Text('Notas & Despesas'),
              selected: _activeSection == 'INVOICES',
              selectedColor: AppTheme.primaryBlue,
              onSelected: (_) => setState(() => _activeSection = 'INVOICES'),
            ),
            ChoiceChip(
              label: const Text('Plano de Contas Hierárquico'),
              selected: _activeSection == 'CHART_OF_ACCOUNTS',
              selectedColor: AppTheme.primaryBlue,
              onSelected: (_) => setState(() => _activeSection = 'CHART_OF_ACCOUNTS'),
            ),
            const SizedBox(width: 8),
            ElevatedButton.icon(
              onPressed: () => _showCreatePayableDialog(context, flatAccounts),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
              ),
              icon: const Icon(Icons.add_card_rounded, size: 16),
              label: const Text('Lançar Conta a Pagar'),
            ),
            OutlinedButton.icon(
              onPressed: () => _showChartAccountDialog(context),
              icon: const Icon(Icons.account_tree_outlined, size: 16),
              label: const Text('Nova Conta Contábil'),
            ),
            IconButton(
              onPressed: _refreshAll,
              tooltip: 'Atualizar Contas a Pagar e Plano de Contas',
              icon: const Icon(Icons.refresh, color: AppTheme.primaryBlue),
            ),
          ],
        ),
        const SizedBox(height: 20),

        if (_activeSection == 'INSTALLMENTS')
          _buildInstallmentsSection(context, installmentsAsync),
        if (_activeSection == 'INVOICES')
          _buildInvoicesSection(context, payablesAsync),
        if (_activeSection == 'CHART_OF_ACCOUNTS')
          _buildChartOfAccountsSection(context, treeAsync, flatAccounts),
      ],
    );
  }

  Widget _buildInstallmentsSection(
    BuildContext context,
    AsyncValue<List<ExpenseInstallmentModel>> installmentsAsync,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Cronograma de Parcelas Pendentes de Pagamento',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            const Text(
              'Baixe parcelas liquidadas para alimentar o Regime de Caixa da DRE e o Fluxo de Desalavancagem',
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 16),
            installmentsAsync.when(
              loading: () => const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              ),
              error: (err, _) => Text(
                'Erro ao carregar parcelas pendentes: $err',
                style: const TextStyle(color: AppTheme.accentError),
              ),
              data: (installments) {
                if (installments.isEmpty) {
                  return const Padding(
                    padding: EdgeInsets.symmetric(vertical: 24),
                    child: Center(
                      child: Text(
                        'Nenhuma parcela de despesa pendente de pagamento.',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ),
                  );
                }

                return Column(
                  children: installments
                      .map(
                        (inst) => Container(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: AppTheme.darkBg,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: AppTheme.darkBorder),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.event_note_rounded, color: AppTheme.accentWarning, size: 22),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Parcela #${inst.installmentNumber} • Vencimento: ${inst.dueDate}',
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    Text(
                                      'Fatura ID: ${inst.payableInvoiceId} • Status: ${inst.status}',
                                      style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 12),
                              Text(
                                _formatCurrency(inst.amount),
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                  color: AppTheme.accentWarning,
                                ),
                              ),
                              const SizedBox(width: 12),
                              ElevatedButton.icon(
                                onPressed: () => _showPayInstallmentDialog(context, inst),
                                style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentGreen),
                                icon: const Icon(Icons.check_circle_outline, size: 16, color: Color(0xFF0A0F1D)),
                                label: const Text(
                                  'Baixar Parcela',
                                  style: TextStyle(color: Color(0xFF0A0F1D), fontWeight: FontWeight.bold, fontSize: 12),
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
          ],
        ),
      ),
    );
  }

  Widget _buildInvoicesSection(
    BuildContext context,
    AsyncValue<List<PayableInvoiceModel>> payablesAsync,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Notas Fiscais de Entrada & Despesas Lançadas',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            const Text(
              'Compromissos registrados pelo Regime de Competência vinculados ao Plano de Contas',
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 16),
            payablesAsync.when(
              loading: () => const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              ),
              error: (err, _) => Text(
                'Erro ao carregar despesas: $err',
                style: const TextStyle(color: AppTheme.accentError),
              ),
              data: (invoices) {
                if (invoices.isEmpty) {
                  return const Padding(
                    padding: EdgeInsets.symmetric(vertical: 24),
                    child: Center(
                      child: Text(
                        'Nenhuma despesa ou nota fiscal a pagar registrada.',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ),
                  );
                }

                return Column(
                  children: invoices
                      .map(
                        (inv) => Container(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: AppTheme.darkBg,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: AppTheme.darkBorder),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.description_outlined, color: AppTheme.primaryBlue, size: 22),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      '${inv.supplierName} — ${inv.description}',
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    Text(
                                      'Conta: ${inv.chartOfAccountCode ?? inv.chartOfAccountId} ${inv.chartOfAccountName ?? ""} • Emissão: ${inv.issueDate} • Parcelas: ${inv.installments.length}',
                                      style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    if (inv.projectName != null && inv.projectName!.isNotEmpty)
                                      Text(
                                        'Projeto FTTH: ${inv.projectName}',
                                        style: const TextStyle(color: AppTheme.primaryBlue, fontSize: 11),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 12),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(
                                    _formatCurrency(inv.totalAmount),
                                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                  ),
                                  Text(
                                    inv.status,
                                    style: TextStyle(
                                      fontSize: 11,
                                      color: inv.status == 'PAID'
                                          ? AppTheme.accentGreen
                                          : AppTheme.accentWarning,
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
    );
  }

  Widget _buildChartOfAccountsSection(
    BuildContext context,
    AsyncValue<List<ChartOfAccountModel>> treeAsync,
    List<ChartOfAccountModel> flatFallback,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Plano de Contas Hierárquico Telecom',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            const Text(
              'Estrutura contábil multinível mapeada para apuração automática do DRE (Receitas, Tributos, Custo de Link, OPEX e CAPEX)',
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 16),
            treeAsync.when(
              loading: () => const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              ),
              error: (err, _) => Text(
                'Erro ao carregar Plano de Contas: $err',
                style: const TextStyle(color: AppTheme.accentError),
              ),
              data: (tree) {
                final accounts = tree.isNotEmpty ? _flattenTree(tree) : flatFallback;
                if (accounts.isEmpty) {
                  return const Padding(
                    padding: EdgeInsets.symmetric(vertical: 24),
                    child: Center(
                      child: Text(
                        'Nenhuma conta contábil cadastrada no Plano de Contas.',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ),
                  );
                }

                return Column(
                  children: accounts
                      .map(
                        (acc) => Container(
                          margin: EdgeInsets.only(
                            bottom: 8,
                            left: ((acc.level - 1).clamp(0, 4)) * 16.0,
                          ),
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                          decoration: BoxDecoration(
                            color: AppTheme.darkBg,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: AppTheme.darkBorder),
                          ),
                          child: Row(
                            children: [
                              Icon(
                                acc.acceptsEntries
                                    ? Icons.subdirectory_arrow_right_rounded
                                    : Icons.folder_outlined,
                                color: acc.acceptsEntries
                                    ? AppTheme.primaryBlue
                                    : AppTheme.accentWarning,
                                size: 18,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      '${acc.code} — ${acc.name}',
                                      style: TextStyle(
                                        fontWeight: acc.acceptsEntries
                                            ? FontWeight.w600
                                            : FontWeight.bold,
                                        fontSize: 13,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    Text(
                                      'Tipo: ${acc.accountType} • DRE: ${acc.dreCategory} • ${acc.acceptsEntries ? "Analítica" : "Sintética"}',
                                      style: const TextStyle(
                                        color: AppTheme.textSecondary,
                                        fontSize: 11,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                              IconButton(
                                onPressed: () => _showChartAccountDialog(context, existing: acc),
                                tooltip: 'Editar Conta Contábil',
                                icon: const Icon(Icons.edit_outlined, size: 18, color: AppTheme.primaryBlue),
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
    );
  }
}
