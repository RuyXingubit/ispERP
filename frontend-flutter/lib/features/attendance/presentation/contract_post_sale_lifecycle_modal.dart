import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/attendance_models.dart';
import '../data/attendance_repository.dart';
import '../data/customer_search_model.dart';

/// Modal operacional do Ciclo de Vida Pós-Venda de Contratos (Onda I):
/// 1. Upgrade / Downgrade de Plano (cálculo Pro-Rata base 30 dias + CoA imediato no FreeRADIUS)
/// 2. Mudança de Endereço (atualiza endereço/CTO + gera O.S. de Instalação + taxa/isenção)
/// 3. Troca de Titularidade (migra faturas PENDING/OVERDUE para o novo CPF/CNPJ + Assinatura Forense)
/// 4. Histórico Auditado de Eventos Pós-Venda
class ContractPostSaleLifecycleModal extends ConsumerStatefulWidget {
  final ContractSummary contract;
  final String currentCustomerId;
  final String currentCustomerName;
  final VoidCallback? onSuccess;

  const ContractPostSaleLifecycleModal({
    super.key,
    required this.contract,
    required this.currentCustomerId,
    required this.currentCustomerName,
    this.onSuccess,
  });

  static Future<void> show(
    BuildContext context, {
    required ContractSummary contract,
    required String currentCustomerId,
    required String currentCustomerName,
    VoidCallback? onSuccess,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => Dialog(
        backgroundColor: AppTheme.darkSurface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
          side: const BorderSide(color: AppTheme.darkBorder, width: 1.2),
        ),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 760, maxHeight: 680),
          child: ContractPostSaleLifecycleModal(
            contract: contract,
            currentCustomerId: currentCustomerId,
            currentCustomerName: currentCustomerName,
            onSuccess: onSuccess,
          ),
        ),
      ),
    );
  }

  @override
  ConsumerState<ContractPostSaleLifecycleModal> createState() =>
      _ContractPostSaleLifecycleModalState();
}

class _ContractPostSaleLifecycleModalState
    extends ConsumerState<ContractPostSaleLifecycleModal>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  bool _isLoadingData = true;
  bool _isSubmitting = false;
  String? _feedbackMessage;
  bool _isErrorFeedback = false;

  // Tab 1: Plan Change
  List<PlanOptionModel> _activePlans = [];
  PlanOptionModel? _selectedPlan;
  final _planNotesCtrl = TextEditingController();

  // Tab 2: Address Change
  late final TextEditingController _addressCtrl;
  late final TextEditingController _cityCtrl;
  late final TextEditingController _stateCtrl;
  late final TextEditingController _zipCtrl;
  late final TextEditingController _ctoIdCtrl;
  late final TextEditingController _ctoPortCtrl;
  final _serviceFeeCtrl = TextEditingController(text: '150.00');
  bool _requestWaiver = false;
  final _waiverReasonCtrl = TextEditingController();
  final _addressNotesCtrl = TextEditingController();

  // Tab 3: Ownership Transfer
  final _customerSearchCtrl = TextEditingController();
  List<CustomerSearchModel> _customerResults = [];
  CustomerSearchModel? _selectedNewCustomer;
  bool _isSearchingCustomer = false;
  final _transferNotesCtrl = TextEditingController();

  // Tab 4: History
  List<ContractLifecycleEventModel> _events = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _addressCtrl = TextEditingController(text: widget.contract.installationAddress);
    _cityCtrl = TextEditingController(text: widget.contract.city);
    _stateCtrl = TextEditingController(
      text: widget.contract.state.isNotEmpty ? widget.contract.state : 'SP',
    );
    _zipCtrl = TextEditingController(text: widget.contract.zipCode);
    _ctoIdCtrl = TextEditingController(text: widget.contract.ctoId ?? '');
    _ctoPortCtrl = TextEditingController(
      text: widget.contract.ctoPortNumber?.toString() ?? '',
    );
    _loadInitialData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _planNotesCtrl.dispose();
    _addressCtrl.dispose();
    _cityCtrl.dispose();
    _stateCtrl.dispose();
    _zipCtrl.dispose();
    _ctoIdCtrl.dispose();
    _ctoPortCtrl.dispose();
    _serviceFeeCtrl.dispose();
    _waiverReasonCtrl.dispose();
    _addressNotesCtrl.dispose();
    _customerSearchCtrl.dispose();
    _transferNotesCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadInitialData() async {
    setState(() => _isLoadingData = true);
    final repo = ref.read(attendanceRepositoryProvider);
    final results = await Future.wait([
      repo.getActivePlans(),
      repo.getContractLifecycleEvents(widget.contract.id),
    ]);
    if (!mounted) return;
    final plans = results[0] as List<PlanOptionModel>;
    final events = results[1] as List<ContractLifecycleEventModel>;
    setState(() {
      _activePlans = plans;
      if (plans.isNotEmpty) {
        _selectedPlan = plans.firstWhere(
          (p) => p.id != widget.contract.planId,
          orElse: () => plans.first,
        );
      }
      _events = events;
      _isLoadingData = false;
    });
  }

  Future<void> _searchNewTitular(String query) async {
    if (query.trim().length < 2) {
      setState(() => _customerResults = []);
      return;
    }
    setState(() => _isSearchingCustomer = true);
    final repo = ref.read(attendanceRepositoryProvider);
    final list = await repo.searchCustomers(query);
    if (!mounted) return;
    setState(() {
      _customerResults = list
          .where((c) => c.id != widget.currentCustomerId)
          .toList();
      _isSearchingCustomer = false;
    });
  }

  Future<void> _submitPlanChange() async {
    final plan = _selectedPlan;
    if (plan == null) return;
    setState(() {
      _isSubmitting = true;
      _feedbackMessage = null;
    });
    try {
      final repo = ref.read(attendanceRepositoryProvider);
      final result = await repo.changeContractPlan(
        contractId: widget.contract.id,
        newPlanId: plan.id,
        notes: _planNotesCtrl.text,
      );
      final updatedEvents = await repo.getContractLifecycleEvents(widget.contract.id);
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _isErrorFeedback = false;
        _feedbackMessage = result.message;
        _events = updatedEvents;
      });
      widget.onSuccess?.call();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _isErrorFeedback = true;
        _feedbackMessage = 'Falha ao alterar plano: $e';
      });
    }
  }

  Future<void> _submitAddressChange() async {
    if (_addressCtrl.text.trim().isEmpty ||
        _cityCtrl.text.trim().isEmpty ||
        _stateCtrl.text.trim().length != 2 ||
        _zipCtrl.text.trim().isEmpty) {
      setState(() {
        _isErrorFeedback = true;
        _feedbackMessage =
            'Preencha endereço completo, cidade, UF (2 letras) e CEP.';
      });
      return;
    }
    setState(() {
      _isSubmitting = true;
      _feedbackMessage = null;
    });
    try {
      final repo = ref.read(attendanceRepositoryProvider);
      final fee = double.tryParse(_serviceFeeCtrl.text.replaceAll(',', '.')) ?? 0.0;
      final port = int.tryParse(_ctoPortCtrl.text.trim());
      final result = await repo.changeContractAddress(
        contractId: widget.contract.id,
        newInstallationAddress: _addressCtrl.text,
        newCity: _cityCtrl.text,
        newState: _stateCtrl.text.toUpperCase(),
        newZipCode: _zipCtrl.text,
        newCtoId: _ctoIdCtrl.text.trim().isEmpty ? null : _ctoIdCtrl.text.trim(),
        newCtoPortNumber: port,
        serviceFee: fee,
        requestWaiver: _requestWaiver,
        waiverReason: _requestWaiver ? _waiverReasonCtrl.text : null,
        notes: _addressNotesCtrl.text,
      );
      final updatedEvents = await repo.getContractLifecycleEvents(widget.contract.id);
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _isErrorFeedback = false;
        _feedbackMessage = result.message;
        _events = updatedEvents;
      });
      widget.onSuccess?.call();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _isErrorFeedback = true;
        _feedbackMessage = 'Falha ao registrar mudança de endereço: $e';
      });
    }
  }

  Future<void> _submitOwnershipTransfer() async {
    final targetCustomer = _selectedNewCustomer;
    if (targetCustomer == null) {
      setState(() {
        _isErrorFeedback = true;
        _feedbackMessage = 'Selecione o novo titular (CPF/CNPJ) antes de confirmar.';
      });
      return;
    }
    setState(() {
      _isSubmitting = true;
      _feedbackMessage = null;
    });
    try {
      final repo = ref.read(attendanceRepositoryProvider);
      final result = await repo.transferContractOwnership(
        contractId: widget.contract.id,
        newCustomerId: targetCustomer.id,
        notes: _transferNotesCtrl.text,
      );
      final updatedEvents = await repo.getContractLifecycleEvents(widget.contract.id);
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _isErrorFeedback = false;
        _feedbackMessage = result.message;
        _events = updatedEvents;
      });
      widget.onSuccess?.call();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _isErrorFeedback = true;
        _feedbackMessage = 'Falha ao transferir titularidade: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Row(
            children: [
              const Icon(Icons.published_with_changes, color: AppTheme.primaryBlue, size: 22),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Pós-Venda de Contrato • #${widget.contract.contractNumber}',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: AppTheme.textPrimary,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                    Text(
                      'Titular atual: ${widget.currentCustomerName} • Mensalidade R\$ ${widget.contract.monthlyFee.toStringAsFixed(2)} (Venc. dia ${widget.contract.dueDay})',
                      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.close, color: AppTheme.textSecondary),
                tooltip: 'Fechar',
              ),
            ],
          ),
          const SizedBox(height: 12),
          TabBar(
            controller: _tabController,
            isScrollable: true,
            labelColor: AppTheme.primaryBlue,
            unselectedLabelColor: AppTheme.textSecondary,
            indicatorColor: AppTheme.primaryBlue,
            tabs: [
              const Tab(
                icon: Icon(Icons.speed, size: 16),
                text: 'Upgrade / Downgrade (Pro-Rata)',
              ),
              const Tab(
                icon: Icon(Icons.location_on_outlined, size: 16),
                text: 'Mudança de Endereço + O.S.',
              ),
              const Tab(
                icon: Icon(Icons.swap_horiz, size: 16),
                text: 'Troca de Titularidade',
              ),
              Tab(
                icon: const Icon(Icons.history, size: 16),
                text: 'Histórico (${_events.length})',
              ),
            ],
          ),
          if (_feedbackMessage != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: (_isErrorFeedback ? AppTheme.accentError : AppTheme.accentGreen)
                    .withValues(alpha: 0.14),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(
                  color: _isErrorFeedback ? AppTheme.accentError : AppTheme.accentGreen,
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    _isErrorFeedback ? Icons.error_outline : Icons.check_circle_outline,
                    color: _isErrorFeedback ? AppTheme.accentError : AppTheme.accentGreen,
                    size: 18,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _feedbackMessage!,
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: _isErrorFeedback ? AppTheme.accentError : AppTheme.accentGreen,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 12),
          Expanded(
            child: _isLoadingData
                ? const Center(child: CircularProgressIndicator())
                : TabBarView(
                    controller: _tabController,
                    children: [
                      _buildPlanChangeTab(),
                      _buildAddressChangeTab(),
                      _buildOwnershipTransferTab(),
                      _buildHistoryTab(),
                    ],
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildPlanChangeTab() {
    if (_activePlans.isEmpty) {
      return const Center(
        child: Text(
          'Nenhum plano ativo encontrado para troca.',
          style: TextStyle(color: AppTheme.textSecondary),
        ),
      );
    }

    final selected = _selectedPlan ?? _activePlans.first;
    final preview = ContractSummary.calculateProRataPreview(
      currentMonthlyFee: widget.contract.monthlyFee,
      newMonthlyFee: selected.monthlyPrice,
      dueDay: widget.contract.dueDay,
    );
    final isUpgrade = selected.monthlyPrice >= widget.contract.monthlyFee;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Selecione o novo plano comercial (velocidade aplicada via CoA no FreeRADIUS imediatamente):',
            style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppTheme.textPrimary),
          ),
          const SizedBox(height: 10),
          DropdownButtonFormField<PlanOptionModel>(
            initialValue: selected,
            dropdownColor: AppTheme.darkSurface,
            isExpanded: true,
            decoration: const InputDecoration(
              labelText: 'Novo Plano Comercial',
              border: OutlineInputBorder(),
            ),
            items: _activePlans.map((plan) {
              return DropdownMenuItem<PlanOptionModel>(
                value: plan,
                child: Text(
                  '${plan.name} • ${plan.downloadSpeedMbps}/${plan.uploadSpeedMbps} Mbps — R\$ ${plan.monthlyPrice.toStringAsFixed(2)}/mês',
                  overflow: TextOverflow.ellipsis,
                ),
              );
            }).toList(),
            onChanged: (val) {
              if (val != null) {
                setState(() => _selectedPlan = val);
              }
            },
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppTheme.darkBg,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 6,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Icon(
                      isUpgrade ? Icons.trending_up : Icons.trending_down,
                      color: isUpgrade ? AppTheme.accentGreen : AppTheme.accentWarning,
                      size: 18,
                    ),
                    Text(
                      isUpgrade ? 'Simulação Pro-Rata de Upgrade' : 'Simulação Pro-Rata de Downgrade',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  '• Dias restantes até o próximo vencimento (dia ${widget.contract.dueDay}): ${preview.remainingDays} dias (base comercial 30 dias)',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                Text(
                  '• Mensalidade anterior: R\$ ${widget.contract.monthlyFee.toStringAsFixed(2)} ➔ Nova mensalidade: R\$ ${selected.monthlyPrice.toStringAsFixed(2)}',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 4),
                Text(
                  '• Ajuste Pro-Rata na próxima fatura: ${preview.proRataDelta >= 0 ? '+' : ''}R\$ ${preview.proRataDelta.toStringAsFixed(2)}',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: preview.proRataDelta >= 0 ? AppTheme.primaryBlue : AppTheme.accentWarning,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  '• Provisionamento: sincronização automática no FreeRADIUS (Mikrotik-Rate-Limit) sem necessidade de nova O.S.',
                  style: TextStyle(fontSize: 11, color: AppTheme.textMuted),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _planNotesCtrl,
            decoration: const InputDecoration(
              labelText: 'Observações comerciais / atendimento (opcional)',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 16),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton.icon(
              onPressed: _isSubmitting ? null : _submitPlanChange,
              icon: _isSubmitting
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.bolt, size: 18),
              label: const Text('Confirmar Mudança de Plano + CoA'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAddressChangeTab() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Novo Endereço de Instalação (gera automaticamente O.S. de Instalação na fila técnica):',
            style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppTheme.textPrimary),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _addressCtrl,
            decoration: const InputDecoration(
              labelText: 'Logradouro, Número e Complemento *',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                flex: 3,
                child: TextField(
                  controller: _cityCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Cidade *',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                flex: 1,
                child: TextField(
                  controller: _stateCtrl,
                  maxLength: 2,
                  decoration: const InputDecoration(
                    labelText: 'UF *',
                    counterText: '',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                flex: 2,
                child: TextField(
                  controller: _zipCtrl,
                  decoration: const InputDecoration(
                    labelText: 'CEP *',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                flex: 2,
                child: TextField(
                  controller: _ctoIdCtrl,
                  decoration: const InputDecoration(
                    labelText: 'UUID da Nova CTO (opcional)',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                flex: 1,
                child: TextField(
                  controller: _ctoPortCtrl,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: 'Porta CTO (1-16)',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                flex: 1,
                child: TextField(
                  controller: _serviceFeeCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(
                    labelText: 'Taxa Mudança (R\$)',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text(
              'Solicitar isenção comercial/fidelidade da taxa de mudança de endereço',
              style: TextStyle(fontSize: 13, color: AppTheme.textPrimary),
            ),
            value: _requestWaiver,
            onChanged: (val) => setState(() => _requestWaiver = val),
          ),
          if (_requestWaiver) ...[
            TextField(
              controller: _waiverReasonCtrl,
              decoration: const InputDecoration(
                labelText: 'Justificativa da Isenção (alçada de aprovação)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
          ],
          TextField(
            controller: _addressNotesCtrl,
            decoration: const InputDecoration(
              labelText: 'Instruções para a equipe de campo / O.S. (opcional)',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 16),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton.icon(
              onPressed: _isSubmitting ? null : _submitAddressChange,
              icon: _isSubmitting
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.engineering_outlined, size: 18),
              label: const Text('Confirmar Novo Endereço e Gerar O.S.'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOwnershipTransferTab() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppTheme.accentWarning.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppTheme.accentWarning),
            ),
            child: const Text(
              'Regra Operacional (Onda I): A troca de titularidade preserva a porta CTO e a ONU instaladas, migra automaticamente todas as faturas PENDING e OVERDUE deste contrato para o novo CPF/CNPJ e gera uma nova sessão de Assinatura Eletrônica Forense.',
              style: TextStyle(fontSize: 12, color: AppTheme.textPrimary),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _customerSearchCtrl,
            onChanged: _searchNewTitular,
            decoration: InputDecoration(
              labelText: 'Buscar Novo Titular por Nome ou CPF/CNPJ',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _isSearchingCustomer
                  ? const Padding(
                      padding: EdgeInsets.all(12),
                      child: SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    )
                  : null,
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 10),
          if (_selectedNewCustomer != null)
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: AppTheme.accentGreen.withValues(alpha: 0.14),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.accentGreen),
              ),
              child: Row(
                children: [
                  const Icon(Icons.person_pin, color: AppTheme.accentGreen, size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Novo Titular Selecionado: ${_selectedNewCustomer!.name} (Doc: ${_selectedNewCustomer!.formattedCpf})',
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: AppTheme.textPrimary,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  TextButton(
                    onPressed: () => setState(() => _selectedNewCustomer = null),
                    child: const Text('Trocar'),
                  ),
                ],
              ),
            )
          else if (_customerResults.isNotEmpty)
            Container(
              constraints: const BoxConstraints(maxHeight: 160),
              decoration: BoxDecoration(
                color: AppTheme.darkBg,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.darkBorder),
              ),
              child: ListView.separated(
                shrinkWrap: true,
                itemCount: _customerResults.length,
                separatorBuilder: (_, _) => const Divider(height: 1, color: AppTheme.darkBorder),
                itemBuilder: (context, index) {
                  final c = _customerResults[index];
                  return ListTile(
                    dense: true,
                    title: Text(
                      c.name,
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                      overflow: TextOverflow.ellipsis,
                    ),
                    subtitle: Text(
                      'CPF/CNPJ: ${c.formattedCpf}',
                      style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                    ),
                    trailing: const Icon(Icons.chevron_right, size: 18),
                    onTap: () => setState(() => _selectedNewCustomer = c),
                  );
                },
              ),
            ),
          const SizedBox(height: 12),
          TextField(
            controller: _transferNotesCtrl,
            decoration: const InputDecoration(
              labelText: 'Motivo / Observações da Troca de Titularidade (opcional)',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 16),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton.icon(
              onPressed: _isSubmitting ? null : _submitOwnershipTransfer,
              icon: _isSubmitting
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.verified_user_outlined, size: 18),
              label: const Text('Confirmar Troca de Titularidade + Migrar Faturas'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHistoryTab() {
    if (_events.isEmpty) {
      return const Center(
        child: Text(
          'Nenhum evento pós-venda registrado para este contrato.',
          style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
        ),
      );
    }

    return ListView.separated(
      itemCount: _events.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (context, index) {
        final ev = _events[index];
        return Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: AppTheme.darkBg,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppTheme.darkBorder),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      ev.eventTypeLabel,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: AppTheme.primaryBlue,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  Text(
                    ev.createdAt,
                    style: const TextStyle(fontSize: 11, color: AppTheme.textMuted),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              if (ev.proRataAdjustmentAmount != null)
                Text(
                  'Ajuste Pro-Rata (${ev.proRataDays ?? 0} dias): R\$ ${ev.proRataAdjustmentAmount!.toStringAsFixed(2)} • CoA RADIUS: ${ev.radiusCoaSynced ? "Sincronizado" : "Pendente"}',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
              if (ev.newAddress != null && ev.newAddress!.isNotEmpty)
                Text(
                  'Novo Endereço: ${ev.newAddress} • O.S. Gerada: ${ev.generatedWorkOrderId ?? "N/A"}',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
              if (ev.migratedInvoicesCount != null)
                Text(
                  'Faturas migradas ao novo titular: ${ev.migratedInvoicesCount} • Sessão Assinatura Forense: ${ev.signatureSessionId ?? "N/A"}',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
              if (ev.notes != null && ev.notes!.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    'Obs: ${ev.notes}',
                    style: const TextStyle(fontSize: 11, color: AppTheme.textMuted),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}
