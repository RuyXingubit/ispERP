import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/theme/app_theme.dart';
import 'package:isperp_app/features/admin/data/nas_model.dart';
import 'package:isperp_app/features/admin/data/nas_repository.dart';
import 'package:isperp_app/features/admin/data/radius_ipam_marcocivil_repository.dart';
import 'package:isperp_app/features/admin/presentation/nas_management_screen.dart';

class _FakeNasRepository implements NasRepository {
  @override
  Future<List<NasModel>> getAllNas() async {
    return [
      NasModel(
        id: 'nas-1',
        nasname: '10.0.0.1',
        shortname: 'bng-core-01',
        secret: 'secret123',
        ports: 1812,
        vendorType: NasVendorType.mikrotik,
        description: 'BNG POP Central',
      ),
    ];
  }

  @override
  Future<NasModel> createNas(NasModel nas) async => nas;

  @override
  Future<NasModel> updateNas(String id, NasModel nas) async => nas;

  @override
  Future<void> deleteNas(String id) async {}
}

void main() {
  group('Onda B Etapa 2 — RADIUS, IPAM, CGNAT & Marco Civil Models', () {
    test('RadiusSessionModel, RadiusDisconnectResultModel & RadiusLifecycleModels parse correctly', () {
      final session = RadiusSessionModel.fromJson({
        'radacctId': 10425,
        'acctSessionId': '81a0002f',
        'username': 'cliente.fibra@provedor',
        'nasIpAddress': '10.0.0.1',
        'nasShortname': 'bng-core-01',
        'acctInputOctets': 104857600,
        'acctOutputOctets': 524288000,
        'callingStationId': '48:57:02:1A:BC:DE',
        'framedIpAddress': '100.64.12.34',
        'delegatedIpv6Prefix': '2804:xxxx:1::/56',
        'isOnline': true,
        'customerName': 'Cliente Fibra',
      });
      expect(session.username, 'cliente.fibra@provedor');
      expect(session.framedIpAddress, '100.64.12.34');
      expect(session.isOnline, isTrue);

      final pod = RadiusDisconnectResultModel.fromJson({
        'username': 'cliente.fibra@provedor',
        'success': true,
        'message': 'Pacote PoD enviado com sucesso.',
      });
      expect(pod.success, isTrue);

      final summary = RadiusLifecycleSummaryModel.fromJson({
        'totalPppoeUsers': 1250,
        'totalActiveUsers': 1180,
        'totalBlockedUsers': 70,
        'totalTrustUnblocked': 15,
        'todayAutoBlocksCount': 5,
        'todayUnblocksCount': 8,
        'toleranceDays': 5,
        'autoBlockEnabled': true,
      });
      expect(summary.totalPppoeUsers, 1250);
      expect(summary.toleranceDays, 5);

      final policy = RadiusPolicyConfigModel.fromJson({
        'id': 'pol-1',
        'autoBlockEnabled': true,
        'toleranceDays': 5,
        'blockMode': 'CAPTIVE_PORTAL',
        'reducedDownloadKbps': 1024,
        'reducedUploadKbps': 512,
        'unblockOnPayment': true,
        'sendPodOnBlock': true,
        'sendPodOnUnblock': true,
        'blockStartHour': 8,
        'blockEndHour': 18,
        'allowBlockOnFriday': false,
        'protectEveOfHolidays': true,
      });
      expect(policy.blockMode, 'CAPTIVE_PORTAL');
      expect(policy.protectEveOfHolidays, isTrue);

      final log = RadiusLifecycleLogModel.fromJson({
        'id': 'log-1',
        'contractId': 'ctr-1',
        'customerId': 'cus-1',
        'customerName': 'Cliente Fibra',
        'username': 'cliente.fibra@provedor',
        'actionType': 'AUTO_BLOCK',
        'reason': 'Fatura vencida há 5 dias',
        'nasIp': '10.0.0.1',
        'success': true,
        'createdAt': '2026-10-14T10:00:00Z',
      });
      expect(log.actionType, 'AUTO_BLOCK');
      expect(log.success, isTrue);
    });

    test('IpamModels (ASN, VRF, Subnet, CIDR Calculation, Split VLSM & IP Address) parse correctly', () {
      final asn = IpamAsnModel.fromJson({
        'id': 'asn-1',
        'asn': 265000,
        'name': 'PROVEDOR TELECOM LTDA',
        'rir': 'REGISTRO_BR',
      });
      expect(asn.asn, 265000);

      final vrf = IpamVrfModel.fromJson({
        'id': 'vrf-1',
        'name': 'VRF-CLIENTES',
        'rd': '265000:100',
        'isDefault': true,
      });
      expect(vrf.rd, '265000:100');

      final subnet = IpamSubnetModel.fromJson({
        'id': 'sub-1',
        'cidr': '100.64.0.0/24',
        'ipVersion': 'IPV4',
        'networkAddress': '100.64.0.0',
        'prefixLength': 24,
        'totalHosts': 254,
        'allocatedHosts': 127,
        'utilizationPercentage': 50.0,
        'isPool': true,
        'poolName': 'pool-cgnat-01',
        'status': 'ACTIVE',
        'category': 'CGNAT',
      });
      expect(subnet.cidr, '100.64.0.0/24');
      expect(subnet.utilizationPercentage, 50.0);

      final split = IpamSplitResponseModel.fromJson({
        'parentSubnetId': 'sub-1',
        'parentCidr': '100.64.0.0/24',
        'targetPrefixLength': 25,
        'totalSubnetsGenerated': 2,
        'generatedSubnets': [
          {
            'cidr': '100.64.0.0/25',
            'ipVersion': 'IPV4',
            'networkAddress': '100.64.0.0',
            'netmask': '255.255.255.128',
            'firstUsableIp': '100.64.0.1',
            'lastUsableIp': '100.64.0.126',
            'prefixLength': 25,
            'totalHosts': 128,
            'usableHosts': 126,
          }
        ],
        'persistedSubnets': [],
      });
      expect(split.totalSubnetsGenerated, 2);
      expect(split.generatedSubnets.first.usableHosts, 126);

      final ip = IpamIpAddressModel.fromJson({
        'id': 'ip-1',
        'subnetId': 'sub-1',
        'subnetCidr': '100.64.0.0/24',
        'ipAddress': '100.64.0.10',
        'status': 'ALLOCATED',
        'dnsName': 'bng01.provedor.net.br',
      });
      expect(ip.ipAddress, '100.64.0.10');
    });

    test('CGNAT & Marco Civil Models parse correctly', () {
      final mapping = CgnatMappingModel.fromJson({
        'id': 'cg-1',
        'vendorType': 'MIKROTIK',
        'publicIp': '200.20.10.1',
        'portStart': 1024,
        'portEnd': 2047,
        'privateIpStart': '100.64.0.2',
        'privateIpEnd': '100.64.0.33',
        'protocol': 'BOTH',
      });
      expect(mapping.publicIp, '200.20.10.1');
      expect(mapping.portStart, 1024);

      final search = MarcoCivilSearchResultModel.fromJson({
        'matched': true,
        'queriedIp': '200.20.10.1',
        'queriedPort': 1055,
        'queriedTimestamp': '2026-10-14T14:30:00Z',
        'usedCgnat': true,
        'resolvedPrivateIp': '100.64.0.2',
        'cgnatRuleSummary': '200.20.10.1:1024-2047 -> 100.64.0.2',
        'username': 'assinante.marco',
        'callingStationId': 'AA:BB:CC:DD:EE:FF',
        'contractId': 'ctr-99',
        'contractNumber': 'CTR-2026-99',
        'customerName': 'Assinante Identificado',
        'customerCpfCnpj': '111.222.333-44',
      });
      expect(search.matched, isTrue);
      expect(search.resolvedPrivateIp, '100.64.0.2');

      final report = MarcoCivilReportModel.fromJson({
        'id': 'rep-1',
        'validationToken': 'MC-TOKEN-2026',
        'sha256Hash': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'publicValidationUrl': 'https://provedor.com.br/public/marco-civil/validate/MC-TOKEN-2026',
        'qrCodePayload': 'MC-TOKEN-2026',
        'queriedIp': '200.20.10.1',
        'queriedTimestamp': '2026-10-14T14:30:00Z',
        'createdAt': '2026-10-14T15:00:00Z',
      });
      expect(report.validationToken, 'MC-TOKEN-2026');

      final val = PublicValidationResultModel.fromJson({
        'valid': true,
        'validationToken': 'MC-TOKEN-2026',
        'sha256Hash': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'queriedIp': '200.20.10.1',
        'queriedTimestamp': '2026-10-14T14:30:00Z',
        'reportIssuedAt': '2026-10-14T15:00:00Z',
        'statusMessage': 'Laudo pericial íntegro e assinado digitalmente.',
      });
      expect(val.valid, isTrue);
    });
  });

  group('Onda B Etapa 2 — NasManagementScreen 5 Tabs Widget Test', () {
    testWidgets('Renders all 5 tabs in /admin/nas and switches cleanly', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            nasRepositoryProvider.overrideWithValue(_FakeNasRepository()),
            radiusActiveSessionsProvider.overrideWith((ref) async => [
                  const RadiusSessionModel(
                    radacctId: 101,
                    acctSessionId: 'sess-01',
                    username: 'assinante.online@fibra',
                    nasIpAddress: '10.0.0.1',
                    nasShortname: 'bng-core-01',
                    acctInputOctets: 1024000,
                    acctOutputOctets: 2048000,
                    framedIpAddress: '100.64.10.5',
                    isOnline: true,
                    customerName: 'Cliente Online',
                  ),
                ]),
            radiusLifecycleSummaryProvider.overrideWith((ref) async =>
                const RadiusLifecycleSummaryModel(
                  totalPppoeUsers: 500,
                  totalActiveUsers: 480,
                  totalBlockedUsers: 20,
                  totalTrustUnblocked: 3,
                  todayAutoBlocksCount: 2,
                  todayUnblocksCount: 4,
                  toleranceDays: 5,
                  autoBlockEnabled: true,
                )),
            radiusLifecyclePolicyProvider.overrideWith((ref) async =>
                const RadiusPolicyConfigModel(
                  id: 'pol-1',
                  autoBlockEnabled: true,
                  toleranceDays: 5,
                  blockMode: 'CAPTIVE_PORTAL',
                  reducedDownloadKbps: 1024,
                  reducedUploadKbps: 512,
                  unblockOnPayment: true,
                  sendPodOnBlock: true,
                  sendPodOnUnblock: true,
                  blockStartHour: 8,
                  blockEndHour: 18,
                  allowBlockOnFriday: false,
                  protectEveOfHolidays: true,
                )),
            radiusLifecycleLogsProvider.overrideWith((ref) async => [
                  const RadiusLifecycleLogModel(
                    id: 'log-1',
                    contractId: 'ctr-1',
                    customerId: 'cus-1',
                    username: 'assinante.bloqueado',
                    actionType: 'AUTO_BLOCK',
                    reason: 'Inadimplência 5 dias',
                    success: true,
                    createdAt: '2026-10-14T10:00:00Z',
                  ),
                ]),
            ipamAsnsProvider.overrideWith((ref) async => [
                  const IpamAsnModel(
                    id: 'asn-1',
                    asn: 265000,
                    name: 'PROVEDOR AS',
                    rir: 'REGISTRO_BR',
                  ),
                ]),
            ipamVrfsProvider.overrideWith((ref) async => [
                  const IpamVrfModel(
                    id: 'vrf-1',
                    name: 'VRF-B2C',
                    rd: '265000:100',
                    isDefault: true,
                  ),
                ]),
            ipamSubnetsProvider.overrideWith((ref) async => [
                  const IpamSubnetModel(
                    id: 'sub-1',
                    cidr: '100.64.0.0/22',
                    ipVersion: 'IPV4',
                    networkAddress: '100.64.0.0',
                    prefixLength: 22,
                    totalHosts: 1022,
                    allocatedHosts: 511,
                    utilizationPercentage: 50.0,
                    isPool: true,
                    poolName: 'pool-cgnat',
                    status: 'ACTIVE',
                    category: 'CGNAT',
                  ),
                ]),
            cgnatMappingsProvider.overrideWith((ref) async => [
                  const CgnatMappingModel(
                    id: 'cg-1',
                    vendorType: 'MIKROTIK',
                    publicIp: '200.20.10.1',
                    portStart: 1024,
                    portEnd: 2047,
                    privateIpStart: '100.64.0.2',
                    privateIpEnd: '100.64.0.33',
                    protocol: 'BOTH',
                  ),
                ]),
          ],
          child: MaterialApp(
            theme: AppTheme.darkTheme,
            home: const NasManagementScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Aba 1: Concentradores BNG / NAS
      expect(find.text('bng-core-01'), findsOneWidget);

      // Aba 2: Sessões RADIUS & PoD
      final tab2 = find.text('Sessões RADIUS & PoD');
      await tester.ensureVisible(tab2);
      await tester.tap(tab2);
      await tester.pumpAndSettle();
      expect(find.text('assinante.online@fibra'), findsOneWidget);

      // Aba 3: Auto-Corte & Ciclo de Vida
      final tab3 = find.text('Auto-Corte & Ciclo de Vida');
      await tester.ensureVisible(tab3);
      await tester.tap(tab3);
      await tester.pumpAndSettle();
      expect(find.text('500'), findsOneWidget);
      expect(find.textContaining('AUTO_BLOCK'), findsOneWidget);

      // Aba 4: IPAM IPv4/IPv6 & VLSM
      final tab4 = find.text('IPAM IPv4/IPv6 & VLSM');
      await tester.ensureVisible(tab4);
      await tester.tap(tab4);
      await tester.pumpAndSettle();
      expect(find.text('100.64.0.0/22'), findsOneWidget);

      // Aba 5: CGNAT & Marco Civil
      final tab5 = find.text('CGNAT & Marco Civil');
      await tester.ensureVisible(tab5);
      await tester.tap(tab5);
      await tester.pumpAndSettle();
      expect(
        find.textContaining('Investigação Forense Reversa — Marco Civil da Internet'),
        findsOneWidget,
      );
      expect(find.textContaining('IP Público: 200.20.10.1'), findsOneWidget);
    });
  });
}
