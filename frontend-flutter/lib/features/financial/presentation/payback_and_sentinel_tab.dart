import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../data/strategic_finance_repository.dart';

/// Aba de Payback de Expansão FTTH (ROI por CTO/Bairro) e Auditoria Pericial Sentinela (Onda C - Etapa 2).
class PaybackAndSentinelTab extends ConsumerStatefulWidget {
  const PaybackAndSentinelTab({super.key});

  @override
  ConsumerState<PaybackAndSentinelTab> createState() => _PaybackAndSentinelTabState();
}

class _PaybackAndSentinelTabState extends ConsumerState<PaybackAndSentinelTab> {
  bool _isSweeping = false;

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  void _refreshAll() {
    ref.invalidate(networkProjectsPaybackProvider);
    ref.invalidate(activeSentinelAlertsProvider);
  }

  Future<void> _showCreateProjectDialog(BuildContext context) async {
    final nameCtrl = TextEditingController();
    final neighborhoodCtrl = TextEditingController();
    final cityCtrl = TextEditingController(text: 'Santarém');
    final budgetCtrl = TextEditingController(text: '65000.00');
    final targetSubsCtrl = TextEditingController(text: '250');
    final startDateCtrl = TextEditingController(text: '2026-10-15');

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Novo Projeto de Expansão FTTH'),
        content: SizedBox(
          width: 440,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Nome do Projeto (ex: Expansão Bairro Esperança)',
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: neighborhoodCtrl,
                        decoration: const InputDecoration(labelText: 'Bairro / Polígono'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: cityCtrl,
                        decoration: const InputDecoration(labelText: 'Cidade'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: budgetCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(
                          labelText: 'Orçamento CAPEX (R\$)',
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: targetSubsCtrl,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(
                          labelText: 'Meta de Assinantes',
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: startDateCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Data de Início (YYYY-MM-DD)',
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
              final name = nameCtrl.text.trim();
              final neighborhood = neighborhoodCtrl.text.trim();
              final city = cityCtrl.text.trim();
              final budget = double.tryParse(budgetCtrl.text.replaceAll(',', '.')) ?? 0.0;
              final subs = int.tryParse(targetSubsCtrl.text.trim()) ?? 0;
              final startDate = startDateCtrl.text.trim();
              if (name.isEmpty || neighborhood.isEmpty || city.isEmpty || budget <= 0) return;

              try {
                final ok = await ref
                    .read(strategicFinanceRepositoryProvider)
                    .createNetworkProject(
                      name: name,
                      neighborhood: neighborhood,
                      city: city,
                      budgetAmount: budget,
                      targetSubscribers: subs,
                      startDate: startDate,
                    );
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (ok) {
                  _refreshAll();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Projeto FTTH cadastrado com sucesso!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Erro ao criar projeto FTTH: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Salvar Projeto'),
          ),
        ],
      ),
    );
  }

  Future<void> _showAssignCtoDialog(BuildContext context, {String? initialProjectId}) async {
    final projectCtrl = TextEditingController(text: initialProjectId ?? '');
    final ctoCtrl = TextEditingController();

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Vincular CTO ao Projeto Financeiro'),
        content: SizedBox(
          width: 420,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: projectCtrl,
                decoration: const InputDecoration(
                  labelText: 'UUID do Projeto FTTH (projectId)',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: ctoCtrl,
                decoration: const InputDecoration(
                  labelText: 'UUID da Caixa CTO (ctoId)',
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
              final pid = projectCtrl.text.trim();
              final cid = ctoCtrl.text.trim();
              if (pid.isEmpty || cid.isEmpty) return;

              try {
                final ok = await ref
                    .read(strategicFinanceRepositoryProvider)
                    .assignCtoToProject(projectId: pid, ctoId: cid);
                if (!context.mounted) return;
                Navigator.of(dialogCtx).pop();
                if (ok) {
                  _refreshAll();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('CTO vinculada ao projeto de expansão FTTH!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Erro ao vincular CTO: $e'),
                    backgroundColor: AppTheme.accentError,
                  ),
                );
              }
            },
            child: const Text('Vincular CTO'),
          ),
        ],
      ),
    );
  }

  Color _getSeverityColor(String severity) {
    switch (severity.toUpperCase()) {
      case 'CRITICAL':
      case 'HIGH':
        return AppTheme.accentError;
      case 'MEDIUM':
        return AppTheme.accentWarning;
      default:
        return AppTheme.primaryBlue;
    }
  }

  @override
  Widget build(BuildContext context) {
    final projectsAsync = ref.watch(networkProjectsPaybackProvider);
    final alertsAsync = ref.watch(activeSentinelAlertsProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Barra de Ações Estratégicas
        Wrap(
          spacing: 10,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            ElevatedButton.icon(
              onPressed: () => _showCreateProjectDialog(context),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
              ),
              icon: const Icon(Icons.hub_outlined, size: 16),
              label: const Text('Novo Projeto FTTH'),
            ),
            OutlinedButton.icon(
              onPressed: () => _showAssignCtoDialog(context),
              icon: const Icon(Icons.settings_input_component_outlined, size: 16),
              label: const Text('Vincular CTO ao Projeto'),
            ),
            ElevatedButton.icon(
              onPressed: _isSweeping
                  ? null
                  : () async {
                      setState(() => _isSweeping = true);
                      try {
                        final found = await ref
                            .read(strategicFinanceRepositoryProvider)
                            .triggerSentinelSweep();
                        if (!context.mounted) return;
                        _refreshAll();
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              'Varredura Pericial Sentinela concluída! ${found.length} alerta(s) ativo(s).',
                            ),
                            backgroundColor: AppTheme.accentGreen,
                          ),
                        );
                      } catch (e) {
                        if (!context.mounted) return;
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text('Falha ao executar varredura: $e'),
                            backgroundColor: AppTheme.accentError,
                          ),
                        );
                      } finally {
                        if (mounted) {
                          setState(() => _isSweeping = false);
                        }
                      }
                    },
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentWarning),
              icon: const Icon(Icons.radar_rounded, size: 16, color: Color(0xFF0A0F1D)),
              label: Text(
                _isSweeping ? 'Executando Varredura...' : 'Executar Varredura Sentinela',
                style: const TextStyle(color: Color(0xFF0A0F1D), fontWeight: FontWeight.bold),
              ),
            ),
            IconButton(
              onPressed: _refreshAll,
              tooltip: 'Atualizar Projetos FTTH e Alertas Sentinela',
              icon: const Icon(Icons.refresh, color: AppTheme.primaryBlue),
            ),
          ],
        ),
        const SizedBox(height: 20),

        // Seção 1: Payback de Projetos de Expansão FTTH
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Payback & ROI de Projetos de Expansão FTTH por CTO',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Cruzamento em tempo real do CAPEX alocado na construção com a receita recorrente das portas ocupadas nas CTOs do projeto',
                  style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 16),
                projectsAsync.when(
                  loading: () => const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (err, _) => Text(
                    'Erro ao carregar payback de projetos FTTH: $err',
                    style: const TextStyle(color: AppTheme.accentError),
                  ),
                  data: (projects) {
                    if (projects.isEmpty) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 20),
                        child: Center(
                          child: Text(
                            'Nenhum projeto de expansão FTTH cadastrado.',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                          ),
                        ),
                      );
                    }

                    return Column(
                      children: projects
                          .map(
                            (p) => Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: AppTheme.darkBg,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(
                                  color: p.isPaybackAchieved
                                      ? AppTheme.accentGreen.withValues(alpha: 0.5)
                                      : AppTheme.darkBorder,
                                ),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(
                                        p.isPaybackAchieved
                                            ? Icons.emoji_events_outlined
                                            : Icons.account_tree_outlined,
                                        color: p.isPaybackAchieved
                                            ? AppTheme.accentGreen
                                            : AppTheme.primaryBlue,
                                        size: 22,
                                      ),
                                      const SizedBox(width: 10),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              '${p.name} (${p.neighborhood} - ${p.city})',
                                              style: const TextStyle(
                                                fontWeight: FontWeight.bold,
                                                fontSize: 14,
                                              ),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                            Text(
                                              'CTOs: ${p.ctoCount} • Portas: ${p.occupiedPorts}/${p.totalPorts} (${p.penetrationRate.toStringAsFixed(1)}%) • Prioridade Comercial: ${p.commercialPriority}',
                                              style: const TextStyle(
                                                color: AppTheme.textSecondary,
                                                fontSize: 11,
                                              ),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ),
                                      ),
                                      OutlinedButton.icon(
                                        onPressed: () => _showAssignCtoDialog(
                                          context,
                                          initialProjectId: p.projectId,
                                        ),
                                        icon: const Icon(Icons.add_link_rounded, size: 15),
                                        label: const Text('Vincular CTO', style: TextStyle(fontSize: 11)),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 10),
                                  Wrap(
                                    spacing: 16,
                                    runSpacing: 6,
                                    children: [
                                      Text(
                                        'CAPEX Orçado: ${_formatCurrency(p.budgetAmount)}',
                                        style: const TextStyle(fontSize: 12),
                                      ),
                                      Text(
                                        'MRR Gerado: ${_formatCurrency(p.monthlyRevenue)}',
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.accentGreen,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                      Text(
                                        'Receita Acumulada: ${_formatCurrency(p.accumulatedRevenue)}',
                                        style: const TextStyle(fontSize: 12),
                                      ),
                                      Text(
                                        p.isPaybackAchieved
                                            ? 'Status: PAYBACK ATINGIDO'
                                            : 'Payback Estimado: ${p.paybackMonthsEstimated} meses',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color: p.isPaybackAchieved
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
        ),
        const SizedBox(height: 20),

        // Seção 2: Auditoria Pericial Sentinela
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Auditoria Pericial Sentinela (Detecção de Vazamento de Receita & Fraudes)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Inspeção contínua de ONUs órfãs sem contrato, dinheiro retido sem depósito, isenções suspeitas e divergências RADIUS vs Faturamento',
                  style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 16),
                alertsAsync.when(
                  loading: () => const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (err, _) => Text(
                    'Erro ao carregar alertas do Sentinela: $err',
                    style: const TextStyle(color: AppTheme.accentError),
                  ),
                  data: (alerts) {
                    if (alerts.isEmpty) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 20),
                        child: Center(
                          child: Text(
                            'Nenhuma anomalia ou vazamento de receita pendente de resolução no Sentinela.',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                          ),
                        ),
                      );
                    }

                    return Column(
                      children: alerts
                          .map(
                            (a) => Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: AppTheme.darkBg,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(
                                  color: _getSeverityColor(a.severity).withValues(alpha: 0.5),
                                ),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(
                                        Icons.gpp_maybe_rounded,
                                        color: _getSeverityColor(a.severity),
                                        size: 22,
                                      ),
                                      const SizedBox(width: 10),
                                      Expanded(
                                        child: Text(
                                          '[${a.severity}] ${a.title}',
                                          style: const TextStyle(
                                            fontWeight: FontWeight.bold,
                                            fontSize: 13.5,
                                          ),
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                      const SizedBox(width: 8),
                                      ElevatedButton.icon(
                                        onPressed: () async {
                                          final res = await ref
                                              .read(strategicFinanceRepositoryProvider)
                                              .resolveSentinelAlert(a.id);
                                          if (!context.mounted) return;
                                          if (res != null) {
                                            _refreshAll();
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(
                                                content: Text('Alerta pericial auditado e arquivado!'),
                                                backgroundColor: AppTheme.accentGreen,
                                              ),
                                            );
                                          }
                                        },
                                        style: ElevatedButton.styleFrom(
                                          backgroundColor: AppTheme.accentGreen,
                                        ),
                                        icon: const Icon(
                                          Icons.task_alt_rounded,
                                          size: 16,
                                          color: Color(0xFF0A0F1D),
                                        ),
                                        label: const Text(
                                          'Resolver Alerta',
                                          style: TextStyle(
                                            color: Color(0xFF0A0F1D),
                                            fontWeight: FontWeight.bold,
                                            fontSize: 12,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 6),
                                  Text(
                                    a.description,
                                    style: const TextStyle(fontSize: 12),
                                  ),
                                  if (a.geminiAnalysis.isNotEmpty) ...[
                                    const SizedBox(height: 6),
                                    Text(
                                      'Diagnóstico Pericial: ${a.geminiAnalysis}',
                                      style: const TextStyle(
                                        fontSize: 11.5,
                                        color: AppTheme.textSecondary,
                                      ),
                                    ),
                                  ],
                                  if (a.recommendedAction.isNotEmpty) ...[
                                    const SizedBox(height: 4),
                                    Text(
                                      'Ação Recomendada: ${a.recommendedAction}',
                                      style: const TextStyle(
                                        fontSize: 11.5,
                                        color: AppTheme.primaryBlue,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ],
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
