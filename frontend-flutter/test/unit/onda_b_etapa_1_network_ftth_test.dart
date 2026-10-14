import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/theme/app_theme.dart';
import 'package:isperp_app/features/network/data/network_ftth_repository.dart';
import 'package:isperp_app/features/network/presentation/network_engineering_screen.dart';

void main() {
  group('Onda B Etapa 1 — Network, OLTs, ONUs, NOC & Topologia FTTH Models', () {
    test('NetworkDeviceModel & OnuModel & OnuDiagnosticsModel parse correctly', () {
      final dev = NetworkDeviceModel.fromJson({
        'id': '01944883-9b48-7b89-9a2c-15a4b7f83b1a',
        'name': 'OLT Central Huawei 5608T',
        'deviceType': 'OLT',
        'driverType': 'SMARTOLT',
        'ipAddress': '192.168.10.2',
        'apiPort': 443,
        'active': true,
      });
      expect(dev.name, 'OLT Central Huawei 5608T');
      expect(dev.driverType, 'SMARTOLT');
      expect(dev.apiPort, 443);

      final onu = OnuModel.fromJson({
        'id': '01944883-9b48-7b89-9a2c-15a4b7f83b1b',
        'contractId': '01944883-9b48-7b89-9a2c-15a4b7f83b1c',
        'customerId': '01944883-9b48-7b89-9a2c-15a4b7f83b1d',
        'onuMac': '48:57:02:1A:BC:DE',
        'onuSerial': 'HWTC1A2B3C4D',
        'vlanId': 100,
        'pppoeUser': 'joao.silva@nexusfibra',
        'downloadSpeed': 600,
        'uploadSpeed': 300,
        'status': 'PROVISIONED',
        'rxPowerDbm': -19.45,
      });
      expect(onu.onuSerial, 'HWTC1A2B3C4D');
      expect(onu.downloadSpeed, 600);
      expect(onu.rxPowerDbm, -19.45);

      final diag = OnuDiagnosticsModel.fromJson({
        'onuMac': '48:57:02:1A:BC:DE',
        'onuSerial': 'HWTC1A2B3C4D',
        'status': 'ONLINE',
        'rxPowerDbm': -18.72,
        'txPowerDbm': 2.15,
        'oltName': 'OLT Central',
        'ponPort': 1,
        'details': 'Sinal óptico dentro dos padrões (-15 a -25 dBm)',
      });
      expect(diag.status, 'ONLINE');
      expect(diag.txPowerDbm, 2.15);
    });

    test('NocMonitoringSummaryModel, OltPonPortModel & FtthIncidentModel parse correctly', () {
      final summary = NocMonitoringSummaryModel.fromJson({
        'totalOlts': 2,
        'totalPonPorts': 16,
        'activePonPorts': 15,
        'totalOnus': 800,
        'onlineOnus': 776,
        'losOnus': 20,
        'dyingGaspOnus': 4,
        'offlineOnus': 24,
        'globalHealthPercentage': 97.0,
        'activeIncidentsCount': 1,
        'criticalIncidentsCount': 1,
        'activeIncidents': [
          {
            'id': '01944883-9b48-7b89-9a2c-15a4b7f83b22',
            'oltName': 'OLT-HUAWEI-5608T',
            'ponName': 'GPON 0/1/4',
            'incidentType': 'FIBER_CUT_PROBABLE',
            'incidentTypeDescription': 'Rompimento Provável de Fibra Óptica',
            'severity': 'CRITICAL',
            'status': 'ACTIVE',
            'title': 'Alarme Massivo PON 0/1/4: 20 ONUs sem sinal (LOS)',
            'affectedCustomersCount': 20,
            'affectedCtoNames': ['CTO-01', 'CTO-02'],
            'affectedCableName': 'CABO-TRONCAL-01',
            'estimatedCutDetails': 'Vértice entre CTO-01 e CTO-02',
            'detectedAt': '2026-10-14T14:00:00Z',
          }
        ],
      });

      expect(summary.totalOlts, 2);
      expect(summary.globalHealthPercentage, 97.0);
      expect(summary.activeIncidents.length, 1);
      expect(summary.activeIncidents.first.incidentType, 'FIBER_CUT_PROBABLE');
      expect(summary.activeIncidents.first.affectedCtoNames, ['CTO-01', 'CTO-02']);

      final pon = OltPonPortModel.fromJson({
        'id': '01944883-9b48-7b89-9a2c-15a4b7f83b33',
        'networkDeviceId': '01944883-9b48-7b89-9a2c-15a4b7f83b1a',
        'oltName': 'OLT-HUAWEI-5608T',
        'slotNumber': 0,
        'portNumber': 4,
        'ponName': 'GPON 0/0/4',
        'adminStatus': 'UP',
        'operStatus': 'UP',
        'txPowerDbm': 4.5,
        'temperatureCelsius': 41.2,
        'totalOnus': 64,
        'onlineOnus': 62,
        'losOnus': 2,
        'dyingGaspOnus': 0,
        'offlineOnus': 2,
        'healthPercentage': 96.8,
      });
      expect(pon.ponName, 'GPON 0/0/4');
      expect(pon.healthPercentage, 96.8);
    });

    test('FtthClosureDiagramModel, FtthFeasibilityResultModel & LightPathTraceResultModel parse correctly', () {
      final diagram = FtthClosureDiagramModel.fromJson({
        'closure': {
          'id': 'ceo-1',
          'name': 'CEO-01-CENTRO',
          'closureType': 'DOMO',
          'trayCount': 4,
          'capacityFusions': 48,
          'usedFusionsCount': 1,
          'status': 'ATIVA',
        },
        'cables': [
          {
            'id': 'cab-1',
            'name': 'CABO-TRONCAL-01',
            'cableType': 'ALIMENTADOR',
            'fiberCount': 24,
            'tubeCount': 2,
            'colorStandard': 'ABNT_NBR_14106',
            'lengthMeters': 450.0,
            'attenuationDbPerKm': 0.35,
            'fibers': [
              {
                'fiberNumber': 1,
                'tubeNumber': 1,
                'fiberInTubeNumber': 1,
                'fiberColorName': 'Verde',
                'fiberColorHex': '#00AA00',
                'tubeColorName': 'Verde',
                'tubeColorHex': '#00AA00',
                'standard': 'ABNT_NBR_14106',
              }
            ],
          }
        ],
        'splitters': [
          {
            'id': 'spl-1',
            'closureId': 'ceo-1',
            'name': 'SPLITTER-1X8-01',
            'splitterType': 'BALANCED_1_8',
            'attenuationDb': 10.5,
            'outputPorts': 8,
          }
        ],
        'fusions': [
          {
            'id': 'fus-1',
            'closureId': 'ceo-1',
            'trayNumber': 1,
            'sourceCableId': 'cab-1',
            'sourceCableName': 'CABO-TRONCAL-01',
            'sourceFiberNumber': 1,
            'targetSplitterId': 'spl-1',
            'targetSplitterName': 'SPLITTER-1X8-01',
            'lossDb': 0.05,
          }
        ],
        'connectedCtos': [
          {
            'id': 'cto-1',
            'name': 'CTO-CENTRO-01',
            'latitude': -23.55052,
            'longitude': -46.633308,
            'totalPorts': 16,
            'freePortsCount': 12,
            'occupiedPortsCount': 4,
            'occupancyPercentage': 25.0,
            'splitterType': 'BALANCED_1_16',
            'status': 'ATIVA',
          }
        ],
      });

      expect(diagram.closure.name, 'CEO-01-CENTRO');
      expect(diagram.cables.first.fibers.first.fiberColorName, 'Verde');
      expect(diagram.splitters.first.outputPorts, 8);
      expect(diagram.fusions.first.targetSplitterName, 'SPLITTER-1X8-01');
      expect(diagram.connectedCtos.first.freePortsCount, 12);

      final feas = FtthFeasibilityResultModel.fromJson({
        'viable': true,
        'viableCtosCount': 1,
        'nearbyCtos': [
          {
            'cto': {
              'id': 'cto-1',
              'name': 'CTO-CENTRO-01',
              'latitude': -23.55052,
              'longitude': -46.633308,
              'totalPorts': 16,
              'freePortsCount': 12,
              'occupiedPortsCount': 4,
              'occupancyPercentage': 25.0,
              'splitterType': 'BALANCED_1_16',
              'status': 'ATIVA',
            },
            'distanceMeters': 62.5,
            'freePorts': 12,
            'hasCapacity': true,
          }
        ],
      });
      expect(feas.viable, isTrue);
      expect(feas.nearbyCtos.first.distanceMeters, 62.5);

      final lightPath = LightPathTraceResultModel.fromJson({
        'reachedSource': true,
        'sourcePopName': 'POP Central Matriz',
        'totalAttenuationDb': 21.4,
        'estimatedRxPowerDbm': -17.4,
        'nodes': [
          {
            'elementType': 'CTO_PORT',
            'name': 'Porta 1 - CTO-CENTRO-01',
            'details': 'Splitter 1:16',
            'addedAttenuationDb': 13.8,
            'cumulativeAttenuationDb': 13.8,
          }
        ],
      });
      expect(lightPath.reachedSource, isTrue);
      expect(lightPath.estimatedRxPowerDbm, -17.4);
      expect(lightPath.nodes.first.elementType, 'CTO_PORT');
    });
  });

  group('Onda B Etapa 1 — NetworkEngineeringScreen Widget Tests', () {
    testWidgets('Renders all 4 tabs and navigates cleanly without overflow', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            nocMonitoringSummaryProvider.overrideWith((ref) async {
              return const NocMonitoringSummaryModel(
                totalOlts: 2,
                totalPonPorts: 8,
                activePonPorts: 8,
                totalOnus: 120,
                onlineOnus: 118,
                losOnus: 1,
                dyingGaspOnus: 1,
                offlineOnus: 2,
                globalHealthPercentage: 98.3,
                activeIncidentsCount: 1,
                criticalIncidentsCount: 1,
                activeIncidents: [],
              );
            }),
            oltPonPortsProvider.overrideWith((ref) async => [
                  const OltPonPortModel(
                    id: 'pon-1',
                    networkDeviceId: 'dev-1',
                    oltName: 'OLT Central',
                    slotNumber: 0,
                    portNumber: 1,
                    ponName: 'GPON 0/0/1',
                    adminStatus: 'UP',
                    operStatus: 'UP',
                    txPowerDbm: 4.5,
                    temperatureCelsius: 40.0,
                    totalOnus: 32,
                    onlineOnus: 32,
                    losOnus: 0,
                    dyingGaspOnus: 0,
                    offlineOnus: 0,
                    healthPercentage: 100.0,
                  ),
                ]),
            ftthIncidentsProvider.overrideWith((ref) async => [
                  const FtthIncidentModel(
                    id: 'inc-1',
                    oltName: 'OLT Central',
                    ponName: 'GPON 0/0/2',
                    incidentType: 'FIBER_CUT_PROBABLE',
                    incidentTypeDescription: 'Rompimento Provável de Fibra',
                    severity: 'CRITICAL',
                    status: 'ACTIVE',
                    title: 'Alarme Massivo PON 0/0/2',
                    affectedCustomersCount: 16,
                    affectedCtoNames: ['CTO-01'],
                    detectedAt: '2026-10-14T14:00:00Z',
                  ),
                ]),
            networkDevicesProvider.overrideWith((ref) async => [
                  const NetworkDeviceModel(
                    id: 'dev-1',
                    name: 'OLT Central Huawei',
                    deviceType: 'OLT',
                    driverType: 'SMARTOLT',
                    ipAddress: '10.0.10.2',
                    apiPort: 443,
                    active: true,
                  ),
                ]),
            onusListProvider.overrideWith((ref) async => [
                  const OnuModel(
                    id: 'onu-1',
                    contractId: 'ctr-1',
                    customerId: 'cus-1',
                    onuMac: '48:57:02:1A:BC:DE',
                    onuSerial: 'HWTC1A2B3C4D',
                    vlanId: 100,
                    pppoeUser: 'cliente.fibra',
                    downloadSpeed: 500,
                    uploadSpeed: 250,
                    status: 'PROVISIONED',
                    rxPowerDbm: -19.2,
                  ),
                ]),
            ftthPopsProvider.overrideWith((ref) async => [
                  const FtthPopModel(id: 'pop-1', name: 'POP Matriz'),
                ]),
            ftthPolesProvider.overrideWith((ref) async => [
                  const FtthPoleModel(
                    id: 'pole-1',
                    code: 'POSTE-01',
                    latitude: -23.55,
                    longitude: -46.63,
                    poleType: 'CONCRETO',
                    reservationMeters: 10,
                  ),
                ]),
            ftthCablesProvider.overrideWith((ref) async => [
                  const FtthCableModel(
                    id: 'cab-1',
                    name: 'CABO-TRONCAL-01',
                    cableType: 'ALIMENTADOR',
                    fiberCount: 24,
                    tubeCount: 2,
                    colorStandard: 'ABNT_NBR_14106',
                    lengthMeters: 500,
                    attenuationDbPerKm: 0.35,
                    fibers: [],
                  ),
                ]),
            ftthCtosProvider.overrideWith((ref) async => [
                  const FtthCtoModel(
                    id: 'cto-1',
                    name: 'CTO-CENTRO-01',
                    latitude: -23.55,
                    longitude: -46.63,
                    totalPorts: 16,
                    freePortsCount: 12,
                    occupiedPortsCount: 4,
                    occupancyPercentage: 25.0,
                    splitterType: 'BALANCED_1_16',
                    status: 'ATIVA',
                    ports: [],
                  ),
                ]),
            ftthClosuresProvider.overrideWith((ref) async => [
                  const FtthClosureModel(
                    id: 'ceo-1',
                    name: 'CEO-01-CENTRO',
                    closureType: 'DOMO',
                    trayCount: 4,
                    capacityFusions: 48,
                    usedFusionsCount: 6,
                    status: 'ATIVA',
                  ),
                ]),
          ],
          child: MaterialApp(
            theme: AppTheme.darkTheme,
            home: const NetworkEngineeringScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Aba 1: Monitoramento NOC & Incidentes
      expect(find.text('Engenharia de Rede & NOC FTTH'), findsOneWidget);
      expect(find.text('98.3%'), findsOneWidget);
      expect(find.text('Alarme Massivo PON 0/0/2'), findsOneWidget);

      // Aba 2: OLTs & ONUs
      final tab2 = find.text('OLTs & ONUs');
      await tester.ensureVisible(tab2);
      await tester.tap(tab2);
      await tester.pumpAndSettle();
      expect(find.text('OLT Central Huawei'), findsOneWidget);
      expect(find.text('SN: HWTC1A2B3C4D'), findsOneWidget);

      // Aba 3: Topologia FTTH & CTOs
      final tab3 = find.text('Topologia FTTH & CTOs');
      await tester.ensureVisible(tab3);
      await tester.tap(tab3);
      await tester.pumpAndSettle();
      expect(find.text('CTO-CENTRO-01'), findsOneWidget);
      expect(find.text('CABO-TRONCAL-01'), findsOneWidget);

      // Aba 4: Caixas de Emenda (CEO) & Fusões
      final tab4 = find.text('Caixas de Emenda (CEO) & Fusões');
      await tester.ensureVisible(tab4);
      await tester.tap(tab4);
      await tester.pumpAndSettle();
      expect(find.text('CEO-01-CENTRO'), findsOneWidget);
    });
  });
}
