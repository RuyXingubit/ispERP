import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';

/// Painel Administrativo & Financeiro do Provedor.
class FinancialDashboardScreen extends StatelessWidget {
  const FinancialDashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text(
                    'Gestão Financeira & Tesouraria',
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Fluxo de caixa, Contas a Pagar, Conciliação Bancária e Fechamento Fiscal',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                  ),
                ],
              ),
              ElevatedButton.icon(
                onPressed: () {},
                icon: const Icon(Icons.add_circle_outline, size: 18),
                label: const Text('Novo Lançamento'),
              ),
            ],
          ),
          const SizedBox(height: 24),

          // Cards de Resumo Financeiro
          Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [
              _buildFinanceCard(
                title: 'Entradas Confirmadas (Hoje)',
                value: 'R\$ 18.450,00',
                delta: 'Pix Automático 84% • Boletos 16%',
                color: AppTheme.accentGreen,
                icon: Icons.arrow_downward_rounded,
              ),
              _buildFinanceCard(
                title: 'Contas a Pagar a Vencer (7 dias)',
                value: 'R\$ 32.120,00',
                delta: 'Link Dedicado + Aluguel de Postes',
                color: AppTheme.accentWarning,
                icon: Icons.arrow_upward_rounded,
              ),
              _buildFinanceCard(
                title: 'DRE Previsto do Mês',
                value: 'R\$ 142.800,00',
                delta: 'Margem Líquida Estimada: 28,6%',
                color: AppTheme.primaryBlue,
                icon: Icons.pie_chart_outline,
              ),
              _buildFinanceCard(
                title: 'Lote NFCom 62',
                value: 'Transmitido',
                delta: 'SEFAZ Autorizada sem pendências',
                color: AppTheme.accentGreen,
                icon: Icons.receipt_long_outlined,
              ),
            ],
          ),
          const SizedBox(height: 28),

          // Tabela de Contas a Pagar Recentes
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Contas a Pagar e Compromissos Iminentes',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                      ),
                      OutlinedButton.icon(
                        onPressed: () {},
                        icon: const Icon(Icons.file_download_outlined, size: 16),
                        label: const Text('Exportar OFX/Excel', style: TextStyle(fontSize: 12)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: DataTable(
                      headingRowColor: WidgetStateProperty.all(AppTheme.darkBg),
                      columns: const [
                        DataColumn(label: Text('Fornecedor / Favorecido')),
                        DataColumn(label: Text('Categoria')),
                        DataColumn(label: Text('Vencimento')),
                        DataColumn(label: Text('Valor (R\$)')),
                        DataColumn(label: Text('Status')),
                      ],
                      rows: [
                        _buildRow('Telefônica Brasil S.A. (Trânsito IP)', 'Link Dedicado', 'Hoje', 'R\$ 14.500,00', 'Pendente', AppTheme.accentWarning),
                        _buildRow('Distribuidora de Energia (Concessionária)', 'Aluguel de Postes', 'Amanhã', 'R\$ 8.240,00', 'Agendado', AppTheme.primaryBlue),
                        _buildRow('Data Center Host Interconexão', 'Hospedagem & Nuvem', '10/09', 'R\$ 3.800,00', 'Aguardando', AppTheme.textSecondary),
                        _buildRow('Distribuidora Fibras & Conectores', 'Materiais Rede Óptica', '12/09', 'R\$ 5.580,00', 'Aguardando', AppTheme.textSecondary),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
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
              Text(title, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              Icon(icon, color: color, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text(delta, style: TextStyle(fontSize: 12, color: color, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }

  DataRow _buildRow(String desc, String cat, String date, String val, String status, Color statusColor) {
    return DataRow(
      cells: [
        DataCell(Text(desc, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13))),
        DataCell(Text(cat, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary))),
        DataCell(Text(date, style: const TextStyle(fontSize: 12))),
        DataCell(Text(val, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13))),
        DataCell(
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: statusColor.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: statusColor, width: 1),
            ),
            child: Text(status, style: TextStyle(color: statusColor, fontSize: 11, fontWeight: FontWeight.bold)),
          ),
        ),
      ],
    );
  }
}
