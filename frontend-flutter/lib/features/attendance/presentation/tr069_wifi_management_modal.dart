import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/attendance_models.dart';
import '../data/attendance_repository.dart';

/// Modal de Gerência Remota de Wi-Fi Dual-Band, Diagnóstico e Reboot de ONU/CPE via TR-069 / CWMP (Onda M).
class Tr069WifiManagementModal extends ConsumerStatefulWidget {
  final ContractSummary contract;
  final String customerName;

  const Tr069WifiManagementModal({
    super.key,
    required this.contract,
    required this.customerName,
  });

  static Future<void> show(
    BuildContext context, {
    required ContractSummary contract,
    required String customerName,
  }) {
    return showDialog<void>(
      context: context,
      builder: (_) => Tr069WifiManagementModal(
        contract: contract,
        customerName: customerName,
      ),
    );
  }

  @override
  ConsumerState<Tr069WifiManagementModal> createState() => _Tr069WifiManagementModalState();
}

class _Tr069WifiManagementModalState extends ConsumerState<Tr069WifiManagementModal> {
  final _ssid24Controller = TextEditingController();
  final _ssid5Controller = TextEditingController();
  final _passwordController = TextEditingController();

  String _channel24 = 'AUTO';
  String _channel5 = 'AUTO';
  bool _bandSteering = true;

  Tr069CpeStatusModel? _status;
  bool _loading = true;
  bool _submitting = false;
  String? _errorMessage;
  String? _feedbackBanner;

  static const _channels24 = ['AUTO', '1', '6', '11'];
  static const _channels5 = ['AUTO', '36', '44', '149'];

  @override
  void initState() {
    super.initState();
    _loadCpeStatus();
  }

  @override
  void dispose() {
    _ssid24Controller.dispose();
    _ssid5Controller.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _loadCpeStatus() async {
    setState(() {
      _loading = true;
      _errorMessage = null;
    });
    try {
      final repo = ref.read(attendanceRepositoryProvider);
      final status = await repo.getTr069CpeStatus(widget.contract.id);
      if (!mounted) return;
      _applyStatusToForm(status);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _errorMessage = 'Falha ao consultar CPE via TR-069 / ACS: $e';
      });
    }
  }

  void _applyStatusToForm(Tr069CpeStatusModel status) {
    _ssid24Controller.text = status.ssid24ghz;
    _ssid5Controller.text = status.ssid5ghz;
    _channel24 = _channels24.contains(status.channel24ghz) ? status.channel24ghz : 'AUTO';
    _channel5 = _channels5.contains(status.channel5ghz) ? status.channel5ghz : 'AUTO';
    _bandSteering = status.bandSteeringEnabled;
    setState(() {
      _status = status;
      _loading = false;
    });
  }

  Future<void> _saveWifiConfig() async {
    final ssid24 = _ssid24Controller.text.trim();
    final ssid5 = _ssid5Controller.text.trim();
    final password = _passwordController.text.trim();

    if (ssid24.isEmpty || ssid5.isEmpty) {
      setState(() {
        _errorMessage = 'Os nomes das redes 2.4 GHz e 5 GHz (SSID) são obrigatórios.';
      });
      return;
    }
    if (password.isNotEmpty && password.length < 8) {
      setState(() {
        _errorMessage = 'A senha WPA2/WPA3 deve conter no mínimo 8 caracteres.';
      });
      return;
    }

    setState(() {
      _submitting = true;
      _errorMessage = null;
      _feedbackBanner = null;
    });

    try {
      final repo = ref.read(attendanceRepositoryProvider);
      final updated = await repo.updateTr069Wifi(
        contractId: widget.contract.id,
        ssid24ghz: ssid24,
        ssid5ghz: ssid5,
        password: password.isNotEmpty ? password : null,
        channel24ghz: _channel24,
        channel5ghz: _channel5,
        bandSteeringEnabled: _bandSteering,
      );
      if (!mounted) return;
      _passwordController.clear();
      _applyStatusToForm(updated);
      setState(() {
        _submitting = false;
        _feedbackBanner =
            'SetParameterValues aplicado na CPE ${updated.deviceId} (${updated.acsSyncStatus}).';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _submitting = false;
        _errorMessage = 'Erro ao aplicar parâmetros Wi-Fi TR-069: $e';
      });
    }
  }

  Future<void> _triggerRemoteReboot() async {
    setState(() {
      _submitting = true;
      _errorMessage = null;
      _feedbackBanner = null;
    });
    try {
      final repo = ref.read(attendanceRepositoryProvider);
      final log = await repo.rebootTr069Cpe(widget.contract.id);
      final refreshed = await repo.getTr069CpeStatus(widget.contract.id);
      if (!mounted) return;
      _applyStatusToForm(refreshed);
      setState(() {
        _submitting = false;
        _feedbackBanner = '${log.commandType}: ${log.summary}';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _submitting = false;
        _errorMessage = 'Falha ao despachar Reboot TR-069: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final status = _status;

    return Dialog(
      backgroundColor: AppTheme.darkBg,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 760, maxHeight: 680),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Header
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(Icons.wifi_tethering_rounded, color: AppTheme.primaryBlue, size: 22),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Gerência Remota Wi-Fi Dual-Band & ONU (TR-069 / CWMP)',
                          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          'Assinante: ${widget.customerName} • Contrato ${widget.contract.contractNumber}',
                          style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: _submitting ? null : _loadCpeStatus,
                    tooltip: 'Sincronizar Inform ACS',
                    icon: const Icon(Icons.sync_rounded, size: 20),
                  ),
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close, size: 20),
                  ),
                ],
              ),
              const Divider(height: 22, color: AppTheme.darkBorder),

              if (_errorMessage != null)
                Container(
                  margin: const EdgeInsets.only(bottom: 12),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppTheme.accentError.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.accentError.withValues(alpha: 0.4)),
                  ),
                  child: Text(
                    _errorMessage!,
                    style: const TextStyle(color: AppTheme.accentError, fontSize: 12),
                  ),
                ),

              if (_feedbackBanner != null)
                Container(
                  margin: const EdgeInsets.only(bottom: 12),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppTheme.accentGreen.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.4)),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle_outline, color: AppTheme.accentGreen, size: 18),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _feedbackBanner!,
                          style: const TextStyle(color: AppTheme.accentGreen, fontSize: 12),
                        ),
                      ),
                    ],
                  ),
                ),

              Expanded(
                child: _loading
                    ? const Center(child: CircularProgressIndicator())
                    : status == null
                        ? const Center(
                            child: Text(
                              'Nenhuma sessão TR-069 encontrada para este contrato.',
                              style: TextStyle(color: AppTheme.textSecondary),
                            ),
                          )
                        : SingleChildScrollView(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                // 1. Telemetria & Identificação da CPE
                                Container(
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: AppTheme.darkSurface,
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: AppTheme.darkBorder),
                                  ),
                                  child: Wrap(
                                    spacing: 16,
                                    runSpacing: 10,
                                    alignment: WrapAlignment.spaceBetween,
                                    crossAxisAlignment: WrapCrossAlignment.center,
                                    children: [
                                      _buildMetricChip('CPE Device ID', status.deviceId),
                                      _buildMetricChip('Modelo / Firmware', '${status.modelName} (${status.firmwareVersion})'),
                                      _buildMetricChip('Uptime CPE', status.formattedUptime),
                                      _buildMetricChip('Status ACS', status.acsSyncStatus),
                                      OutlinedButton.icon(
                                        onPressed: _submitting ? null : _triggerRemoteReboot,
                                        style: OutlinedButton.styleFrom(
                                          foregroundColor: AppTheme.accentWarning,
                                          side: BorderSide(color: AppTheme.accentWarning.withValues(alpha: 0.6)),
                                        ),
                                        icon: const Icon(Icons.restart_alt_rounded, size: 16),
                                        label: const Text('Reboot Remoto ONU', style: TextStyle(fontSize: 12)),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(height: 14),

                                // 2. Formulário Dual-Band Wi-Fi (2.4 GHz & 5 GHz)
                                Container(
                                  padding: const EdgeInsets.all(14),
                                  decoration: BoxDecoration(
                                    color: AppTheme.darkSurface,
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: AppTheme.darkBorder),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'Parâmetros WLAN Dual-Band (CWMP SetParameterValues)',
                                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                      ),
                                      const SizedBox(height: 12),
                                      Row(
                                        children: [
                                          Expanded(
                                            flex: 2,
                                            child: TextField(
                                              controller: _ssid24Controller,
                                              decoration: const InputDecoration(
                                                labelText: 'SSID 2.4 GHz',
                                                isDense: true,
                                                border: OutlineInputBorder(),
                                              ),
                                            ),
                                          ),
                                          const SizedBox(width: 10),
                                          Expanded(
                                            child: DropdownButtonFormField<String>(
                                              initialValue: _channel24,
                                              isExpanded: true,
                                              decoration: const InputDecoration(
                                                labelText: 'Canal 2.4 GHz',
                                                isDense: true,
                                                border: OutlineInputBorder(),
                                              ),
                                              items: _channels24
                                                  .map((c) => DropdownMenuItem(
                                                        value: c,
                                                        child: Text('Canal $c', overflow: TextOverflow.ellipsis),
                                                      ))
                                                  .toList(),
                                              onChanged: (val) {
                                                if (val != null) setState(() => _channel24 = val);
                                              },
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
                                              controller: _ssid5Controller,
                                              decoration: const InputDecoration(
                                                labelText: 'SSID 5 GHz',
                                                isDense: true,
                                                border: OutlineInputBorder(),
                                              ),
                                            ),
                                          ),
                                          const SizedBox(width: 10),
                                          Expanded(
                                            child: DropdownButtonFormField<String>(
                                              initialValue: _channel5,
                                              isExpanded: true,
                                              decoration: const InputDecoration(
                                                labelText: 'Canal 5 GHz',
                                                isDense: true,
                                                border: OutlineInputBorder(),
                                              ),
                                              items: _channels5
                                                  .map((c) => DropdownMenuItem(
                                                        value: c,
                                                        child: Text('Canal $c', overflow: TextOverflow.ellipsis),
                                                      ))
                                                  .toList(),
                                              onChanged: (val) {
                                                if (val != null) setState(() => _channel5 = val);
                                              },
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 10),
                                      Row(
                                        children: [
                                          Expanded(
                                            child: TextField(
                                              controller: _passwordController,
                                              obscureText: true,
                                              decoration: const InputDecoration(
                                                labelText: 'Nova Senha WPA2/WPA3 (mín. 8 dígitos — em branco mantém a atual)',
                                                isDense: true,
                                                border: OutlineInputBorder(),
                                              ),
                                            ),
                                          ),
                                          const SizedBox(width: 12),
                                          Flexible(
                                            child: Row(
                                              mainAxisSize: MainAxisSize.min,
                                              children: [
                                                Switch(
                                                  value: _bandSteering,
                                                  onChanged: (v) => setState(() => _bandSteering = v),
                                                ),
                                                const Flexible(
                                                  child: Text(
                                                    'Band Steering (Unificar 2.4/5G)',
                                                    style: TextStyle(fontSize: 12),
                                                    overflow: TextOverflow.ellipsis,
                                                  ),
                                                ),
                                              ],
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 12),
                                      Align(
                                        alignment: Alignment.centerRight,
                                        child: ElevatedButton.icon(
                                          onPressed: _submitting ? null : _saveWifiConfig,
                                          style: ElevatedButton.styleFrom(
                                            backgroundColor: AppTheme.primaryBlue,
                                            foregroundColor: Colors.white,
                                          ),
                                          icon: const Icon(Icons.save_outlined, size: 16),
                                          label: const Text('Aplicar na ONU via TR-069'),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(height: 14),

                                // 3. Hosts Conectados na LAN/WLAN da ONU
                                Container(
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: AppTheme.darkSurface,
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: AppTheme.darkBorder),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Dispositivos Conectados na LAN/WLAN (${status.connectedHosts.length})',
                                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                                      ),
                                      const SizedBox(height: 8),
                                      if (status.connectedHosts.isEmpty)
                                        const Text(
                                          'Nenhum host ativo reportado no último Inform CWMP.',
                                          style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                                        )
                                      else
                                        Wrap(
                                          spacing: 8,
                                          runSpacing: 6,
                                          children: status.connectedHosts.map((host) {
                                            return Container(
                                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                                              decoration: BoxDecoration(
                                                color: AppTheme.darkBg,
                                                borderRadius: BorderRadius.circular(6),
                                                border: Border.all(color: AppTheme.darkBorder),
                                              ),
                                              child: Text(
                                                '${host.hostName} • ${host.ipAddress} • ${host.band}${host.rssiDbm != null ? " (${host.rssiDbm} dBm)" : ""}',
                                                style: const TextStyle(fontSize: 11),
                                              ),
                                            );
                                          }).toList(),
                                        ),
                                    ],
                                  ),
                                ),
                                const SizedBox(height: 14),

                                // 4. Trilha de Auditoria de Comandos TR-069 (Suporte vs Portal do Assinante)
                                Container(
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: AppTheme.darkSurface,
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: AppTheme.darkBorder),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'Histórico Auditado de Comandos TR-069 / CWMP',
                                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                                      ),
                                      const SizedBox(height: 8),
                                      if (status.recentCommands.isEmpty)
                                        const Text(
                                          'Nenhum comando remoto registrado para esta CPE.',
                                          style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                                        )
                                      else
                                        ...status.recentCommands.take(6).map((cmd) {
                                          final isPortal = cmd.triggeredByRole.toUpperCase().contains('PORTAL') ||
                                              cmd.triggeredByRole.toUpperCase().contains('SUBSCRIBER');
                                          return Padding(
                                            padding: const EdgeInsets.only(bottom: 6),
                                            child: Row(
                                              children: [
                                                Container(
                                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                                  decoration: BoxDecoration(
                                                    color: (isPortal ? AppTheme.accentGreen : AppTheme.primaryBlue)
                                                        .withValues(alpha: 0.16),
                                                    borderRadius: BorderRadius.circular(4),
                                                  ),
                                                  child: Text(
                                                    isPortal ? 'AUTOATENDIMENTO' : 'SUPORTE N1/N2',
                                                    style: TextStyle(
                                                      fontSize: 9,
                                                      fontWeight: FontWeight.bold,
                                                      color: isPortal ? AppTheme.accentGreen : AppTheme.primaryBlue,
                                                    ),
                                                  ),
                                                ),
                                                const SizedBox(width: 8),
                                                Expanded(
                                                  child: Text(
                                                    '${cmd.commandType}: ${cmd.summary}',
                                                    style: const TextStyle(fontSize: 11),
                                                    overflow: TextOverflow.ellipsis,
                                                  ),
                                                ),
                                                const SizedBox(width: 8),
                                                Text(
                                                  cmd.status,
                                                  style: const TextStyle(
                                                    fontSize: 10,
                                                    color: AppTheme.textSecondary,
                                                    fontWeight: FontWeight.w600,
                                                  ),
                                                ),
                                              ],
                                            ),
                                          );
                                        }),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMetricChip(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 10)),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
      ],
    );
  }
}
