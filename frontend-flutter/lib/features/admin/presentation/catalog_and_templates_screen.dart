import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../../sales/data/sales_onboarding_notifier.dart';
import '../data/catalog_repository.dart';

/// Tela unificada de Catálogo de Planos (`/plans`) e Templates de Contrato (`/contracts/templates`).
class CatalogAndTemplatesScreen extends ConsumerStatefulWidget {
  final int initialTabIndex;

  const CatalogAndTemplatesScreen({super.key, this.initialTabIndex = 0});

  @override
  ConsumerState<CatalogAndTemplatesScreen> createState() =>
      _CatalogAndTemplatesScreenState();
}

class _CatalogAndTemplatesScreenState
    extends ConsumerState<CatalogAndTemplatesScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  String _planFilter = 'ALL'; // ALL, ACTIVE, INACTIVE

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTabIndex.clamp(0, 1),
    );
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  String _formatCurrency(double val) {
    return NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$').format(val);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
            child: Wrap(
              spacing: 16,
              runSpacing: 12,
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'Catálogo de Planos & Minutas Contratuais',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Gestão do portfólio de velocidades SCM/SVA e templates dinâmicos de assinatura eletrônica',
                      style: TextStyle(
                        color: AppTheme.textSecondary,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
                Wrap(
                  spacing: 10,
                  runSpacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () {
                        ref.invalidate(adminPlansListProvider);
                        ref.invalidate(contractTemplatesListProvider);
                        ref.invalidate(contractTemplateVariablesProvider);
                      },
                      icon: const Icon(Icons.refresh, size: 18),
                      label: const Text('Atualizar'),
                    ),
                    ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryBlue,
                        foregroundColor: Colors.white,
                      ),
                      onPressed: () => _showPlanDialog(),
                      icon: const Icon(Icons.speed_outlined, size: 18),
                      label: const Text('Novo Plano'),
                    ),
                    ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryIndigo,
                        foregroundColor: Colors.white,
                      ),
                      onPressed: () => _showTemplateDialog(),
                      icon: const Icon(Icons.description_outlined, size: 18),
                      label: const Text('Nova Minuta'),
                    ),
                  ],
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            decoration: const BoxDecoration(
              border: Border(bottom: BorderSide(color: AppTheme.darkCard)),
            ),
            child: TabBar(
              controller: _tabController,
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              indicatorColor: AppTheme.primaryBlue,
              labelColor: AppTheme.primaryBlue,
              unselectedLabelColor: AppTheme.textSecondary,
              tabs: const [
                Tab(
                  icon: Icon(Icons.speed_outlined, size: 18),
                  text: 'Planos de Internet (/plans)',
                ),
                Tab(
                  icon: Icon(Icons.description_outlined, size: 18),
                  text: 'Templates de Contrato (/contracts/templates)',
                ),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildPlansTab(),
                _buildTemplatesTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ===========================================================================
  // ABA 1: PLANOS DE INTERNET (/plans)
  // ===========================================================================

  Widget _buildPlansTab() {
    final plansAsync = ref.watch(adminPlansListProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ChoiceChip(
                label: const Text('Todos'),
                selected: _planFilter == 'ALL',
                onSelected: (_) => setState(() => _planFilter = 'ALL'),
              ),
              ChoiceChip(
                label: const Text('Ativos'),
                selected: _planFilter == 'ACTIVE',
                onSelected: (_) => setState(() => _planFilter = 'ACTIVE'),
              ),
              ChoiceChip(
                label: const Text('Inativos'),
                selected: _planFilter == 'INACTIVE',
                onSelected: (_) => setState(() => _planFilter = 'INACTIVE'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          plansAsync.when(
            loading: () => const Center(
              child: Padding(
                padding: EdgeInsets.all(40.0),
                child: CircularProgressIndicator(),
              ),
            ),
            error: (err, _) => _buildErrorCard(
              'Erro ao carregar catálogo de planos: $err',
              () => ref.invalidate(adminPlansListProvider),
            ),
            data: (plans) {
              final filtered = plans.where((p) {
                if (_planFilter == 'ACTIVE') return p.active;
                if (_planFilter == 'INACTIVE') return !p.active;
                return true;
              }).toList();

              if (filtered.isEmpty) {
                return _buildEmptyCard(
                  icon: Icons.speed_outlined,
                  title: 'Nenhum plano encontrado para o filtro selecionado',
                  subtitle:
                      'Cadastre um novo plano de acesso para disponibilizá-lo imediatamente no fluxo comercial e na vitrine.',
                );
              }

              return Wrap(
                spacing: 16,
                runSpacing: 16,
                children: filtered.map(_buildPlanCard).toList(),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildPlanCard(AdminPlanModel plan) {
    return Container(
      width: 340,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: plan.active
              ? AppTheme.primaryBlue.withValues(alpha: 0.35)
              : AppTheme.darkCard,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  plan.name,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: (plan.active
                          ? AppTheme.accentGreen
                          : AppTheme.textMuted)
                      .withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  plan.active ? 'ATIVO' : 'INATIVO',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: plan.active
                        ? AppTheme.accentGreen
                        : AppTheme.textMuted,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            '${_formatCurrency(plan.price)} / mês',
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppTheme.accentGreen,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 12,
            runSpacing: 6,
            children: [
              _buildMetricPill(
                Icons.download_rounded,
                '${plan.downloadSpeed} Mbps Down',
                AppTheme.primaryBlue,
              ),
              _buildMetricPill(
                Icons.upload_rounded,
                '${plan.uploadSpeed} Mbps Up',
                AppTheme.primaryIndigo,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 6,
            children: [
              _buildMetricPill(
                Icons.receipt_long_outlined,
                'SCM (NFCom): ${_formatCurrency(plan.scmAmount > 0 ? plan.scmAmount : plan.price)}',
                AppTheme.accentGreen,
              ),
              if (plan.svaAmount > 0)
                _buildMetricPill(
                  Icons.layers_outlined,
                  'SVA/Locação: ${_formatCurrency(plan.svaAmount)}',
                  AppTheme.accentWarning,
                ),
            ],
          ),
          if (plan.compositionItems.length > 1) ...[
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 4,
              children: plan.compositionItems
                  .map(
                    (item) => Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 3,
                      ),
                      decoration: BoxDecoration(
                        color: AppTheme.darkBg,
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: AppTheme.darkCard),
                      ),
                      child: Text(
                        '${item.name} (${item.category}): ${_formatCurrency(item.amount)}',
                        style: const TextStyle(
                          fontSize: 10,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ],
          if (plan.svaIncluded != null && plan.svaIncluded!.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              'SVA Incluso: ${plan.svaIncluded}',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textSecondary,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
          if (plan.description != null && plan.description!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              plan.description!,
              style: const TextStyle(fontSize: 12, color: AppTheme.textMuted),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
          const SizedBox(height: 10),
          Text(
            'Tolerância corte: ${plan.suspensionDays} dias • NFCom auto: ${plan.alwaysIssueNfcom ? "Sim" : "Não"}',
            style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
            overflow: TextOverflow.ellipsis,
          ),
          const Divider(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton.icon(
                onPressed: () => _showPlanDialog(existing: plan),
                icon: const Icon(Icons.edit_outlined, size: 16),
                label: const Text('Editar'),
              ),
              const SizedBox(width: 8),
              TextButton.icon(
                style: TextButton.styleFrom(
                  foregroundColor: AppTheme.accentError,
                ),
                onPressed: () => _confirmDeletePlan(plan),
                icon: const Icon(Icons.delete_outline, size: 16),
                label: const Text('Remover'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMetricPill(IconData icon, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: color,
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showPlanDialog({AdminPlanModel? existing}) async {
    final formKey = GlobalKey<FormState>();
    final nameCtrl = TextEditingController(text: existing?.name ?? '');
    final downCtrl = TextEditingController(
      text: existing != null ? '${existing.downloadSpeed}' : '500',
    );
    final upCtrl = TextEditingController(
      text: existing != null ? '${existing.uploadSpeed}' : '250',
    );
    final priceCtrl = TextEditingController(
      text: existing != null ? existing.price.toStringAsFixed(2) : '99.90',
    );
    final descCtrl = TextEditingController(text: existing?.description ?? '');
    final svaCtrl = TextEditingController(text: existing?.svaIncluded ?? '');
    final suspCtrl = TextEditingController(
      text: existing != null ? '${existing.suspensionDays}' : '5',
    );
    bool alwaysIssueNfcom = existing?.alwaysIssueNfcom ?? false;
    bool active = existing?.active ?? true;
    bool isSaving = false;

    final List<Map<String, dynamic>> editableItems =
        (existing != null && existing.compositionItems.isNotEmpty)
        ? existing.compositionItems
              .map(
                (i) => {
                  'id': i.id,
                  'nameCtrl': TextEditingController(text: i.name),
                  'category': i.category,
                  'amountCtrl': TextEditingController(
                    text: i.amount.toStringAsFixed(2),
                  ),
                  'fiscalCodeCtrl': TextEditingController(
                    text: i.fiscalCode ?? '',
                  ),
                },
              )
              .toList()
        : [
            {
              'id': null,
              'nameCtrl': TextEditingController(
                text: existing != null
                    ? 'Acesso SCM — ${existing.name}'
                    : 'Acesso SCM Fibra Óptica',
              ),
              'category': 'SCM',
              'amountCtrl': TextEditingController(
                text: existing != null
                    ? existing.price.toStringAsFixed(2)
                    : '99.90',
              ),
              'fiscalCodeCtrl': TextEditingController(text: '5307'),
            },
          ];

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            final planTotal =
                double.tryParse(priceCtrl.text.trim().replaceAll(',', '.')) ??
                0.0;
            double scmSum = 0.0;
            double svaSum = 0.0;
            for (final row in editableItems) {
              final amt =
                  double.tryParse(
                    (row['amountCtrl'] as TextEditingController).text
                        .trim()
                        .replaceAll(',', '.'),
                  ) ??
                  0.0;
              if ((row['category'] as String) == 'SCM') {
                scmSum += amt;
              } else {
                svaSum += amt;
              }
            }
            final itemsSum = scmSum + svaSum;
            final sumMatches = (itemsSum - planTotal).abs() <= 0.01;

            return AlertDialog(
              backgroundColor: AppTheme.darkSurface,
              title: Text(
                existing == null
                    ? 'Cadastrar Novo Plano (/plans)'
                    : 'Editar Plano: ${existing.name}',
              ),
              content: SizedBox(
                width: 640,
                child: SingleChildScrollView(
                  child: Form(
                    key: formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        TextFormField(
                          controller: nameCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Nome Comercial do Plano *',
                            hintText: 'Ex: Fibra Turbo 600 Mega',
                          ),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Informe o nome do plano'
                              : null,
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: downCtrl,
                                keyboardType: TextInputType.number,
                                decoration: const InputDecoration(
                                  labelText: 'Download (Mbps) *',
                                ),
                                validator: (v) =>
                                    (int.tryParse(v ?? '') ?? 0) <= 0
                                    ? 'Inválido'
                                    : null,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: upCtrl,
                                keyboardType: TextInputType.number,
                                decoration: const InputDecoration(
                                  labelText: 'Upload (Mbps) *',
                                ),
                                validator: (v) =>
                                    (int.tryParse(v ?? '') ?? 0) <= 0
                                    ? 'Inválido'
                                    : null,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: priceCtrl,
                                keyboardType:
                                    const TextInputType.numberWithOptions(
                                      decimal: true,
                                    ),
                                decoration: const InputDecoration(
                                  labelText: 'Mensalidade Total (R\$) *',
                                ),
                                onChanged: (_) => setModalState(() {}),
                                validator: (v) {
                                  final parsed = double.tryParse(
                                    (v ?? '').replaceAll(',', '.'),
                                  );
                                  return (parsed == null || parsed <= 0)
                                      ? 'Valor inválido'
                                      : null;
                                },
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: suspCtrl,
                                keyboardType: TextInputType.number,
                                decoration: const InputDecoration(
                                  labelText: 'Dias p/ Bloqueio',
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: AppTheme.darkBg,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(
                              color: sumMatches
                                  ? AppTheme.darkCard
                                  : AppTheme.accentError,
                            ),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Wrap(
                                spacing: 8,
                                runSpacing: 8,
                                alignment: WrapAlignment.spaceBetween,
                                crossAxisAlignment: WrapCrossAlignment.center,
                                children: [
                                  const Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Composição Fiscal Multi-Item (SCM + SVA / Locação)',
                                        style: TextStyle(
                                          fontSize: 13,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                      Text(
                                        'A NFCom (Mod. 62) tributa apenas a soma dos itens SCM.',
                                        style: TextStyle(
                                          fontSize: 11,
                                          color: AppTheme.textSecondary,
                                        ),
                                      ),
                                    ],
                                  ),
                                  OutlinedButton.icon(
                                    onPressed: () {
                                      setModalState(() {
                                        editableItems.add({
                                          'id': null,
                                          'nameCtrl': TextEditingController(
                                            text: 'SVA Adicional',
                                          ),
                                          'category': 'SVA_DIGITAL_CONTENT',
                                          'amountCtrl': TextEditingController(
                                            text: '10.00',
                                          ),
                                          'fiscalCodeCtrl':
                                              TextEditingController(text: ''),
                                        });
                                      });
                                    },
                                    icon: const Icon(Icons.add, size: 16),
                                    label: const Text('Adicionar Item'),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 10),
                              ...editableItems.asMap().entries.map((entry) {
                                final idx = entry.key;
                                final row = entry.value;
                                return Padding(
                                  padding: const EdgeInsets.only(bottom: 10),
                                  child: Wrap(
                                    spacing: 8,
                                    runSpacing: 8,
                                    crossAxisAlignment:
                                        WrapCrossAlignment.center,
                                    children: [
                                      SizedBox(
                                        width: 190,
                                        child: TextFormField(
                                          controller:
                                              row['nameCtrl']
                                                  as TextEditingController,
                                          decoration: const InputDecoration(
                                            labelText: 'Descrição do Item *',
                                            isDense: true,
                                          ),
                                          validator: (v) =>
                                              (v == null || v.trim().isEmpty)
                                              ? 'Obrigatório'
                                              : null,
                                        ),
                                      ),
                                      SizedBox(
                                        width: 195,
                                        child: DropdownButtonFormField<String>(
                                          initialValue:
                                              row['category'] as String,
                                          isExpanded: true,
                                          decoration: const InputDecoration(
                                            labelText: 'Categoria Fiscal *',
                                            isDense: true,
                                          ),
                                          items: const [
                                            DropdownMenuItem(
                                              value: 'SCM',
                                              child: Text(
                                                'SCM (NFCom Mod. 62)',
                                                overflow: TextOverflow.ellipsis,
                                              ),
                                            ),
                                            DropdownMenuItem(
                                              value: 'EQUIPMENT_RENTAL',
                                              child: Text(
                                                'Locação Equipamento',
                                                overflow: TextOverflow.ellipsis,
                                              ),
                                            ),
                                            DropdownMenuItem(
                                              value: 'SVA_DIGITAL_CONTENT',
                                              child: Text(
                                                'SVA Conteúdo Digital',
                                                overflow: TextOverflow.ellipsis,
                                              ),
                                            ),
                                            DropdownMenuItem(
                                              value: 'SVA_STREAMING_APP',
                                              child: Text(
                                                'SVA App / Streaming',
                                                overflow: TextOverflow.ellipsis,
                                              ),
                                            ),
                                            DropdownMenuItem(
                                              value: 'SVA_OTHER',
                                              child: Text(
                                                'SVA Outros',
                                                overflow: TextOverflow.ellipsis,
                                              ),
                                            ),
                                          ],
                                          onChanged: (val) {
                                            if (val != null) {
                                              setModalState(
                                                () => row['category'] = val,
                                              );
                                            }
                                          },
                                        ),
                                      ),
                                      SizedBox(
                                        width: 105,
                                        child: TextFormField(
                                          controller:
                                              row['amountCtrl']
                                                  as TextEditingController,
                                          keyboardType:
                                              const TextInputType.numberWithOptions(
                                                decimal: true,
                                              ),
                                          decoration: const InputDecoration(
                                            labelText: 'Valor (R\$) *',
                                            isDense: true,
                                          ),
                                          onChanged: (_) =>
                                              setModalState(() {}),
                                          validator: (v) {
                                            final parsed = double.tryParse(
                                              (v ?? '').replaceAll(',', '.'),
                                            );
                                            return (parsed == null ||
                                                    parsed < 0)
                                                ? 'Inválido'
                                                : null;
                                          },
                                        ),
                                      ),
                                      if (editableItems.length > 1)
                                        IconButton(
                                          tooltip: 'Remover Item',
                                          icon: const Icon(
                                            Icons.remove_circle_outline,
                                            color: AppTheme.accentError,
                                            size: 20,
                                          ),
                                          onPressed: () {
                                            setModalState(() {
                                              editableItems.removeAt(idx);
                                            });
                                          },
                                        ),
                                    ],
                                  ),
                                );
                              }),
                              const Divider(height: 16),
                              Wrap(
                                spacing: 12,
                                runSpacing: 6,
                                children: [
                                  Text(
                                    'Base SCM (NFCom): ${_formatCurrency(scmSum)}',
                                    style: const TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: AppTheme.accentGreen,
                                    ),
                                  ),
                                  Text(
                                    'SVA / Locação (Isento ICMS): ${_formatCurrency(svaSum)}',
                                    style: const TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: AppTheme.accentWarning,
                                    ),
                                  ),
                                  Text(
                                    'Soma dos Itens: ${_formatCurrency(itemsSum)} / ${_formatCurrency(planTotal)}',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: sumMatches
                                          ? AppTheme.textSecondary
                                          : AppTheme.accentError,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: svaCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Resumo Comercial SVAs (Opcional)',
                            hintText: 'Ex: Paramount+, E-books, Telemedicina',
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: descCtrl,
                          maxLines: 2,
                          decoration: const InputDecoration(
                            labelText: 'Descrição Comercial (Opcional)',
                          ),
                        ),
                        const SizedBox(height: 12),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text(
                            'Emitir NFCom (Modelo 62) automaticamente na baixa',
                            style: TextStyle(fontSize: 13),
                          ),
                          value: alwaysIssueNfcom,
                          onChanged: (val) =>
                              setModalState(() => alwaysIssueNfcom = val),
                        ),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text(
                            'Plano Ativo para Comercialização',
                            style: TextStyle(fontSize: 13),
                          ),
                          value: active,
                          onChanged: (val) => setModalState(() => active = val),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: isSaving
                      ? null
                      : () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancelar'),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryBlue,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: isSaving
                      ? null
                      : () async {
                          if (!formKey.currentState!.validate()) return;
                          if (!sumMatches) {
                            ScaffoldMessenger.of(this.context).showSnackBar(
                              SnackBar(
                                content: Text(
                                  'A soma dos itens (${_formatCurrency(itemsSum)}) deve ser igual ao preço total do plano (${_formatCurrency(planTotal)}).',
                                ),
                                backgroundColor: AppTheme.accentError,
                              ),
                            );
                            return;
                          }
                          setModalState(() => isSaving = true);
                          try {
                            final parsedItems = editableItems.map((row) {
                              final nameText =
                                  (row['nameCtrl'] as TextEditingController)
                                      .text
                                      .trim();
                              final catText = row['category'] as String;
                              final amtVal = double.parse(
                                (row['amountCtrl'] as TextEditingController)
                                    .text
                                    .trim()
                                    .replaceAll(',', '.'),
                              );
                              final fCode =
                                  (row['fiscalCodeCtrl']
                                          as TextEditingController)
                                      .text
                                      .trim();
                              return PlanCompositionItemModel(
                                id: row['id']?.toString(),
                                name: nameText,
                                category: catText,
                                amount: amtVal,
                                fiscalCode: fCode.isEmpty ? null : fCode,
                              );
                            }).toList();

                            final payload = PlanSavePayload(
                              name: nameCtrl.text,
                              downloadSpeed: int.parse(downCtrl.text.trim()),
                              uploadSpeed: int.parse(upCtrl.text.trim()),
                              price: double.parse(
                                priceCtrl.text.trim().replaceAll(',', '.'),
                              ),
                              description: descCtrl.text,
                              svaIncluded: svaCtrl.text,
                              suspensionDays:
                                  int.tryParse(suspCtrl.text.trim()) ?? 5,
                              alwaysIssueNfcom: alwaysIssueNfcom,
                              active: active,
                              compositionItems: parsedItems,
                            );
                            final repo = ref.read(catalogRepositoryProvider);
                            if (existing == null) {
                              await repo.createPlan(payload);
                            } else {
                              await repo.updatePlan(existing.id, payload);
                            }
                            ref.invalidate(adminPlansListProvider);
                            ref
                                .read(salesOnboardingProvider.notifier)
                                .loadActivePlans();
                            if (dialogContext.mounted) {
                              Navigator.of(dialogContext).pop();
                            }
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                    existing == null
                                        ? 'Plano cadastrado com sucesso!'
                                        : 'Plano atualizado com sucesso!',
                                  ),
                                  backgroundColor: AppTheme.accentGreen,
                                ),
                              );
                            }
                          } catch (e) {
                            setModalState(() => isSaving = false);
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text('Erro ao salvar plano: $e'),
                                  backgroundColor: AppTheme.accentError,
                                ),
                              );
                            }
                          }
                        },
                  child: Text(isSaving ? 'Salvando...' : 'Salvar Plano'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Future<void> _confirmDeletePlan(AdminPlanModel plan) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Remover Plano'),
        content: Text(
          'Deseja realmente remover o plano "${plan.name}"? Caso possua contratos vinculados, prefira inativá-lo na edição.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentError,
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Confirmar Exclusão'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;
    try {
      await ref.read(catalogRepositoryProvider).deletePlan(plan.id);
      ref.invalidate(adminPlansListProvider);
      ref.read(salesOnboardingProvider.notifier).loadActivePlans();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Plano removido com sucesso.'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Não foi possível excluir o plano: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  // ===========================================================================
  // ABA 2: TEMPLATES DE CONTRATO (/contracts/templates)
  // ===========================================================================

  Widget _buildTemplatesTab() {
    final templatesAsync = ref.watch(contractTemplatesListProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: templatesAsync.when(
        loading: () => const Center(
          child: Padding(
            padding: EdgeInsets.all(40.0),
            child: CircularProgressIndicator(),
          ),
        ),
        error: (err, _) => _buildErrorCard(
          'Erro ao carregar templates de contrato: $err',
          () => ref.invalidate(contractTemplatesListProvider),
        ),
        data: (templates) {
          if (templates.isEmpty) {
            return _buildEmptyCard(
              icon: Icons.description_outlined,
              title: 'Nenhuma minuta contratual cadastrada',
              subtitle:
                  'Crie templates em Markdown com variáveis dinâmicas ({{cliente.nome}}, {{plano.valor}}) para assinatura eletrônica.',
            );
          }

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: templates.map(_buildTemplateCard).toList(),
          );
        },
      ),
    );
  }

  Widget _buildTemplateCard(ContractTemplateModel tpl) {
    return Card(
      margin: const EdgeInsets.only(bottom: 14),
      color: AppTheme.darkSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppTheme.darkCard),
      ),
      child: Padding(
        padding: const EdgeInsets.all(18.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 12,
              runSpacing: 8,
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.description_outlined,
                      color: AppTheme.primaryIndigo,
                      size: 20,
                    ),
                    const SizedBox(width: 10),
                    Flexible(
                      child: Text(
                        '${tpl.name} (v${tpl.version})',
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: (tpl.isActive
                                ? AppTheme.accentGreen
                                : AppTheme.textMuted)
                            .withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        tpl.isActive ? 'ATIVO' : 'INATIVO',
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: tpl.isActive
                              ? AppTheme.accentGreen
                              : AppTheme.textMuted,
                        ),
                      ),
                    ),
                  ],
                ),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () => _cloneTemplate(tpl),
                      icon: const Icon(Icons.copy_outlined, size: 16),
                      label: const Text('Clonar Versão'),
                    ),
                    OutlinedButton.icon(
                      onPressed: () => _showTemplateDialog(existing: tpl),
                      icon: const Icon(Icons.edit_outlined, size: 16),
                      label: const Text('Editar / Preview'),
                    ),
                    IconButton(
                      tooltip: 'Remover Minuta',
                      icon: const Icon(
                        Icons.delete_outline,
                        color: AppTheme.accentError,
                        size: 20,
                      ),
                      onPressed: () => _deleteTemplate(tpl),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              'Tipo Documental: ${tpl.documentTypeLabel}',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.primaryBlue,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Cláusula de Consentimento: ${tpl.consentClause}',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textSecondary,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _cloneTemplate(ContractTemplateModel tpl) async {
    try {
      await ref
          .read(catalogRepositoryProvider)
          .cloneContractTemplate(tpl.id);
      ref.invalidate(contractTemplatesListProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Minuta "${tpl.name}" clonada com sucesso!'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erro ao clonar minuta: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  Future<void> _deleteTemplate(ContractTemplateModel tpl) async {
    try {
      await ref
          .read(catalogRepositoryProvider)
          .deleteContractTemplate(tpl.id);
      ref.invalidate(contractTemplatesListProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Minuta removida com sucesso.'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erro ao excluir minuta: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  Future<void> _showTemplateDialog({ContractTemplateModel? existing}) async {
    final formKey = GlobalKey<FormState>();
    final nameCtrl = TextEditingController(text: existing?.name ?? '');
    final versionCtrl = TextEditingController(
      text: existing != null ? '${existing.version}' : '1',
    );
    final contentCtrl = TextEditingController(
      text: existing?.contentMarkdown ??
          '# CONTRATO DE PRESTAÇÃO DE SERVIÇOS SCM\n\nContratante: {{customer.name}} - CPF/CNPJ: {{customer.cpf}}\nPlano Contratado: {{plan.name}} ({{plan.downloadSpeed}} Mbps)\nValor Mensal: R\$ {{contract.monthlyFee}}',
    );
    final consentCtrl = TextEditingController(
      text: existing?.consentClause ??
          'Declaro que li e concordo integralmente com os termos desta minuta contratual.',
    );
    String documentType = existing?.documentType ?? 'SERVICE_AGREEMENT';
    bool isActive = existing?.isActive ?? true;
    bool isSaving = false;
    String? previewRendered;
    bool isPreviewLoading = false;

    final variables = await ref
        .read(catalogRepositoryProvider)
        .getTemplateVariables()
        .catchError((_) => <ContractTemplateVariableModel>[]);

    if (!mounted) return;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return AlertDialog(
              backgroundColor: AppTheme.darkSurface,
              title: Text(
                existing == null
                    ? 'Nova Minuta Contratual (/contracts/templates)'
                    : 'Editar Minuta: ${existing.name}',
              ),
              content: SizedBox(
                width: 680,
                child: SingleChildScrollView(
                  child: Form(
                    key: formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              flex: 3,
                              child: TextFormField(
                                controller: nameCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Título da Minuta *',
                                ),
                                validator: (v) =>
                                    (v == null || v.trim().isEmpty)
                                    ? 'Obrigatório'
                                    : null,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              flex: 2,
                              child: DropdownButtonFormField<String>(
                                initialValue: documentType,
                                decoration: const InputDecoration(
                                  labelText: 'Tipo Documental *',
                                ),
                                items: const [
                                  DropdownMenuItem(
                                    value: 'SERVICE_AGREEMENT',
                                    child: Text('Contrato SCM/SVA'),
                                  ),
                                  DropdownMenuItem(
                                    value: 'LOYALTY_TERM',
                                    child: Text('Termo de Fidelidade'),
                                  ),
                                  DropdownMenuItem(
                                    value: 'EQUIPMENT_COMODATO',
                                    child: Text('Comodato ONU/Roteador'),
                                  ),
                                  DropdownMenuItem(
                                    value: 'CUSTOM_TERM',
                                    child: Text('Termo Personalizado'),
                                  ),
                                ],
                                onChanged: (val) {
                                  if (val != null) {
                                    setModalState(() => documentType = val);
                                  }
                                },
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              flex: 1,
                              child: TextFormField(
                                controller: versionCtrl,
                                keyboardType: TextInputType.number,
                                decoration: const InputDecoration(
                                  labelText: 'Versão',
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),
                        if (variables.isNotEmpty) ...[
                          const Text(
                            'Variáveis Dinâmicas Disponíveis (clique para inserir no texto):',
                            style: TextStyle(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Wrap(
                            spacing: 6,
                            runSpacing: 6,
                            children: variables.map((v) {
                              return ActionChip(
                                label: Text(
                                  v.tag,
                                  style: const TextStyle(
                                    fontSize: 11,
                                    fontFamily: 'monospace',
                                  ),
                                ),
                                tooltip: '${v.label} (${v.example ?? ""})',
                                onPressed: () {
                                  final text = contentCtrl.text;
                                  contentCtrl.text = '$text ${v.tag}';
                                },
                              );
                            }).toList(),
                          ),
                          const SizedBox(height: 12),
                        ],
                        TextFormField(
                          controller: contentCtrl,
                          maxLines: 8,
                          decoration: const InputDecoration(
                            labelText: 'Conteúdo em Markdown *',
                            alignLabelWithHint: true,
                          ),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Informe o corpo da minuta'
                              : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: consentCtrl,
                          maxLines: 2,
                          decoration: const InputDecoration(
                            labelText: 'Cláusula de Aceite Eletrônico *',
                          ),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Informe a cláusula de aceite'
                              : null,
                        ),
                        const SizedBox(height: 10),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: SwitchListTile(
                                contentPadding: EdgeInsets.zero,
                                title: const Text(
                                  'Template Ativo',
                                  style: TextStyle(fontSize: 13),
                                ),
                                value: isActive,
                                onChanged: (val) =>
                                    setModalState(() => isActive = val),
                              ),
                            ),
                            OutlinedButton.icon(
                              onPressed: isPreviewLoading
                                  ? null
                                  : () async {
                                      setModalState(
                                        () => isPreviewLoading = true,
                                      );
                                      try {
                                        final rendered = await ref
                                            .read(catalogRepositoryProvider)
                                            .previewContractTemplate(
                                              contentCtrl.text,
                                            );
                                        setModalState(() {
                                          previewRendered = rendered;
                                          isPreviewLoading = false;
                                        });
                                      } catch (e) {
                                        setModalState(
                                          () => isPreviewLoading = false,
                                        );
                                      }
                                    },
                              icon: const Icon(
                                Icons.visibility_outlined,
                                size: 16,
                              ),
                              label: Text(
                                isPreviewLoading
                                    ? 'Renderizando...'
                                    : 'Testar Preview (/preview)',
                              ),
                            ),
                          ],
                        ),
                        if (previewRendered != null) ...[
                          const SizedBox(height: 10),
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppTheme.darkBg,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: AppTheme.primaryBlue.withValues(
                                  alpha: 0.4,
                                ),
                              ),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'Pré-visualização Interpolada:',
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                    color: AppTheme.primaryBlue,
                                  ),
                                ),
                                const SizedBox(height: 6),
                                SelectableText(
                                  previewRendered!,
                                  style: const TextStyle(fontSize: 12),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: isSaving
                      ? null
                      : () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancelar'),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryIndigo,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: isSaving
                      ? null
                      : () async {
                          if (!formKey.currentState!.validate()) return;
                          setModalState(() => isSaving = true);
                          try {
                            final payload = ContractTemplateSavePayload(
                              name: nameCtrl.text,
                              documentType: documentType,
                              version:
                                  int.tryParse(versionCtrl.text.trim()) ?? 1,
                              isActive: isActive,
                              contentMarkdown: contentCtrl.text,
                              consentClause: consentCtrl.text,
                            );
                            final repo = ref.read(catalogRepositoryProvider);
                            if (existing == null) {
                              await repo.createContractTemplate(payload);
                            } else {
                              await repo.updateContractTemplate(
                                existing.id,
                                payload,
                              );
                            }
                            ref.invalidate(contractTemplatesListProvider);
                            if (dialogContext.mounted) {
                              Navigator.of(dialogContext).pop();
                            }
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                    existing == null
                                        ? 'Minuta criada com sucesso!'
                                        : 'Minuta atualizada com sucesso!',
                                  ),
                                  backgroundColor: AppTheme.accentGreen,
                                ),
                              );
                            }
                          } catch (e) {
                            setModalState(() => isSaving = false);
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text('Erro ao salvar minuta: $e'),
                                  backgroundColor: AppTheme.accentError,
                                ),
                              );
                            }
                          }
                        },
                  child: Text(isSaving ? 'Salvando...' : 'Salvar Minuta'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Widget _buildEmptyCard({
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        children: [
          Icon(icon, size: 36, color: AppTheme.textMuted),
          const SizedBox(height: 12),
          Text(
            title,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildErrorCard(String message, VoidCallback onRetry) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.accentError.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: AppTheme.accentError),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(color: AppTheme.accentError, fontSize: 13),
            ),
          ),
          TextButton(onPressed: onRetry, child: const Text('Tentar Novamente')),
        ],
      ),
    );
  }
}
