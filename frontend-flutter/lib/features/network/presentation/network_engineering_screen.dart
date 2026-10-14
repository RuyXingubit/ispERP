import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/network_ftth_repository.dart';
import 'closures_and_fusions_tab.dart';
import 'ftth_topology_and_ctos_tab.dart';
import 'noc_monitoring_and_incidents_tab.dart';
import 'olts_and_onus_tab.dart';

/// Tela dedicada de Engenharia de Rede & NOC FTTH (`/network`).
/// Organizada em 4 abas:
/// 1. Monitoramento NOC & Incidentes
/// 2. OLTs & ONUs
/// 3. Topologia FTTH & CTOs
/// 4. Caixas de Emenda (CEO) & Fusões
class NetworkEngineeringScreen extends ConsumerWidget {
  const NetworkEngineeringScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        body: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Cabeçalho superior com botão de atualizar todos os providers
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 20, 24, 12),
              child: Row(
                children: [
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Engenharia de Rede & NOC FTTH',
                          style: TextStyle(
                              fontSize: 22, fontWeight: FontWeight.bold),
                          overflow: TextOverflow.ellipsis,
                        ),
                        SizedBox(height: 4),
                        Text(
                          'Monitoramento PON proativo, OLTs/ONUs, topologia georreferenciada ABNT NBR 14106, CEOs e LightPath',
                          style: TextStyle(
                              fontSize: 13, color: AppTheme.textSecondary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  OutlinedButton.icon(
                    onPressed: () {
                      ref.invalidate(nocMonitoringSummaryProvider);
                      ref.invalidate(oltPonPortsProvider);
                      ref.invalidate(ftthIncidentsProvider);
                      ref.invalidate(networkDevicesProvider);
                      ref.invalidate(onusListProvider);
                      ref.invalidate(ftthPopsProvider);
                      ref.invalidate(ftthPolesProvider);
                      ref.invalidate(ftthCablesProvider);
                      ref.invalidate(ftthCtosProvider);
                      ref.invalidate(ftthClosuresProvider);
                    },
                    icon: const Icon(Icons.refresh, size: 18),
                    label: const Text('Sincronizar NOC & Rede'),
                  ),
                ],
              ),
            ),

            // Barra de Abas responsiva sem overflow
            Container(
              decoration: const BoxDecoration(
                border: Border(
                  bottom: BorderSide(color: AppTheme.darkCard, width: 1),
                ),
              ),
              child: const TabBar(
                isScrollable: true,
                tabAlignment: TabAlignment.start,
                padding: EdgeInsets.symmetric(horizontal: 16),
                tabs: [
                  Tab(
                    icon: Icon(Icons.radar_outlined, size: 18),
                    text: 'Monitoramento NOC & Incidentes',
                  ),
                  Tab(
                    icon: Icon(Icons.router_outlined, size: 18),
                    text: 'OLTs & ONUs',
                  ),
                  Tab(
                    icon: Icon(Icons.hub_outlined, size: 18),
                    text: 'Topologia FTTH & CTOs',
                  ),
                  Tab(
                    icon: Icon(Icons.call_split_outlined, size: 18),
                    text: 'Caixas de Emenda (CEO) & Fusões',
                  ),
                ],
              ),
            ),

            // Conteúdo das Abas
            const Expanded(
              child: TabBarView(
                children: [
                  NocMonitoringAndIncidentsTab(),
                  OltsAndOnusTab(),
                  FtthTopologyAndCtosTab(),
                  ClosuresAndFusionsTab(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
