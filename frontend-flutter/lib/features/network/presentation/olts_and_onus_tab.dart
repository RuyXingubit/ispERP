import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/network_ftth_repository.dart';

/// Aba 2: Gestão de OLTs / Concentradores (`/network-devices`) e Provisionamento/Telemetria de ONUs (`/onus`).
class OltsAndOnusTab extends ConsumerStatefulWidget {
  const OltsAndOnusTab({super.key});

  @override
  ConsumerState<OltsAndOnusTab> createState() => _OltsAndOnusTabState();
}

class _OltsAndOnusTabState extends ConsumerState<OltsAndOnusTab> {
  String _onuSearch = '';

  void _showCreateDeviceDialog() {
    final nameCtrl = TextEditingController();
    final ipCtrl = TextEditingController();
    final portCtrl = TextEditingController(text: '443');
    final tokenCtrl = TextEditingController();
    final snmpCtrl = TextEditingController();
    String deviceType = 'OLT';
    String driverType = 'SMARTOLT';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Cadastrar OLT / Dispositivo de Rede'),
          content: SizedBox(
            width: 480,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: nameCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Nome do Equipamento *',
                      hintText: 'Ex: OLT Central Huawei MA5800-X7',
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          initialValue: deviceType,
                          decoration: const InputDecoration(labelText: 'Tipo'),
                          items: const [
                            DropdownMenuItem(value: 'OLT', child: Text('OLT GPON/XGS-PON')),
                            DropdownMenuItem(value: 'BNG', child: Text('BNG / Concentrador')),
                            DropdownMenuItem(value: 'SWITCH', child: Text('Switch Core')),
                          ],
                          onChanged: (v) {
                            if (v != null) setModalState(() => deviceType = v);
                          },
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          initialValue: driverType,
                          decoration: const InputDecoration(labelText: 'Driver de Integração *'),
                          items: const [
                            DropdownMenuItem(value: 'SMARTOLT', child: Text('SMARTOLT')),
                            DropdownMenuItem(value: 'MIKROTIK_ROUTEROS', child: Text('MIKROTIK_ROUTEROS')),
                            DropdownMenuItem(value: 'RADIUS', child: Text('RADIUS')),
                            DropdownMenuItem(value: 'EXTERNAL_MICROSERVICE', child: Text('EXTERNAL_MICROSERVICE')),
                            DropdownMenuItem(value: 'MOCK', child: Text('MOCK')),
                          ],
                          onChanged: (v) {
                            if (v != null) setModalState(() => driverType = v);
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        flex: 3,
                        child: TextField(
                          controller: ipCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Endereço IP / Hostname *',
                            hintText: 'Ex: 10.0.10.2',
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        flex: 1,
                        child: TextField(
                          controller: portCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Porta API'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: tokenCtrl,
                    obscureText: true,
                    decoration: const InputDecoration(
                      labelText: 'Token Privado API (Armazenado criptografado)',
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: snmpCtrl,
                    decoration: const InputDecoration(
                      labelText: 'SNMP Community v2c (Telemetria PON)',
                      hintText: 'Ex: isperp_ro_snmp',
                    ),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                final name = nameCtrl.text.trim();
                final ip = ipCtrl.text.trim();
                if (name.isEmpty || ip.isEmpty) return;
                try {
                  await ref.read(networkFtthRepositoryProvider).createDevice(
                        name: name,
                        deviceType: deviceType,
                        driverType: driverType,
                        ipAddress: ip,
                        apiPort: int.tryParse(portCtrl.text.trim()) ?? 443,
                        apiToken: tokenCtrl.text.trim(),
                        snmpCommunity: snmpCtrl.text.trim(),
                      );
                  ref.invalidate(networkDevicesProvider);
                  ref.invalidate(nocMonitoringSummaryProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content: Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Salvar Equipamento'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _handleDiagnoseOnu(OnuModel onu) async {
    try {
      final diag =
          await ref.read(networkFtthRepositoryProvider).diagnoseOnu(onu.id);
      if (!mounted) return;
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Row(
            children: [
              const Icon(Icons.troubleshoot, color: AppTheme.primaryBlue),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Diagnóstico Óptico — ${diag.onuSerial}',
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          content: SizedBox(
            width: 440,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildDiagRow('Status na OLT', diag.status),
                _buildDiagRow('MAC Address', diag.onuMac),
                _buildDiagRow(
                  'Potência RX (ONU)',
                  diag.rxPowerDbm != null
                      ? '${diag.rxPowerDbm!.toStringAsFixed(2)} dBm'
                      : 'N/D',
                ),
                _buildDiagRow(
                  'Potência TX (ONU)',
                  diag.txPowerDbm != null
                      ? '${diag.txPowerDbm!.toStringAsFixed(2)} dBm'
                      : 'N/D',
                ),
                if (diag.oltName != null)
                  _buildDiagRow(
                    'OLT / Porta PON',
                    '${diag.oltName} (PON ${diag.ponPort ?? "-"})',
                  ),
                if (diag.details != null && diag.details!.isNotEmpty) ...[
                  const Divider(height: 24),
                  Text(
                    diag.details!,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppTheme.accentGreen,
                    ),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            ElevatedButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Fechar'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  Future<void> _handleToggleOnuBlock(OnuModel onu) async {
    final isBlocked = onu.status == 'BLOCKED';
    try {
      if (isBlocked) {
        await ref
            .read(networkFtthRepositoryProvider)
            .unblockOnuByContract(onu.contractId);
      } else {
        await ref
            .read(networkFtthRepositoryProvider)
            .blockOnuByContract(onu.contractId, reason: 'Bloqueio Técnico/NOC');
      }
      ref.invalidate(onusListProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(isBlocked
                ? 'ONU ${onu.onuSerial} desbloqueada na OLT.'
                : 'ONU ${onu.onuSerial} bloqueada na OLT.'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  Widget _buildDiagRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label,
              style:
                  const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final devicesAsync = ref.watch(networkDevicesProvider);
    final onusAsync = ref.watch(onusListProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 1. Seção OLTs & Dispositivos de Rede
          Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'OLTs & Dispositivos de Rede Ativos',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Inventário de OLTs GPON/XGS-PON e concentradores com credenciais de API/SNMP blindadas',
                      style:
                          TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
              ),
              ElevatedButton.icon(
                onPressed: _showCreateDeviceDialog,
                icon: const Icon(Icons.add, size: 18),
                label: const Text('Nova OLT / Dispositivo'),
              ),
            ],
          ),
          const SizedBox(height: 14),

          devicesAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (devices) {
              if (devices.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Text(
                      'Nenhuma OLT ou dispositivo cadastrado. Clique em "Nova OLT / Dispositivo" para adicionar.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }
              return Wrap(
                spacing: 12,
                runSpacing: 12,
                children: devices.map((dev) {
                  return Container(
                    width: 280,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppTheme.darkCard),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.dns_outlined,
                              size: 18,
                              color: dev.active
                                  ? AppTheme.accentGreen
                                  : AppTheme.textSecondary,
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                dev.name,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold, fontSize: 14),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          '${dev.deviceType} • Driver: ${dev.driverType}',
                          style: const TextStyle(
                              fontSize: 12, color: AppTheme.primaryBlue),
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'IP: ${dev.ipAddress}:${dev.apiPort}',
                          style: const TextStyle(
                              fontSize: 12, color: AppTheme.textSecondary),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          ),
          const SizedBox(height: 28),

          // 2. Seção ONUs Provisionadas & Telemetria
          Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'ONUs Provisionadas & Telemetria Óptica',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Diagnóstico em tempo real de potência RX/TX dBm, VLAN, perfil de velocidade e bloqueio/desbloqueio na OLT',
                      style:
                          TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
              ),
              SizedBox(
                width: 260,
                child: TextField(
                  onChanged: (v) => setState(() => _onuSearch = v.trim()),
                  decoration: const InputDecoration(
                    prefixIcon: Icon(Icons.search, size: 18),
                    hintText: 'Filtrar Serial, MAC ou PPPoE...',
                    isDense: true,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),

          onusAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (onus) {
              final filtered = _onuSearch.isEmpty
                  ? onus
                  : onus.where((o) {
                      final q = _onuSearch.toLowerCase();
                      return o.onuSerial.toLowerCase().contains(q) ||
                          o.onuMac.toLowerCase().contains(q) ||
                          (o.pppoeUser?.toLowerCase().contains(q) ?? false);
                    }).toList();

              if (filtered.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Text(
                      'Nenhuma ONU provisionada encontrada para o filtro informado.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }

              return Column(
                children: filtered.map((onu) {
                  final isBlocked = onu.status == 'BLOCKED';
                  final rx = onu.rxPowerDbm;
                  final signalCritical = rx != null && rx < -27.0;

                  return Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          Icon(
                            Icons.router_outlined,
                            color: isBlocked
                                ? AppTheme.accentError
                                : signalCritical
                                    ? AppTheme.accentWarning
                                    : AppTheme.accentGreen,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Wrap(
                                  spacing: 10,
                                  runSpacing: 4,
                                  crossAxisAlignment: WrapCrossAlignment.center,
                                  children: [
                                    Text(
                                      'SN: ${onu.onuSerial}',
                                      style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 14),
                                    ),
                                    Text(
                                      'MAC: ${onu.onuMac}',
                                      style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary),
                                    ),
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                          horizontal: 8, vertical: 2),
                                      decoration: BoxDecoration(
                                        color: (isBlocked
                                                ? AppTheme.accentError
                                                : AppTheme.accentGreen)
                                            .withValues(alpha: 0.15),
                                        borderRadius: BorderRadius.circular(6),
                                      ),
                                      child: Text(
                                        onu.status,
                                        style: TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.bold,
                                          color: isBlocked
                                              ? AppTheme.accentError
                                              : AppTheme.accentGreen,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Wrap(
                                  spacing: 14,
                                  runSpacing: 4,
                                  children: [
                                    Text(
                                      'PPPoE: ${onu.pppoeUser ?? "-"}',
                                      style: const TextStyle(fontSize: 12),
                                    ),
                                    Text(
                                      'VLAN: ${onu.vlanId}',
                                      style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary),
                                    ),
                                    Text(
                                      'Plano: ↓${onu.downloadSpeed} / ↑${onu.uploadSpeed} Mbps',
                                      style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.primaryBlue),
                                    ),
                                    Text(
                                      'Sinal RX: ${rx != null ? "${rx.toStringAsFixed(2)} dBm" : "N/D"}',
                                      style: TextStyle(
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                        color: signalCritical
                                            ? AppTheme.accentError
                                            : AppTheme.accentGreen,
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 8),
                          Wrap(
                            spacing: 8,
                            children: [
                              OutlinedButton.icon(
                                onPressed: () => _handleDiagnoseOnu(onu),
                                icon: const Icon(Icons.troubleshoot, size: 16),
                                label: const Text('Diagnosticar'),
                              ),
                              ElevatedButton.icon(
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: isBlocked
                                      ? AppTheme.accentGreen
                                      : AppTheme.accentError,
                                ),
                                onPressed: () => _handleToggleOnuBlock(onu),
                                icon: Icon(
                                  isBlocked ? Icons.lock_open : Icons.block,
                                  size: 16,
                                ),
                                label: Text(
                                    isBlocked ? 'Desbloquear' : 'Bloquear'),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              );
            },
          ),
        ],
      ),
    );
  }
}
