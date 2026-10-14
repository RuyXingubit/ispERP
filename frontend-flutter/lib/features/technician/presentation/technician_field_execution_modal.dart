import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../../inventory/data/inventory_models.dart';
import '../data/technician_execution_models.dart';
import '../data/technician_execution_repository.dart';

/// Modal Guiado em Etapas para Execução Técnica de Campo (OLT Auto-Find -> Provisionamento -> RADIUS -> Laudo)
/// e Fluxo de Logística Reversa para Ordens de Serviço do tipo RETIRADA.
class TechnicianFieldExecutionModal extends ConsumerStatefulWidget {
  final Map<String, dynamic> workOrder;
  final VoidCallback onUpdated;

  const TechnicianFieldExecutionModal({
    super.key,
    required this.workOrder,
    required this.onUpdated,
  });

  static Future<void> show(
    BuildContext context, {
    required Map<String, dynamic> workOrder,
    required VoidCallback onUpdated,
  }) {
    return showDialog(
      context: context,
      builder: (_) => TechnicianFieldExecutionModal(
        workOrder: workOrder,
        onUpdated: onUpdated,
      ),
    );
  }

  @override
  ConsumerState<TechnicianFieldExecutionModal> createState() =>
      _TechnicianFieldExecutionModalState();
}

class _TechnicianFieldExecutionModalState
    extends ConsumerState<TechnicianFieldExecutionModal> {
  // Etapas: 0 = Auto-Find OLT, 1 = Provisionar 1-Clique, 2 = Validar RADIUS, 3 = Laudo & Assinatura
  int _currentStep = 0;

  // Estado Auto-Find OLT
  bool _isScanningOlt = false;
  List<OltUnprovisionedOnuModel> _discoveredOnus = [];
  String? _oltScanMessage;

  // Estado Provisionamento 1-Clique
  final _serialCtrl = TextEditingController();
  final _macCtrl = TextEditingController();
  final _vlanCtrl = TextEditingController(text: '100');
  final _pppoeUserCtrl = TextEditingController();
  final _pppoePassCtrl = TextEditingController();
  bool _isProvisioning = false;
  ProvisionOnuResult? _provisionResult;

  // Estado Diagnóstico RADIUS
  bool _isCheckingRadius = false;
  RadiusSessionStatusModel? _radiusStatus;

  // Estado Laudo & Conclusão
  final _signalCtrl = TextEditingController(text: '-19.50');
  final _wifiSsidCtrl = TextEditingController();
  final _signatureNameCtrl = TextEditingController();
  final _notesCtrl = TextEditingController();
  bool _isCompleting = false;

  // Estado Logística Reversa (O.S. RETIRADA)
  bool _isRemovalFailMode = false;
  List<WarehouseModel> _warehouses = [];
  String? _selectedWarehouseId;
  String _selectedUnsuccessReason = 'Cliente ausente e imóvel desocupado';
  final _removalNotesCtrl = TextEditingController();
  final _removalPhotoCtrl = TextEditingController();
  bool _isSubmittingRemoval = false;
  LegalCollectionResultModel? _legalCollectionResult;

  String? _errorMessage;

  String get _woId => widget.workOrder['id']?.toString() ?? '';
  String get _woType =>
      (widget.workOrder['type']?.toString() ?? 'INSTALACAO').toUpperCase();
  bool get _isRemovalOrder => _woType.contains('RETIRADA');

  @override
  void initState() {
    super.initState();
    final existingSerial = widget.workOrder['onuSerial']?.toString() ?? '';
    final existingMac = widget.workOrder['onuMac']?.toString() ?? '';
    final existingSignal = widget.workOrder['fiberSignalDbm']?.toString();
    if (existingSerial.isNotEmpty) _serialCtrl.text = existingSerial;
    if (existingMac.isNotEmpty) _macCtrl.text = existingMac;
    if (existingSignal != null && existingSignal.isNotEmpty) {
      _signalCtrl.text = existingSignal;
    }

    if (_isRemovalOrder) {
      _loadWarehouses();
    } else {
      _scanUnprovisionedOnus();
    }
  }

  @override
  void dispose() {
    _serialCtrl.dispose();
    _macCtrl.dispose();
    _vlanCtrl.dispose();
    _pppoeUserCtrl.dispose();
    _pppoePassCtrl.dispose();
    _signalCtrl.dispose();
    _wifiSsidCtrl.dispose();
    _signatureNameCtrl.dispose();
    _notesCtrl.dispose();
    _removalNotesCtrl.dispose();
    _removalPhotoCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadWarehouses() async {
    final repo = ref.read(technicianExecutionRepositoryProvider);
    final list = await repo.listWarehouses();
    if (!mounted) return;
    setState(() {
      _warehouses = list;
      if (list.isNotEmpty && _selectedWarehouseId == null) {
        _selectedWarehouseId = list.first.id;
      }
    });
  }

  Future<void> _scanUnprovisionedOnus() async {
    if (_woId.isEmpty) return;
    setState(() {
      _isScanningOlt = true;
      _errorMessage = null;
      _oltScanMessage = null;
    });
    try {
      final repo = ref.read(technicianExecutionRepositoryProvider);
      final onus = await repo.listUnprovisionedOnus(_woId);
      if (!mounted) return;
      setState(() {
        _discoveredOnus = onus;
        _isScanningOlt = false;
        if (onus.isEmpty) {
          _oltScanMessage =
              'Nenhuma ONU aguardando autorização encontrada nas portas PON da OLT.';
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isScanningOlt = false;
        _errorMessage = 'Falha ao consultar OLT: $e';
      });
    }
  }

  void _selectDiscoveredOnu(OltUnprovisionedOnuModel onu) {
    setState(() {
      _serialCtrl.text = onu.onuSerial;
      if (onu.onuMac != null && onu.onuMac!.isNotEmpty) {
        _macCtrl.text = onu.onuMac!;
      }
      if (onu.rxPowerDbm != null) {
        _signalCtrl.text = onu.rxPowerDbm!.toStringAsFixed(2);
      }
      _currentStep = 1;
    });
  }

  Future<void> _provisionOnu() async {
    final serial = _serialCtrl.text.trim();
    if (serial.isEmpty) {
      setState(() => _errorMessage = 'Informe o Serial PON da ONU para provisionar.');
      return;
    }
    setState(() {
      _isProvisioning = true;
      _errorMessage = null;
    });
    try {
      final repo = ref.read(technicianExecutionRepositoryProvider);
      final vlan = int.tryParse(_vlanCtrl.text.trim()) ?? 100;
      final res = await repo.provisionOnu(
        workOrderId: _woId,
        onuSerial: serial,
        vlanId: vlan,
        pppoeUsername: _pppoeUserCtrl.text,
        pppoePassword: _pppoePassCtrl.text,
      );
      if (!mounted) return;
      setState(() {
        _provisionResult = res;
        _isProvisioning = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isProvisioning = false;
        _errorMessage = 'Falha ao provisionar ONU na OLT: $e';
      });
    }
  }

  Future<void> _checkRadiusStatus() async {
    setState(() {
      _isCheckingRadius = true;
      _errorMessage = null;
    });
    try {
      final repo = ref.read(technicianExecutionRepositoryProvider);
      final status = await repo.getRadiusStatus(_woId);
      if (!mounted) return;
      setState(() {
        _radiusStatus = status;
        _isCheckingRadius = false;
        if (status.macAddress != null &&
            status.macAddress!.isNotEmpty &&
            _macCtrl.text.trim().isEmpty) {
          _macCtrl.text = status.macAddress!;
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isCheckingRadius = false;
        _errorMessage = 'Falha ao consultar sessão FreeRADIUS: $e';
      });
    }
  }

  Future<void> _completeFieldExecution() async {
    final serial = _serialCtrl.text.trim();
    final signal = double.tryParse(_signalCtrl.text.trim().replaceAll(',', '.'));
    final sigName = _signatureNameCtrl.text.trim();

    if (serial.isEmpty) {
      setState(() => _errorMessage = 'O Serial da ONU é obrigatório para concluir a O.S.');
      return;
    }
    if (sigName.isEmpty) {
      setState(() => _errorMessage = 'Informe o nome legível do cliente/recebedor.');
      return;
    }

    setState(() {
      _isCompleting = true;
      _errorMessage = null;
    });

    try {
      final repo = ref.read(technicianExecutionRepositoryProvider);
      await repo.completeInstallation(
        _woId,
        TechnicianExecutionCompletePayload(
          onuSerial: serial,
          onuMac: _macCtrl.text.trim().isNotEmpty ? _macCtrl.text.trim() : null,
          vlanId: int.tryParse(_vlanCtrl.text.trim()),
          pppoeUsername: _pppoeUserCtrl.text.trim().isNotEmpty
              ? _pppoeUserCtrl.text.trim()
              : null,
          pppoePassword: _pppoePassCtrl.text.trim().isNotEmpty
              ? _pppoePassCtrl.text.trim()
              : null,
          wifiSsid: _wifiSsidCtrl.text.trim().isNotEmpty
              ? _wifiSsidCtrl.text.trim()
              : null,
          fiberSignalDbm: signal,
          customerSignatureName: sigName,
          notes: _notesCtrl.text.trim().isNotEmpty
              ? _notesCtrl.text.trim()
              : null,
        ),
      );
      if (!mounted) return;
      widget.onUpdated();
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('O.S. concluída com provisionamento e laudo técnico!'),
          backgroundColor: AppTheme.accentGreen,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isCompleting = false;
        _errorMessage = 'Erro ao concluir O.S.: $e';
      });
    }
  }

  Future<void> _submitEquipmentRemoval() async {
    setState(() {
      _isSubmittingRemoval = true;
      _errorMessage = null;
    });

    try {
      final repo = ref.read(technicianExecutionRepositoryProvider);
      if (!_isRemovalFailMode) {
        final warehouseId = _selectedWarehouseId;
        if (warehouseId == null || warehouseId.isEmpty) {
          setState(() {
            _isSubmittingRemoval = false;
            _errorMessage = 'Selecione o almoxarifado de destino para devolver o equipamento.';
          });
          return;
        }
        await repo.completeRemoval(
          _woId,
          CompleteRemovalPayload(
            warehouseId: warehouseId,
            photoUrl: _removalPhotoCtrl.text.trim().isNotEmpty
                ? _removalPhotoCtrl.text.trim()
                : null,
            notes: _removalNotesCtrl.text.trim().isNotEmpty
                ? _removalNotesCtrl.text.trim()
                : null,
          ),
        );
        if (!mounted) return;
        widget.onUpdated();
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Equipamento recolhido e devolvido ao almoxarifado com sucesso!'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      } else {
        final result = await repo.failRemoval(
          _woId,
          FailRemovalPayload(
            unsuccessReason: _selectedUnsuccessReason,
            photoUrl: _removalPhotoCtrl.text.trim().isNotEmpty
                ? _removalPhotoCtrl.text.trim()
                : null,
            notes: _removalNotesCtrl.text.trim().isNotEmpty
                ? _removalNotesCtrl.text.trim()
                : null,
          ),
        );
        if (!mounted) return;
        setState(() {
          _isSubmittingRemoval = false;
          _legalCollectionResult = result;
        });
        widget.onUpdated();
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSubmittingRemoval = false;
        _errorMessage = 'Erro ao registrar logística reversa: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final customerName =
        widget.workOrder['customerName']?.toString() ?? 'Assinante';
    final address =
        widget.workOrder['installationAddress']?.toString() ?? 'Endereço do cliente';

    return Dialog(
      backgroundColor: AppTheme.darkBg,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: AppTheme.darkBorder),
      ),
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 700, maxHeight: 620),
        child: Padding(
          padding: const EdgeInsets.all(18.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Cabeçalho
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: (_isRemovalOrder
                              ? AppTheme.accentWarning
                              : AppTheme.primaryBlue)
                          .withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Icon(
                      _isRemovalOrder
                          ? Icons.assignment_return_outlined
                          : Icons.router_outlined,
                      color: _isRemovalOrder
                          ? AppTheme.accentWarning
                          : AppTheme.primaryBlue,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _isRemovalOrder
                              ? 'Logística Reversa em Campo ($_woType)'
                              : 'Execução Técnica de Campo ($_woType)',
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 15,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          '$customerName • $address',
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
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close, size: 18),
                  ),
                ],
              ),
              const SizedBox(height: 10),

              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: AppTheme.accentError.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: AppTheme.accentError.withValues(alpha: 0.4),
                    ),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.error_outline,
                        color: AppTheme.accentError,
                        size: 16,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(
                            color: AppTheme.accentError,
                            fontSize: 11,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
              ],

              if (_isRemovalOrder)
                Expanded(child: _buildRemovalFlow())
              else ...[
                _buildStepSelectorBar(),
                const SizedBox(height: 12),
                Expanded(
                  child: SingleChildScrollView(
                    child: _buildActiveTechnicalStep(),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStepSelectorBar() {
    final steps = [
      (0, '1. Auto-Find OLT', Icons.radar_outlined),
      (1, '2. Provisionar', Icons.bolt_outlined),
      (2, '3. FreeRADIUS', Icons.fact_check_outlined),
      (3, '4. Laudo Final', Icons.verified_outlined),
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: steps.map((item) {
          final idx = item.$1;
          final label = item.$2;
          final selected = _currentStep == idx;
          return Padding(
            padding: const EdgeInsets.only(right: 8.0),
            child: ChoiceChip(
              selected: selected,
              label: Text(
                label,
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: selected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
              selectedColor: AppTheme.primaryBlue.withValues(alpha: 0.25),
              onSelected: (_) => setState(() => _currentStep = idx),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildActiveTechnicalStep() {
    switch (_currentStep) {
      case 0:
        return _buildStepAutoFindOlt();
      case 1:
        return _buildStepProvisionOnu();
      case 2:
        return _buildStepRadiusValidation();
      case 3:
      default:
        return _buildStepFinalReport();
    }
  }

  // ===========================================================================
  // ETAPA 1: AUTO-FIND OLT
  // ===========================================================================
  Widget _buildStepAutoFindOlt() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: AppTheme.darkSurface,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: AppTheme.darkBorder),
          ),
          child: Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Descoberta Automática de ONUs (Auto-Find PON)',
                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                    ),
                    SizedBox(height: 2),
                    Text(
                      'Varre as portas PON da OLT em busca de equipamentos conectados aguardando autorização.',
                      style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton.icon(
                onPressed: _isScanningOlt ? null : _scanUnprovisionedOnus,
                icon: _isScanningOlt
                    ? const SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.refresh, size: 16),
                label: const Text('Buscar ONUs na OLT', style: TextStyle(fontSize: 12)),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        if (_isScanningOlt)
          const Padding(
            padding: EdgeInsets.all(24.0),
            child: Center(child: CircularProgressIndicator()),
          )
        else if (_discoveredOnus.isEmpty)
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Column(
              children: [
                const Icon(
                  Icons.sensors_off_outlined,
                  color: AppTheme.textSecondary,
                  size: 28,
                ),
                const SizedBox(height: 8),
                Text(
                  _oltScanMessage ??
                      'Nenhuma ONU não provisionada detectada no momento.',
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 10),
                TextButton.icon(
                  onPressed: () => setState(() => _currentStep = 1),
                  icon: const Icon(Icons.edit_note, size: 16),
                  label: const Text('Informar Serial Manualmente na Etapa 2'),
                ),
              ],
            ),
          )
        else
          ..._discoveredOnus.map((onu) {
            final signalColor = onu.isSignalCritical
                ? AppTheme.accentError
                : (onu.isSignalOptimal
                    ? AppTheme.accentGreen
                    : AppTheme.accentWarning);
            return Container(
              margin: const EdgeInsets.only(bottom: 8),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.darkSurface,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.darkBorder),
              ),
              child: Row(
                children: [
                  const Icon(Icons.router, color: AppTheme.primaryBlue, size: 22),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Wrap(
                          spacing: 8,
                          runSpacing: 4,
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            Text(
                              'SN: ${onu.onuSerial}',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 13,
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 6,
                                vertical: 2,
                              ),
                              decoration: BoxDecoration(
                                color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                '${onu.oltName} • ${onu.displayPon}',
                                style: const TextStyle(
                                  color: AppTheme.primaryBlue,
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            if (onu.rxPowerDbm != null)
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 6,
                                  vertical: 2,
                                ),
                                decoration: BoxDecoration(
                                  color: signalColor.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  '${onu.rxPowerDbm!.toStringAsFixed(2)} dBm',
                                  style: TextStyle(
                                    color: signalColor,
                                    fontSize: 10,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                          ],
                        ),
                        if (onu.onuMac != null && onu.onuMac!.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 3),
                            child: Text(
                              'MAC: ${onu.onuMac}',
                              style: const TextStyle(
                                color: AppTheme.textSecondary,
                                fontSize: 11,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton.icon(
                    onPressed: () => _selectDiscoveredOnu(onu),
                    icon: const Icon(Icons.check_circle_outline, size: 15),
                    label: const Text('Usar ONU', style: TextStyle(fontSize: 12)),
                  ),
                ],
              ),
            );
          }),
      ],
    );
  }

  // ===========================================================================
  // ETAPA 2: PROVISIONAMENTO 1-CLIQUE
  // ===========================================================================
  Widget _buildStepProvisionOnu() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Provisionamento em 1-Clique na OLT',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
        ),
        const SizedBox(height: 4),
        const Text(
          'Autoriza o Serial PON na OLT e aplica o profile de VLAN e credenciais PPPoE do contrato.',
          style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              flex: 2,
              child: TextField(
                controller: _serialCtrl,
                decoration: const InputDecoration(
                  labelText: 'Serial PON da ONU *',
                  hintText: 'Ex: ZTEGC89A12B3',
                  isDense: true,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _vlanCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'VLAN ID',
                  hintText: '100',
                  isDense: true,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _pppoeUserCtrl,
                decoration: const InputDecoration(
                  labelText: 'Usuário PPPoE (Opcional)',
                  hintText: 'cliente@provedor.com.br',
                  isDense: true,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _pppoePassCtrl,
                decoration: const InputDecoration(
                  labelText: 'Senha PPPoE (Opcional)',
                  isDense: true,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Wrap(
          spacing: 10,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          children: [
            ElevatedButton.icon(
              onPressed: _isProvisioning ? null : _provisionOnu,
              icon: _isProvisioning
                  ? const SizedBox(
                      width: 15,
                      height: 15,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.bolt, size: 16),
              label: const Text('Provisionar ONU na OLT'),
            ),
            OutlinedButton.icon(
              onPressed: () {
                setState(() => _currentStep = 2);
                _checkRadiusStatus();
              },
              icon: const Icon(Icons.arrow_forward, size: 16),
              label: const Text('Ir para Validação RADIUS'),
            ),
          ],
        ),
        if (_provisionResult != null) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: (_provisionResult!.success
                      ? AppTheme.accentGreen
                      : AppTheme.accentError)
                  .withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                color: (_provisionResult!.success
                        ? AppTheme.accentGreen
                        : AppTheme.accentError)
                    .withValues(alpha: 0.4),
              ),
            ),
            child: Row(
              children: [
                Icon(
                  _provisionResult!.success
                      ? Icons.check_circle
                      : Icons.error_outline,
                  color: _provisionResult!.success
                      ? AppTheme.accentGreen
                      : AppTheme.accentError,
                  size: 18,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _provisionResult!.message,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  // ===========================================================================
  // ETAPA 3: VALIDAÇÃO DE SESSÃO PPPOE / FREERADIUS
  // ===========================================================================
  Widget _buildStepRadiusValidation() {
    final status = _radiusStatus;
    final isOnline = status?.authenticated == true;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Diagnóstico de Sessão PPPoE no FreeRADIUS',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                  ),
                  SizedBox(height: 2),
                  Text(
                    'Confirma se o roteador/ONU autenticou no concentrador BNG/BRAS antes de encerrar a O.S.',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            ElevatedButton.icon(
              onPressed: _isCheckingRadius ? null : _checkRadiusStatus,
              icon: _isCheckingRadius
                  ? const SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.network_check, size: 16),
              label: const Text('Validar Sessão RADIUS', style: TextStyle(fontSize: 12)),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (status == null && !_isCheckingRadius)
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: const Text(
              'Clique em "Validar Sessão RADIUS" para consultar a tabela radacct do FreeRADIUS.',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
              textAlign: TextAlign.center,
            ),
          )
        else if (status != null)
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: (isOnline ? AppTheme.accentGreen : AppTheme.accentWarning)
                  .withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(
                color: (isOnline ? AppTheme.accentGreen : AppTheme.accentWarning)
                    .withValues(alpha: 0.4),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(
                      isOnline ? Icons.wifi_tethering : Icons.wifi_off,
                      color: isOnline
                          ? AppTheme.accentGreen
                          : AppTheme.accentWarning,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        isOnline
                            ? 'AUTENTICADO ONLINE NO FREERADIUS'
                            : 'AGUARDANDO SESSÃO PPPOE',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                          color: isOnline
                              ? AppTheme.accentGreen
                              : AppTheme.accentWarning,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 16,
                  runSpacing: 8,
                  children: [
                    _buildTelemetryBadge('Login PPPoE', status.username ?? 'N/D'),
                    _buildTelemetryBadge('IP Atribuído', status.framedIpAddress ?? 'N/D'),
                    _buildTelemetryBadge('MAC Address', status.macAddress ?? 'N/D'),
                    _buildTelemetryBadge('BNG / NAS IP', status.nasIpAddress ?? 'N/D'),
                    _buildTelemetryBadge('Uptime', status.formattedUptime),
                  ],
                ),
              ],
            ),
          ),
        const SizedBox(height: 14),
        Align(
          alignment: Alignment.centerRight,
          child: OutlinedButton.icon(
            onPressed: () => setState(() => _currentStep = 3),
            icon: const Icon(Icons.arrow_forward, size: 16),
            label: const Text('Avançar para Laudo & Assinatura'),
          ),
        ),
      ],
    );
  }

  Widget _buildTelemetryBadge(String label, String value) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppTheme.darkBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(color: AppTheme.textSecondary, fontSize: 10),
          ),
          const SizedBox(height: 2),
          Text(
            value,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
          ),
        ],
      ),
    );
  }

  // ===========================================================================
  // ETAPA 4: LAUDO FINAL & ASSINATURA DO RECEBEDOR
  // ===========================================================================
  Widget _buildStepFinalReport() {
    final parsedSignal =
        double.tryParse(_signalCtrl.text.trim().replaceAll(',', '.'));
    final isSignalBad = parsedSignal != null && parsedSignal < -27.0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Laudo Técnico & Encerramento da O.S.',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _serialCtrl,
                decoration: const InputDecoration(
                  labelText: 'Serial da ONU *',
                  isDense: true,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _macCtrl,
                decoration: const InputDecoration(
                  labelText: 'MAC da ONU',
                  hintText: '48:57:02:11:22:33',
                  isDense: true,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _signalCtrl,
                onChanged: (_) => setState(() {}),
                keyboardType: const TextInputType.numberWithOptions(
                  signed: true,
                  decimal: true,
                ),
                decoration: InputDecoration(
                  labelText: 'Sinal Óptico (dBm) *',
                  isDense: true,
                  helperText: isSignalBad
                      ? 'Atenção: Sinal fora do padrão (<-27 dBm)'
                      : 'Faixa ideal: -15.0 a -25.0 dBm',
                  helperStyle: TextStyle(
                    color: isSignalBad
                        ? AppTheme.accentError
                        : AppTheme.textSecondary,
                    fontSize: 10,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _signatureNameCtrl,
                decoration: const InputDecoration(
                  labelText: 'Nome Legível do Recebedor *',
                  hintText: 'Quem acompanhou a O.S.',
                  isDense: true,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        TextField(
          controller: _notesCtrl,
          maxLines: 2,
          decoration: const InputDecoration(
            labelText: 'Observações Técnicas do Atendimento',
            isDense: true,
          ),
        ),
        const SizedBox(height: 14),
        ElevatedButton.icon(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.accentGreen,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 12),
          ),
          onPressed: _isCompleting ? null : _completeFieldExecution,
          icon: _isCompleting
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white,
                  ),
                )
              : const Icon(Icons.verified_outlined, size: 18),
          label: const Text(
            'Concluir O.S. e Ativar Contrato',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
      ],
    );
  }

  // ===========================================================================
  // FLUXO B: LOGÍSTICA REVERSA EM CAMPO (O.S. RETIRADA)
  // ===========================================================================
  Widget _buildRemovalFlow() {
    if (_legalCollectionResult != null) {
      final lc = _legalCollectionResult!;
      return SingleChildScrollView(
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppTheme.accentError.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: AppTheme.accentError.withValues(alpha: 0.4),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Row(
                children: [
                  Icon(Icons.gavel_outlined, color: AppTheme.accentError, size: 22),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Retirada Infrutífera Registrada • Cobrança Jurídica Acionada',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                        color: AppTheme.accentError,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                'Status do Processo: ${lc.status}',
                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 6),
              Text(
                'Indenização do Equipamento: R\$ ${lc.equipmentIndemnityAmount.toStringAsFixed(2)} • Dívida Total Acumulada: R\$ ${lc.totalClaimAmount.toStringAsFixed(2)}',
                style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 14),
              ElevatedButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('Fechar'),
              ),
            ],
          ),
        ),
      );
    }

    final reasons = [
      'Cliente ausente e imóvel desocupado',
      'Recusa expressa de devolução do comodato',
      'Endereço não localizado / mudança sem aviso',
      'Equipamento extraviado ou danificado pelo cliente',
    ];

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Wrap(
            spacing: 10,
            runSpacing: 8,
            children: [
              ChoiceChip(
                selected: !_isRemovalFailMode,
                label: const Text('Equipamento Recolhido (Devolver ao Estoque)'),
                selectedColor: AppTheme.accentGreen.withValues(alpha: 0.25),
                onSelected: (_) => setState(() => _isRemovalFailMode = false),
              ),
              ChoiceChip(
                selected: _isRemovalFailMode,
                label: const Text('Retirada Infrutífera (Acionar SPC / Jurídico)'),
                selectedColor: AppTheme.accentError.withValues(alpha: 0.25),
                onSelected: (_) => setState(() => _isRemovalFailMode = true),
              ),
            ],
          ),
          const SizedBox(height: 14),
          if (!_isRemovalFailMode) ...[
            DropdownButtonFormField<String>(
              isExpanded: true,
              initialValue: _selectedWarehouseId,
              decoration: const InputDecoration(
                labelText: 'Almoxarifado de Destino para Devolução *',
                isDense: true,
              ),
              items: _warehouses
                  .map(
                    (w) => DropdownMenuItem<String>(
                      value: w.id,
                      child: Text(
                        '${w.name} (${w.code})',
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  )
                  .toList(),
              onChanged: (val) => setState(() => _selectedWarehouseId = val),
            ),
          ] else ...[
            DropdownButtonFormField<String>(
              isExpanded: true,
              initialValue: _selectedUnsuccessReason,
              decoration: const InputDecoration(
                labelText: 'Motivo da Retirada Infrutífera *',
                isDense: true,
              ),
              items: reasons
                  .map(
                    (r) => DropdownMenuItem<String>(
                      value: r,
                      child: Text(r, overflow: TextOverflow.ellipsis),
                    ),
                  )
                  .toList(),
              onChanged: (val) {
                if (val != null) setState(() => _selectedUnsuccessReason = val);
              },
            ),
          ],
          const SizedBox(height: 10),
          TextField(
            controller: _removalPhotoCtrl,
            decoration: const InputDecoration(
              labelText: 'URL da Foto Comprobatória (Termo ou Fachada)',
              hintText: 'https://...',
              isDense: true,
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _removalNotesCtrl,
            maxLines: 2,
            decoration: const InputDecoration(
              labelText: 'Observações da Diligência em Campo',
              isDense: true,
            ),
          ),
          const SizedBox(height: 14),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: _isRemovalFailMode
                  ? AppTheme.accentError
                  : AppTheme.accentGreen,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 12),
            ),
            onPressed: _isSubmittingRemoval ? null : _submitEquipmentRemoval,
            icon: _isSubmittingRemoval
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : Icon(
                    _isRemovalFailMode
                        ? Icons.gavel_outlined
                        : Icons.inventory_2_outlined,
                    size: 18,
                  ),
            label: Text(
              _isRemovalFailMode
                  ? 'Registrar Retirada Infrutífera & Gerar Cobrança SPC'
                  : 'Confirmar Devolução do Equipamento ao Almoxarifado',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }
}
