import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../data/dashboard_bi_model.dart';
import '../data/dashboard_bi_provider.dart';

/// Painel Administrativo & Financeiro do Provedor.
/// Apresenta o formato estruturado de cards executivos e tabela de inadimplência
/// alimentados estritamente por dados reais da API (GET /bi/metrics).
class FinancialDashboardScreen extends ConsumerWidget {
  const FinancialDashboardScreen({super.key});

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final biAsync = ref.watch(dashboardBiProvider);

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header responsivo sem overflow
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text(
                        'Gestão Financeira & Tesouraria',
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Fluxo de caixa, Contas a Pagar, Conciliação Bancária e Fechamento Fiscal',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                IconButton(
                  onPressed: () => ref.invalidate(dashboardBiProvider),
                  tooltip: 'Atualizar Indicadores',
                  icon: const Icon(Icons.refresh, color: AppTheme.primaryBlue),
                ),
                const SizedBox(width: 8),
                ElevatedButton.icon(
                  onPressed: () {
                    // Pronto para plugar fluxo de novo lançamento financeiro
                  },
                  icon: const Icon(Icons.add_circle_outline, size: 18),
                  label: const Text('Novo Lançamento'),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Estados do Provider (Carregando / Erro / Dados Reais)
            biAsync.when(
              loading: () => _buildLoadingState(),
              error: (err, stack) => _buildErrorState(ref, err.toString()),
              data: (bi) => _buildContent(context, bi),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLoadingState() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 16,
          runSpacing: 16,
          children: List.generate(4, (_) => _buildPlaceholderCard()),
        ),
        const SizedBox(height: 28),
        Card(
          child: Container(
            height: 140,
            alignment: Alignment.center,
            child: const CircularProgressIndicator(),
          ),
        ),
      ],
    );
  }

  Widget _buildErrorState(WidgetRef ref, String error) {
    return Card(
      color: AppTheme.accentError.withValues(alpha: 0.1),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: const [
                Icon(Icons.error_outline, color: AppTheme.accentError, size: 20),
                SizedBox(width: 8),
                Text(
                  'Não foi possível obter os dados da API',
                  style: TextStyle(fontWeight: FontWeight.bold, color: AppTheme.accentError),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(error, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: () => ref.invalidate(dashboardBiProvider),
              icon: const Icon(Icons.refresh, size: 16),
              label: const Text('Tentar Novamente'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, DashboardBiModel bi) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Cards com formatação visual e valores 100% reais do banco
        Wrap(
          spacing: 16,
          runSpacing: 16,
          children: [
            _buildFinanceCard(
              title: 'Entradas no Mês',
              value: _formatCurrency(bi.totalReceivedMonth),
              delta: 'Pix Xingubit Pay: ${bi.pixConversionRate.toStringAsFixed(1)}%',
              color: AppTheme.accentGreen,
              icon: Icons.arrow_downward_rounded,
            ),
            _buildFinanceCard(
              title: 'Inadimplência (Vencido)',
              value: _formatCurrency(bi.overdueAmount),
              delta: 'Taxa: ${bi.defaultRate.toStringAsFixed(2)}% da base',
              color: AppTheme.accentWarning,
              icon: Icons.warning_amber_rounded,
            ),
            _buildFinanceCard(
              title: 'Receita Recorrente (MRR)',
              value: _formatCurrency(bi.mrr),
              delta: 'Ticket Médio: ${_formatCurrency(bi.arpu)}',
              color: AppTheme.primaryBlue,
              icon: Icons.trending_up_rounded,
            ),
            _buildFinanceCard(
              title: 'Contratos Ativos',
              value: '${bi.activeContracts} Ativos',
              delta: 'Total de Clientes: ${bi.totalCustomers}',
              color: AppTheme.accentGreen,
              icon: Icons.people_outline,
            ),
          ],
        ),
        const SizedBox(height: 28),

        // Tabela de Faturas Vencidas (dados reais retornados pela API)
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20.0),
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
                            'Faturas em Atraso (Cobrança Ativa)',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Títulos pendentes de liquidação registrados no backend',
                            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                if (bi.recentOverdueInvoices.isEmpty)
                  Container(
                    padding: const EdgeInsets.symmetric(vertical: 24),
                    alignment: Alignment.center,
                    child: const Text(
                      'Nenhuma fatura em atraso registrada no momento.',
                      style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                    ),
                  )
                else
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: DataTable(
                      headingRowColor: WidgetStateProperty.all(AppTheme.darkBg),
                      columns: const [
                        DataColumn(label: Text('ID Fatura')),
                        DataColumn(label: Text('ID Contrato')),
                        DataColumn(label: Text('Vencimento')),
                        DataColumn(label: Text('Valor')),
                        DataColumn(label: Text('Status')),
                      ],
                      rows: bi.recentOverdueInvoices.map((inv) {
                        return DataRow(
                          cells: [
                            DataCell(Text(
                              inv.id.length > 8 ? '${inv.id.substring(0, 8)}...' : inv.id,
                              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                            )),
                            DataCell(Text(
                              inv.contractId.length > 8 ? '${inv.contractId.substring(0, 8)}...' : inv.contractId,
                              style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                            )),
                            DataCell(Text(inv.dueDate, style: const TextStyle(fontSize: 12))),
                            DataCell(Text(
                              _formatCurrency(inv.amount),
                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                            )),
                            DataCell(
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: AppTheme.accentWarning.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(6),
                                  border: Border.all(color: AppTheme.accentWarning, width: 1),
                                ),
                                child: const Text(
                                  'Vencido',
                                  style: TextStyle(color: AppTheme.accentWarning, fontSize: 11, fontWeight: FontWeight.bold),
                                ),
                              ),
                            ),
                          ],
                        );
                      }).toList(),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFinanceCard({
    required String title,
    required String value,
    required String delta,
    required Color color,
    required IconData icon,
  }) {
    return Container(
      width: 240,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              Icon(icon, color: color, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            value,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 6),
          Text(
            delta,
            style: TextStyle(fontSize: 12, color: color, fontWeight: FontWeight.w500),
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }

  Widget _buildPlaceholderCard() {
    return Container(
      width: 240,
      height: 100,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: const Center(
        child: SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      ),
    );
  }
}
