import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/process_lifecycle_stepper.dart';
import '../data/dashboard_bi_model.dart';
import '../data/dashboard_bi_provider.dart';

/// Painel Administrativo & Financeiro do Provedor com Régua de Adimplência (Dunning / RGC Anatel).
/// Apresenta cards executivos, esteira de processos de cobrança e auditoria de faturas em atraso
/// alimentados estritamente por dados reais da API (GET /bi/metrics).
class FinancialDashboardScreen extends ConsumerStatefulWidget {
  const FinancialDashboardScreen({super.key});

  @override
  ConsumerState<FinancialDashboardScreen> createState() => _FinancialDashboardScreenState();
}

class _FinancialDashboardScreenState extends ConsumerState<FinancialDashboardScreen> {
  OverdueInvoiceItem? _selectedInvoice;
  String _dunningFilter = 'ALL'; // 'ALL', 'TOLERANCE', 'NOTICE', 'REDUCTION', 'BLOCK', 'CHURN'

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  @override
  Widget build(BuildContext context) {
    final biAsync = ref.watch(dashboardBiProvider);
    final authState = ref.watch(authProvider);
    final currentUserRole = authState.role?.name.toUpperCase();

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
                        'Fluxo de caixa, Contas a Pagar, Régua de Cobrança e Fechamento Fiscal',
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
              ],
            ),
            const SizedBox(height: 24),

            // Estados do Provider (Carregando / Erro / Dados Reais)
            biAsync.when(
              loading: () => _buildLoadingState(),
              error: (err, stack) => _buildErrorState(ref, err.toString()),
              data: (bi) => _buildContent(context, bi, currentUserRole),
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
      shape: RoundedRectangleBorder(
        side: const BorderSide(color: AppTheme.accentError),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            const Icon(Icons.error_outline, color: AppTheme.accentError, size: 40),
            const SizedBox(height: 12),
            const Text(
              'Não foi possível carregar os indicadores financeiros',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppTheme.accentError),
            ),
            const SizedBox(height: 6),
            Text(
              error,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: () => ref.invalidate(dashboardBiProvider),
              icon: const Icon(Icons.replay),
              label: const Text('Tentar Novamente'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, DashboardBiModel bi, String? currentUserRole) {
    // Auto-seleciona a primeira fatura se nenhuma selecionada ainda
    if (_selectedInvoice == null && bi.recentOverdueInvoices.isNotEmpty) {
      _selectedInvoice = bi.recentOverdueInvoices.first;
    }

    final filteredInvoices = _filterInvoices(bi.recentOverdueInvoices);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Grid de Métricas Principais
        Wrap(
          spacing: 16,
          runSpacing: 16,
          children: [
            _buildFinanceCard(
              title: 'Recebido no Mês',
              value: _formatCurrency(bi.totalReceivedMonth),
              delta: 'Conversão Pix: ${bi.pixConversionRate.toStringAsFixed(1)}%',
              color: AppTheme.accentGreen,
              icon: Icons.account_balance_wallet_rounded,
            ),
            _buildFinanceCard(
              title: 'Inadimplência Total',
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

        // Painel de Auditoria da Régua de Adimplência (ProcessLifecycleStepper)
        if (_selectedInvoice != null) ...[
          Container(
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Barra de Ação da Fatura Selecionada
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Row(
                        children: [
                          const Icon(Icons.receipt_long_rounded, color: AppTheme.primaryBlue, size: 22),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Auditoria de Régua: Fatura ${_selectedInvoice!.id.length > 8 ? _selectedInvoice!.id.substring(0, 8) : _selectedInvoice!.id}',
                                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
                                  overflow: TextOverflow.ellipsis,
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  'Contrato: ${_selectedInvoice!.contractId.length > 8 ? _selectedInvoice!.contractId.substring(0, 8) : _selectedInvoice!.contractId} • Vencimento: ${_selectedInvoice!.dueDate} • Valor: ${_formatCurrency(_selectedInvoice!.amount)}',
                                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: _getDunningColor(_selectedInvoice!.daysOverdue).withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: _getDunningColor(_selectedInvoice!.daysOverdue).withValues(alpha: 0.4)),
                      ),
                      child: Text(
                        '${_selectedInvoice!.daysOverdue} dias de atraso (${_selectedInvoice!.dunningStageLabel})',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: _getDunningColor(_selectedInvoice!.daysOverdue),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton(
                      icon: const Icon(Icons.close, size: 18, color: AppTheme.textMuted),
                      tooltip: 'Ocultar Auditoria da Régua',
                      onPressed: () {
                        setState(() {
                          _selectedInvoice = null;
                        });
                      },
                    ),
                  ],
                ),
                const SizedBox(height: 14),

                // Componente Reutilizável de Governança e Processo
                ProcessLifecycleStepper(
                  processTitle: 'Régua de Adimplência & Cobrança Legal (Dunning / RGC Anatel)',
                  steps: _buildDunningSteps(_selectedInvoice!),
                  currentUserRole: currentUserRole,
                ),
                const SizedBox(height: 14),

                // Ações Operacionais da Cobrança
                Wrap(
                  spacing: 10,
                  runSpacing: 8,
                  alignment: WrapAlignment.end,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () {
                        Clipboard.setData(ClipboardData(text: _selectedInvoice!.id));
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('ID da Fatura copiado para a área de transferência!'),
                            duration: Duration(seconds: 2),
                          ),
                        );
                      },
                      icon: const Icon(Icons.copy_rounded, size: 16),
                      label: const Text('Copiar ID da Fatura'),
                    ),
                    if (currentUserRole == 'ADMIN' || currentUserRole == 'FINANCIAL') ...[
                      ElevatedButton.icon(
                        onPressed: () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                              backgroundColor: AppTheme.accentGreen,
                              content: Text('Desbloqueio em Confiança (48h) ativado para o contrato ${_selectedInvoice!.contractId.length > 8 ? _selectedInvoice!.contractId.substring(0, 8) : _selectedInvoice!.contractId}!'),
                              duration: const Duration(seconds: 3),
                            ),
                          );
                        },
                        style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentGreen),
                        icon: const Icon(Icons.lock_open_rounded, size: 16, color: Color(0xFF0A0F1D)),
                        label: const Text('Desbloqueio em Confiança (48h)', style: TextStyle(color: Color(0xFF0A0F1D), fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
        ],

        // Tabela de Faturas Vencidas com Filtros da Régua
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
                            'Faturas em Atraso & Régua de Adimplência (Cobrança Ativa)',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Selecione uma fatura para inspecionar seu estágio na régua de cobrança legal',
                            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),

                // Filtros rápidos por estágio da régua de cobrança
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _buildDunningFilterChip('Todas as Vencidas', 'ALL', bi.recentOverdueInvoices.length),
                    _buildDunningFilterChip(
                      'Tolerância (1-5d)',
                      'TOLERANCE',
                      bi.recentOverdueInvoices.where((i) => i.daysOverdue <= 5).length,
                      color: AppTheme.primaryBlue,
                    ),
                    _buildDunningFilterChip(
                      'Notificação (6-14d)',
                      'NOTICE',
                      bi.recentOverdueInvoices.where((i) => i.daysOverdue >= 6 && i.daysOverdue <= 14).length,
                      color: AppTheme.accentWarning,
                    ),
                    _buildDunningFilterChip(
                      'Redução QoS (15-29d)',
                      'REDUCTION',
                      bi.recentOverdueInvoices.where((i) => i.daysOverdue >= 15 && i.daysOverdue <= 29).length,
                      color: Colors.orangeAccent,
                    ),
                    _buildDunningFilterChip(
                      'Bloqueio Total (30-59d)',
                      'BLOCK',
                      bi.recentOverdueInvoices.where((i) => i.daysOverdue >= 30 && i.daysOverdue <= 59).length,
                      color: AppTheme.accentError,
                    ),
                    _buildDunningFilterChip(
                      'Rescisão / Serasa (60d+)',
                      'CHURN',
                      bi.recentOverdueInvoices.where((i) => i.daysOverdue >= 60).length,
                      color: Colors.red[900],
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                if (filteredInvoices.isEmpty)
                  Container(
                    padding: const EdgeInsets.symmetric(vertical: 28),
                    alignment: Alignment.center,
                    child: const Text(
                      'Nenhuma fatura encontrada para o filtro de cobrança selecionado.',
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
                        DataColumn(label: Text('Atraso')),
                        DataColumn(label: Text('Estágio da Régua')),
                        DataColumn(label: Text('Ação')),
                      ],
                      rows: filteredInvoices.map((inv) {
                        final isSelected = _selectedInvoice?.id == inv.id;
                        final days = inv.daysOverdue;
                        final stageColor = _getDunningColor(days);

                        return DataRow(
                          selected: isSelected,
                          onSelectChanged: (_) {
                            setState(() {
                              _selectedInvoice = inv;
                            });
                          },
                          color: isSelected
                              ? WidgetStateProperty.all(AppTheme.primaryBlue.withValues(alpha: 0.12))
                              : null,
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
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: stageColor.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  '$days dias',
                                  style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: stageColor),
                                ),
                              ),
                            ),
                            DataCell(
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: stageColor.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(6),
                                  border: Border.all(color: stageColor, width: 1),
                                ),
                                child: Text(
                                  inv.dunningStageLabel,
                                  style: TextStyle(color: stageColor, fontSize: 11, fontWeight: FontWeight.bold),
                                ),
                              ),
                            ),
                            DataCell(
                              InkWell(
                                onTap: () {
                                  setState(() {
                                    _selectedInvoice = inv;
                                  });
                                },
                                borderRadius: BorderRadius.circular(6),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: isSelected ? AppTheme.primaryBlue : AppTheme.darkCard,
                                    borderRadius: BorderRadius.circular(6),
                                    border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.4)),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(Icons.alt_route_rounded, size: 14, color: isSelected ? const Color(0xFF0A0F1D) : AppTheme.primaryBlue),
                                      const SizedBox(width: 4),
                                      Text(
                                        'Auditar Régua',
                                        style: TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.bold,
                                          color: isSelected ? const Color(0xFF0A0F1D) : AppTheme.primaryBlue,
                                        ),
                                      ),
                                    ],
                                  ),
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

  Widget _buildDunningFilterChip(String label, String value, int count, {Color? color}) {
    final isSelected = _dunningFilter == value;
    final activeColor = color ?? AppTheme.primaryBlue;

    return ChoiceChip(
      label: Text(
        '$label ($count)',
        style: TextStyle(
          fontSize: 12,
          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
          color: isSelected ? const Color(0xFF0A0F1D) : AppTheme.textSecondary,
        ),
      ),
      selected: isSelected,
      selectedColor: activeColor,
      backgroundColor: AppTheme.darkSurface,
      side: BorderSide(color: isSelected ? activeColor : AppTheme.darkBorder),
      onSelected: (_) {
        setState(() {
          _dunningFilter = value;
        });
      },
    );
  }

  List<OverdueInvoiceItem> _filterInvoices(List<OverdueInvoiceItem> list) {
    switch (_dunningFilter) {
      case 'TOLERANCE':
        return list.where((i) => i.daysOverdue <= 5).toList();
      case 'NOTICE':
        return list.where((i) => i.daysOverdue >= 6 && i.daysOverdue <= 14).toList();
      case 'REDUCTION':
        return list.where((i) => i.daysOverdue >= 15 && i.daysOverdue <= 29).toList();
      case 'BLOCK':
        return list.where((i) => i.daysOverdue >= 30 && i.daysOverdue <= 59).toList();
      case 'CHURN':
        return list.where((i) => i.daysOverdue >= 60).toList();
      case 'ALL':
      default:
        return list;
    }
  }

  Color _getDunningColor(int days) {
    if (days <= 5) return AppTheme.primaryBlue;
    if (days <= 14) return AppTheme.accentWarning;
    if (days <= 29) return Colors.orangeAccent;
    if (days <= 59) return AppTheme.accentError;
    return Colors.red[800] ?? Colors.red;
  }

  List<ProcessLifecycleStep> _buildDunningSteps(OverdueInvoiceItem invoice) {
    final days = invoice.daysOverdue;

    return [
      ProcessLifecycleStep(
        id: 'step_dun_preventive',
        title: '1. Notificação Preventiva (D-3)',
        subtitle: 'Pix Copia e Cola Enviado',
        responsibleRoleName: 'Sistema Automatizado',
        allowedRoles: const ['FINANCIAL', 'ADMIN'],
        isCompleted: true,
        isActive: false,
        icon: Icons.mark_email_read_rounded,
        popGuideTitle: 'POP-DUN-01: Notificação Preventiva e Envio de Pix',
        popGuideContent:
            '1. Objetivo: Alertar o assinante 3 dias e 1 dia antes do vencimento com chave Pix Copia e Cola.\n\n'
            '2. Canais de Disparo: Mensagem de WhatsApp com QR Code Pix dinâmico e e-mail com fatura em anexo.\n\n'
            '3. Impacto: Reduz o esquecimento involuntário em até 42% antes da incidência de juros.',
      ),
      ProcessLifecycleStep(
        id: 'step_dun_tolerance',
        title: '2. Tolerância (D0 a D+5)',
        subtitle: days <= 5 ? 'Em Tolerância ($days d atraso)' : 'Tolerância Expirada',
        responsibleRoleName: 'Sistema Automatizado',
        allowedRoles: const ['FINANCIAL', 'ADMIN'],
        isCompleted: days > 5,
        isActive: days >= 1 && days <= 5,
        icon: Icons.event_available_rounded,
        popGuideTitle: 'POP-DUN-02: Gestão de Tolerância e Cortesia Comercial',
        popGuideContent:
            '1. Objetivo: Conceder janela amigável de liquidação sem restrição de tráfego ou penalidades severas.\n\n'
            '2. Comunicação: Lembrete cordial de fatura vencida no D+2 e D+4 via WhatsApp.\n\n'
            '3. Governança: Proibido efetuar redução de velocidade ou corte de conexão neste estágio.',
      ),
      ProcessLifecycleStep(
        id: 'step_dun_notice',
        title: '3. Notificação Formal (D+10)',
        subtitle: days <= 14 ? (days >= 6 ? 'Notificação Emitida ($days d)' : 'Aguardando Prazo') : 'Notificado Formalmente',
        responsibleRoleName: 'Setor de Cobrança',
        allowedRoles: const ['FINANCIAL', 'ADMIN'],
        isCompleted: days > 14,
        isActive: days >= 6 && days <= 14,
        icon: Icons.warning_amber_rounded,
        popGuideTitle: 'POP-DUN-03: Notificação Formal com Alerta Legal de Redução',
        popGuideContent:
            '1. Requisito Regulatório Anatel (RGC): O assinante deve ser notificado formalmente com no mínimo 5 dias de antecedência antes da redução de velocidade.\n\n'
            '2. Notificação: Envio de alerta oficial registrando que no D+15 a conexão sofrerá degradação de banda para 256 kbps.\n\n'
            '3. Ação do Operador: Contato telefônico ou mensagem ativa oferecendo promessa de pagamento (desbloqueio em confiança de 48h).',
      ),
      ProcessLifecycleStep(
        id: 'step_dun_reduction',
        title: '4. Redução de Banda (D+15)',
        subtitle: days <= 29 ? (days >= 15 ? 'Velocidade Reduzida ($days d)' : 'Aguardando Prazo') : 'Executado',
        responsibleRoleName: 'NOC / FreeRADIUS',
        allowedRoles: const ['SUPPORT_N2', 'FINANCIAL', 'ADMIN'],
        isCompleted: days > 29,
        isActive: days >= 15 && days <= 29,
        icon: Icons.speed_rounded,
        popGuideTitle: 'POP-DUN-04: Aplicação de Perfil de Redução no RADIUS (CoA)',
        popGuideContent:
            '1. Procedimento Técnico: Envio de pacote CoA Disconnect para o NAS/Concentrador PPPoE/IPoE.\n\n'
            '2. Perfil Aplicado: Grupo de velocidade reduzida (ex: 256 kbps down / 128 kbps up) com redirecionamento de DNS para o portal de pagamento.\n\n'
            '3. Exceção: Serviços essenciais (governo, emergência) mantêm acesso nos termos da legislação.',
      ),
      ProcessLifecycleStep(
        id: 'step_dun_block',
        title: '5. Bloqueio Total (D+30)',
        subtitle: days <= 59 ? (days >= 30 ? 'Suspensão Total ($days d)' : 'Aguardando Prazo') : 'Executado',
        responsibleRoleName: 'Sistema / Financeiro',
        allowedRoles: const ['FINANCIAL', 'ADMIN'],
        isCompleted: days >= 60,
        isActive: days >= 30 && days <= 59,
        icon: Icons.block_flipped,
        popGuideTitle: 'POP-DUN-05: Suspensão Total da Prestação do Serviço',
        popGuideContent:
            '1. Requisito Legal: Decorridos 30 dias da notificação sem quitação, a prestação do serviço é totalmente suspensa.\n\n'
            '2. Estado Contratual: Contrato entra no status SUSPENDED no ERP, cessando faturamento proporcional.\n\n'
            '3. Restauração Automática: Ao identificar o pagamento via Pix no Xingubit Pay, o sistema dispara imediatamente o desbloqueio no RADIUS em menos de 60 segundos.',
      ),
      ProcessLifecycleStep(
        id: 'step_dun_churn',
        title: '6. Desconexão & Serasa (D+60)',
        subtitle: days >= 60 ? 'Rescisão Contratual ($days d)' : 'Aguardando Prazo',
        responsibleRoleName: 'Financeiro / Jurídico',
        allowedRoles: const ['FINANCIAL', 'ADMIN'],
        isCompleted: false,
        isActive: days >= 60,
        icon: Icons.gavel_rounded,
        popGuideTitle: 'POP-DUN-06: Rescisão Contratual Definitiva e Negativação',
        popGuideContent:
            '1. Rescisão Definitiva: Após 60 dias de suspensão, o contrato é cancelado por inadimplência irrevogável.\n\n'
            '2. Negativação: Envio automático da dívida para os órgãos de proteção ao crédito (Serasa/SPC).\n\n'
            '3. Logística Reversa: Emissão automática da Ordem de Serviço de Retirada/Recolhimento dos equipamentos em comodato (ONU e Roteador Wi-Fi).',
      ),
    ];
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
