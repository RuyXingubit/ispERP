import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../data/strategic_finance_repository.dart';

/// Aba de DRE Telecom (Competência vs Caixa) e Motor de Desalavancagem 36M + Simulador CAPEX (Onda C - Etapa 2).
class DreAndDeleveragingTab extends ConsumerStatefulWidget {
  const DreAndDeleveragingTab({super.key});

  @override
  ConsumerState<DreAndDeleveragingTab> createState() => _DreAndDeleveragingTabState();
}

class _DreAndDeleveragingTabState extends ConsumerState<DreAndDeleveragingTab> {
  String _startDate = '2026-10-01';
  String _endDate = '2026-10-31';
  String _method = 'ACCRUAL'; // 'ACCRUAL' | 'CASH'
  InvestmentSimulationResultModel? _lastSimulation;

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  DreFilterParams get _currentParams => DreFilterParams(
        startDate: _startDate,
        endDate: _endDate,
        method: _method,
      );

  void _refreshAll() {
    ref.invalidate(dreReportProvider(_currentParams));
    ref.invalidate(deleveragingProjectionProvider);
  }

  Future<void> _showPeriodDialog(BuildContext context) async {
    final startCtrl = TextEditingController(text: _startDate);
    final endCtrl = TextEditingController(text: _endDate);

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Período de Apuração do DRE'),
        content: SizedBox(
          width: 380,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: startCtrl,
                decoration: const InputDecoration(labelText: 'Data Inicial (YYYY-MM-DD)'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: endCtrl,
                decoration: const InputDecoration(labelText: 'Data Final (YYYY-MM-DD)'),
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
            onPressed: () {
              final s = startCtrl.text.trim();
              final e = endCtrl.text.trim();
              if (s.isNotEmpty && e.isNotEmpty) {
                setState(() {
                  _startDate = s;
                  _endDate = e;
                });
              }
              Navigator.of(dialogCtx).pop();
            },
            child: const Text('Aplicar Período'),
          ),
        ],
      ),
    );
  }

  Future<void> _showSimulationDialog(BuildContext context) async {
    final descCtrl = TextEditingController(text: 'Aquisição de OLT GPON 16 Portas');
    final amountCtrl = TextEditingController(text: '48000.00');
    final installmentsCtrl = TextEditingController(text: '12');
    final firstDueCtrl = TextEditingController(text: '2026-11-15');

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Simulador de Impacto de CAPEX / Empréstimo (36M)'),
        content: SizedBox(
          width: 440,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: descCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Descrição do Investimento / Equipamento',
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: amountCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'Valor Total (R\$)'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: installmentsCtrl,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(labelText: 'Nº Parcelas'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: firstDueCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Data do 1º Vencimento (YYYY-MM-DD)',
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
              final desc = descCtrl.text.trim();
              final total = double.tryParse(amountCtrl.text.replaceAll(',', '.')) ?? 0.0;
              final count = int.tryParse(installmentsCtrl.text.trim()) ?? 1;
              final due = firstDueCtrl.text.trim();
              if (desc.isEmpty || total <= 0 || due.isEmpty) return;

              try {
                final sim = await ref
                    .read(strategicFinanceRepositoryProvider)
                    .simulateNewInvestment(
                      description: desc,
                      totalAmount: total,
                      installmentsCount: count,
                      firstDueDate: due,
                    );
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (sim != null) {
                  setState(() {
                    _lastSimulation = sim;
                  });
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Erro ao simular investimento: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Executar Simulação'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final dreAsync = ref.watch(dreReportProvider(_currentParams));
    final projectionAsync = ref.watch(deleveragingProjectionProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Barra de Controles de Regime Contábil, Período e Simulador
        Wrap(
          spacing: 10,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            ChoiceChip(
              label: const Text('Regime de Competência (ACCRUAL)'),
              selected: _method == 'ACCRUAL',
              selectedColor: AppTheme.primaryBlue,
              onSelected: (_) => setState(() => _method = 'ACCRUAL'),
            ),
            ChoiceChip(
              label: const Text('Regime de Caixa (CASH)'),
              selected: _method == 'CASH',
              selectedColor: AppTheme.primaryBlue,
              onSelected: (_) => setState(() => _method = 'CASH'),
            ),
            OutlinedButton.icon(
              onPressed: () => _showPeriodDialog(context),
              icon: const Icon(Icons.date_range_outlined, size: 16),
              label: Text('Período: $_startDate a $_endDate'),
            ),
            ElevatedButton.icon(
              onPressed: () => _showSimulationDialog(context),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
              ),
              icon: const Icon(Icons.calculate_outlined, size: 16),
              label: const Text('Simular Novo CAPEX / Empréstimo'),
            ),
            IconButton(
              onPressed: _refreshAll,
              tooltip: 'Recalcular DRE e Projeção 36M',
              icon: const Icon(Icons.refresh, color: AppTheme.primaryBlue),
            ),
          ],
        ),
        const SizedBox(height: 20),

        // Seção 1: Demonstrativo do Resultado do Exercício (DRE Telecom)
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.assessment_outlined, color: AppTheme.primaryBlue, size: 22),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'DRE Telecom Oficial (${_method == "ACCRUAL" ? "Regime de Competência" : "Regime de Caixa"})',
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                dreAsync.when(
                  loading: () => const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (err, _) => Text(
                    'Erro ao apurar DRE: $err',
                    style: const TextStyle(color: AppTheme.accentError),
                  ),
                  data: (dre) {
                    if (dre == null) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 20),
                        child: Center(
                          child: Text(
                            'Nenhum dado contábil disponível para o período selecionado.',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                          ),
                        ),
                      );
                    }

                    return Column(
                      children: [
                        _buildDreLine('(=) Receita Operacional Bruta', dre.grossRevenue,
                            isBold: true, color: AppTheme.accentGreen),
                        _buildDreLine('(-) Deduções e Impostos (ICMS/PIS/COFINS)',
                            -dre.taxDeductions),
                        const Divider(height: 16),
                        _buildDreLine('(=) Receita Operacional Líquida (ROL)',
                            dre.netRevenue,
                            isBold: true),
                        _buildDreLine(
                            '(-) Custos Diretos (Link Dedicado / Trânsito IP / CDN)',
                            -dre.directCostsInterconnection),
                        const Divider(height: 16),
                        _buildDreLine('(=) Margem de Contribuição',
                            dre.contributionMargin,
                            isBold: true, color: AppTheme.primaryBlue),
                        _buildDreLine('(-) OPEX Pessoal & Encargos', -dre.opexPersonnel),
                        _buildDreLine(
                            '(-) OPEX Infraestrutura (Aluguel de Postes / Frota / Energia)',
                            -dre.opexInfrastructure),
                        _buildDreLine(
                            '(-) OPEX Administrativo & Sistemas', -dre.opexAdministrative),
                        _buildDreLine(
                            '(-) OPEX Comercial & Marketing', -dre.opexCommercial),
                        _buildDreLine('(-) Total OPEX Operacional', -dre.totalOpex,
                            isBold: true),
                        const Divider(height: 16),
                        _buildDreLine(
                          '(=) EBITDA (${dre.ebitdaMarginPercentage.toStringAsFixed(1)}%)',
                          dre.ebitda,
                          isBold: true,
                          color: dre.ebitda >= 0
                              ? AppTheme.accentGreen
                              : AppTheme.accentError,
                        ),
                        _buildDreLine(
                            '(+/-) Resultado Financeiro / Juros', dre.financialResult),
                        const Divider(height: 16),
                        _buildDreLine(
                          '(=) Resultado Líquido do Exercício (${dre.netMarginPercentage.toStringAsFixed(1)}%)',
                          dre.netIncome,
                          isBold: true,
                          color: dre.netIncome >= 0
                              ? AppTheme.accentGreen
                              : AppTheme.accentError,
                        ),
                      ],
                    );
                  },
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 20),

        // Card de Resultado de Simulação CAPEX (se executado)
        if (_lastSimulation != null) ...[
          Card(
            color: (_lastSimulation!.feasible ? AppTheme.accentGreen : AppTheme.accentError)
                .withValues(alpha: 0.08),
            shape: RoundedRectangleBorder(
              side: BorderSide(
                color: _lastSimulation!.feasible ? AppTheme.accentGreen : AppTheme.accentError,
              ),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        _lastSimulation!.feasible
                            ? Icons.verified_rounded
                            : Icons.warning_amber_rounded,
                        color: _lastSimulation!.feasible
                            ? AppTheme.accentGreen
                            : AppTheme.accentError,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          _lastSimulation!.feasible
                              ? 'Simulação Aprovada: Investimento Sustentável pelo Fluxo de Caixa'
                              : 'Alerta de Risco de Insolvência: Investimento Compromete o Caixa Mínimo',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Parcela Mensal: ${_formatCurrency(_lastSimulation!.monthlyInstallmentAmount)} • Pior Mês Simulado: ${_lastSimulation!.simulatedWorstMonth} (${_formatCurrency(_lastSimulation!.simulatedWorstBalance)}) • Atraso p/ Liberdade Financeira: ${_lastSimulation!.delayInMonthsForFreedom} meses',
                    style: const TextStyle(fontSize: 12),
                  ),
                  if (_lastSimulation!.riskAnalysisSummary.isNotEmpty) ...[
                    const SizedBox(height: 6),
                    Text(
                      _lastSimulation!.riskAnalysisSummary,
                      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
        ],

        // Seção 2: Motor de Desalavancagem de 36 Meses & Os 3 Números Sagrados
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Motor de Desalavancagem de 36 Meses & Os 3 Números Sagrados',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Projeção contínua de fôlego de caixa cruzando MRR, Churn, OPEX e cronograma real de parcelas de dívidas/fornecedores',
                  style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 16),
                projectionAsync.when(
                  loading: () => const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (err, _) => Text(
                    'Erro ao calcular projeção de 36 meses: $err',
                    style: const TextStyle(color: AppTheme.accentError),
                  ),
                  data: (proj) {
                    if (proj == null) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 20),
                        child: Center(
                          child: Text(
                            'Projeção de desalavancagem indisponível no momento.',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                          ),
                        ),
                      );
                    }

                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Wrap(
                          spacing: 14,
                          runSpacing: 14,
                          children: [
                            _buildSacredMetricCard(
                              title: '1. Pior Mês do Caixa',
                              value: proj.worstMonthYearMonth,
                              subtitle: 'Saldo Mínimo: ${_formatCurrency(proj.worstMonthProjectedBalance)}',
                              color: proj.worstMonthProjectedBalance < 0
                                  ? AppTheme.accentError
                                  : AppTheme.accentWarning,
                            ),
                            _buildSacredMetricCard(
                              title: '2. Mês do Break-Even',
                              value: proj.breakEvenYearMonth,
                              subtitle: 'EBITDA Atual: ${_formatCurrency(proj.currentEbitda)}',
                              color: AppTheme.primaryBlue,
                            ),
                            _buildSacredMetricCard(
                              title: '3. Liberdade Financeira',
                              value: '${proj.monthsUntilFreedom} meses',
                              subtitle: 'MRR: ${_formatCurrency(proj.currentMrr)} • Churn: ${proj.monthlyChurnRatePercentage.toStringAsFixed(1)}%',
                              color: AppTheme.accentGreen,
                            ),
                          ],
                        ),
                        if (proj.timeline.isNotEmpty) ...[
                          const SizedBox(height: 20),
                          const Text(
                            'Linha do Tempo de Caixa Projetado (Primeiros Meses Críticos):',
                            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                          ),
                          const SizedBox(height: 10),
                          SingleChildScrollView(
                            scrollDirection: Axis.horizontal,
                            child: DataTable(
                              headingRowColor: WidgetStateProperty.all(AppTheme.darkBg),
                              columns: const [
                                DataColumn(label: Text('Competência')),
                                DataColumn(label: Text('MRR Projetado')),
                                DataColumn(label: Text('Custos + OPEX')),
                                DataColumn(label: Text('Parcelas Dívida')),
                                DataColumn(label: Text('Fluxo Líquido')),
                                DataColumn(label: Text('Saldo Acumulado')),
                              ],
                              rows: proj.timeline.take(12).map((pt) {
                                return DataRow(
                                  cells: [
                                    DataCell(Text(pt.yearMonth,
                                        style: const TextStyle(fontWeight: FontWeight.bold))),
                                    DataCell(Text(_formatCurrency(pt.projectedMrr))),
                                    DataCell(Text(_formatCurrency(
                                        pt.projectedDirectCosts + pt.projectedOpex))),
                                    DataCell(Text(
                                        _formatCurrency(pt.projectedDebtInstallments))),
                                    DataCell(Text(
                                      _formatCurrency(pt.projectedNetCashFlow),
                                      style: TextStyle(
                                        color: pt.projectedNetCashFlow >= 0
                                            ? AppTheme.accentGreen
                                            : AppTheme.accentError,
                                      ),
                                    )),
                                    DataCell(Text(
                                      _formatCurrency(pt.cumulativeCashBalance),
                                      style: TextStyle(
                                        fontWeight: FontWeight.bold,
                                        color: pt.isNegativeBalance
                                            ? AppTheme.accentError
                                            : AppTheme.textPrimary,
                                      ),
                                    )),
                                  ],
                                );
                              }).toList(),
                            ),
                          ),
                        ],
                      ],
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

  Widget _buildDreLine(
    String label,
    double amount, {
    bool isBold = false,
    Color? color,
  }) {
    final textStyle = TextStyle(
      fontSize: isBold ? 13.5 : 12.5,
      fontWeight: isBold ? FontWeight.bold : FontWeight.normal,
      color: color ?? AppTheme.textPrimary,
    );

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Text(
              label,
              style: textStyle,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(width: 12),
          Text(
            _formatCurrency(amount),
            style: textStyle,
          ),
        ],
      ),
    );
  }

  Widget _buildSacredMetricCard({
    required String title,
    required String value,
    required String subtitle,
    required Color color,
  }) {
    return Container(
      width: 240,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
