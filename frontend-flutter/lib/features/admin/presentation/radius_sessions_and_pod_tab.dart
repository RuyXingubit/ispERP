import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/radius_ipam_marcocivil_repository.dart';

/// Aba 2 de `/admin/nas`: Sessões RADIUS Ativas, Histórico de Conexões e Desconexão PoD/CoA.
class RadiusSessionsAndPodTab extends ConsumerStatefulWidget {
  const RadiusSessionsAndPodTab({super.key});

  @override
  ConsumerState<RadiusSessionsAndPodTab> createState() =>
      _RadiusSessionsAndPodTabState();
}

class _RadiusSessionsAndPodTabState
    extends ConsumerState<RadiusSessionsAndPodTab> {
  String _searchQuery = '';
  final _historyUsernameCtrl = TextEditingController();
  List<RadiusSessionModel>? _historyResults;
  bool _loadingHistory = false;

  @override
  void dispose() {
    _historyUsernameCtrl.dispose();
    super.dispose();
  }

  String _formatBytes(int bytes) {
    if (bytes <= 0) return '0 B';
    const suffixes = ['B', 'KB', 'MB', 'GB', 'TB'];
    var i = 0;
    double size = bytes.toDouble();
    while (size >= 1024 && i < suffixes.length - 1) {
      size /= 1024;
      i++;
    }
    return '${size.toStringAsFixed(2)} ${suffixes[i]}';
  }

  Future<void> _handleDisconnect(RadiusSessionModel session) async {
    try {
      final res = await ref
          .read(radiusIpamMarcoCivilRepositoryProvider)
          .disconnectSession(
            username: session.username,
            nasIpAddress: session.nasIpAddress,
            acctSessionId: session.acctSessionId,
            framedIpAddress: session.framedIpAddress,
          );
      ref.invalidate(radiusActiveSessionsProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(res.message),
            backgroundColor:
                res.success ? AppTheme.accentGreen : AppTheme.accentWarning,
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

  Future<void> _handleLookupHistory([String? prefilledUser]) async {
    final username = (prefilledUser ?? _historyUsernameCtrl.text).trim();
    if (username.isEmpty) return;
    _historyUsernameCtrl.text = username;

    setState(() => _loadingHistory = true);
    try {
      final history = await ref
          .read(radiusIpamMarcoCivilRepositoryProvider)
          .getSessionHistory(username);
      if (mounted) {
        setState(() => _historyResults = history);
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
    } finally {
      if (mounted) setState(() => _loadingHistory = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final activeAsync = ref.watch(radiusActiveSessionsProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Cabeçalho & Busca
          Wrap(
            spacing: 12,
            runSpacing: 12,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Sessões PPPoE/IPoE Ativas no FreeRADIUS & Desconexão PoD',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Monitoramento de túnel em tempo real (IPv4/IPv6 Dual-Stack, MAC e tráfego) e envio de Packet of Disconnect (porta 3799)',
                    style:
                        TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                ],
              ),
              Wrap(
                spacing: 10,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  SizedBox(
                    width: 260,
                    child: TextField(
                      onChanged: (v) => setState(() => _searchQuery = v.trim()),
                      decoration: const InputDecoration(
                        prefixIcon: Icon(Icons.search, size: 18),
                        hintText: 'Filtrar usuário, IP ou MAC...',
                        isDense: true,
                      ),
                    ),
                  ),
                  OutlinedButton.icon(
                    onPressed: () =>
                        ref.invalidate(radiusActiveSessionsProvider),
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('Atualizar Sessões'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Lista de Sessões Ativas
          activeAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (sessions) {
              final filtered = _searchQuery.isEmpty
                  ? sessions
                  : sessions.where((s) {
                      final q = _searchQuery.toLowerCase();
                      return s.username.toLowerCase().contains(q) ||
                          (s.framedIpAddress?.toLowerCase().contains(q) ??
                              false) ||
                          (s.callingStationId?.toLowerCase().contains(q) ??
                              false) ||
                          (s.customerName?.toLowerCase().contains(q) ?? false);
                    }).toList();

              if (filtered.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Text(
                      'Nenhuma sessão PPPoE/IPoE ativa localizada no momento.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }

              return Column(
                children: filtered.map((s) {
                  return Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          const Icon(Icons.wifi_tethering,
                              color: AppTheme.accentGreen),
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
                                      s.username,
                                      style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 14),
                                    ),
                                    if (s.customerName != null)
                                      Text(
                                        '• ${s.customerName}',
                                        style: const TextStyle(
                                            fontSize: 12,
                                            color: AppTheme.textSecondary),
                                      ),
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                          horizontal: 8, vertical: 2),
                                      decoration: BoxDecoration(
                                        color: AppTheme.primaryBlue
                                            .withValues(alpha: 0.15),
                                        borderRadius: BorderRadius.circular(6),
                                      ),
                                      child: Text(
                                        'IP: ${s.framedIpAddress ?? "N/D"}',
                                        style: const TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.bold,
                                          color: AppTheme.primaryBlue,
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
                                      'BNG: ${s.nasShortname ?? s.nasIpAddress}',
                                      style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary),
                                    ),
                                    Text(
                                      'MAC: ${s.callingStationId ?? "-"}',
                                      style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary),
                                    ),
                                    Text(
                                      'Download: ${_formatBytes(s.acctOutputOctets)} • Upload: ${_formatBytes(s.acctInputOctets)}',
                                      style: const TextStyle(fontSize: 12),
                                    ),
                                    if (s.delegatedIpv6Prefix != null)
                                      Text(
                                        'IPv6 PD: ${s.delegatedIpv6Prefix}',
                                        style: const TextStyle(
                                            fontSize: 11,
                                            color: AppTheme.primaryIndigo),
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
                                onPressed: () =>
                                    _handleLookupHistory(s.username),
                                icon: const Icon(Icons.history, size: 16),
                                label: const Text('Histórico'),
                              ),
                              ElevatedButton.icon(
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppTheme.accentError,
                                ),
                                onPressed: () => _handleDisconnect(s),
                                icon: const Icon(Icons.link_off, size: 16),
                                label: const Text('Desconectar (PoD)'),
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
          const SizedBox(height: 28),

          // Consulta de Histórico RADIUS Accounting por Username
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Histórico de Conexões RADIUS Accounting (radacct)',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      SizedBox(
                        width: 300,
                        child: TextField(
                          controller: _historyUsernameCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Login PPPoE do Assinante',
                            hintText: 'Ex: cliente@provedor',
                            isDense: true,
                          ),
                        ),
                      ),
                      ElevatedButton.icon(
                        onPressed:
                            _loadingHistory ? null : () => _handleLookupHistory(),
                        icon: const Icon(Icons.manage_search, size: 18),
                        label: Text(_loadingHistory
                            ? 'Consultando...'
                            : 'Consultar Histórico'),
                      ),
                    ],
                  ),
                  if (_historyResults != null) ...[
                    const SizedBox(height: 12),
                    if (_historyResults!.isEmpty)
                      const Text(
                        'Nenhum registro de sessão encontrado para este usuário.',
                        style: TextStyle(
                            fontSize: 12, color: AppTheme.textSecondary),
                      )
                    else
                      Column(
                        children: _historyResults!.map((h) {
                          return ListTile(
                            dense: true,
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(
                              h.isOnline
                                  ? Icons.circle
                                  : Icons.history_toggle_off,
                              size: 16,
                              color: h.isOnline
                                  ? AppTheme.accentGreen
                                  : AppTheme.textSecondary,
                            ),
                            title: Text(
                              'Sessão ${h.acctSessionId} • IP: ${h.framedIpAddress ?? "-"} (${h.isOnline ? "ONLINE" : "ENCERRADA"})',
                              style: const TextStyle(
                                  fontSize: 13, fontWeight: FontWeight.w600),
                            ),
                            subtitle: Text(
                              'Início: ${h.acctStartTime ?? "-"} • Fim: ${h.acctStopTime ?? "Ativa"} • MAC: ${h.callingStationId ?? "-"}',
                              style: const TextStyle(
                                  fontSize: 11, color: AppTheme.textSecondary),
                            ),
                          );
                        }).toList(),
                      ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
