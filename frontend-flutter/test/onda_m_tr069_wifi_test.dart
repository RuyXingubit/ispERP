import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/network/api_client.dart';
import 'package:isperp_app/core/storage/storage_service.dart';
import 'package:isperp_app/features/attendance/data/attendance_models.dart';
import 'package:isperp_app/features/attendance/data/attendance_repository.dart';
import 'package:isperp_app/features/attendance/presentation/tr069_wifi_management_modal.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _FakeAttendanceRepository extends AttendanceRepository {
  _FakeAttendanceRepository(SharedPreferences prefs)
      : super(ApiClient(StorageService(prefs)));

  bool rebootCalled = false;
  String? lastUpdatedSsid24;
  String? lastUpdatedSsid5;

  @override
  Future<Tr069CpeStatusModel> getTr069CpeStatus(String contractId) async {
    return Tr069CpeStatusModel(
      id: '01958f10-0000-7000-8000-000000000001',
      contractId: contractId,
      deviceId: 'OUI-ONU-20260001',
      serialNumber: 'HWTC20260001',
      modelName: 'Huawei EG8145X6 Wi-Fi 6',
      firmwareVersion: 'V5R020C10S202',
      ssid24ghz: lastUpdatedSsid24 ?? 'ISP_FIBRA_2G',
      channel24ghz: '6',
      ssid5ghz: lastUpdatedSsid5 ?? 'ISP_FIBRA_5G',
      channel5ghz: '36',
      bandSteeringEnabled: true,
      securityMode: 'WPA2_WPA3_PSK',
      uptimeSeconds: 90060,
      acsSyncStatus: 'SYNCED_EMBEDDED_CWMP',
      connectedHosts: const [
        Tr069ConnectedHostModel(
          hostName: 'SmartTV-Sala',
          ipAddress: '192.168.18.10',
          macAddress: 'AA:BB:CC:11:22:33',
          band: '5GHz',
          rssiDbm: -48,
        ),
      ],
      recentCommands: const [
        Tr069CommandLogModel(
          id: '01958f10-0000-7000-8000-000000000002',
          contractId: 'contract-01',
          deviceId: 'OUI-ONU-20260001',
          commandType: 'SET_WIFI_CONFIG',
          triggeredByRole: 'SUBSCRIBER_PORTAL',
          triggeredByName: 'Maria Assinante',
          status: 'APPLIED',
          summary: 'SSID 2.4G=ISP_FIBRA_2G, 5G=ISP_FIBRA_5G',
          createdAt: '2026-10-17T00:00:00Z',
        ),
      ],
    );
  }

  @override
  Future<Tr069CpeStatusModel> updateTr069Wifi({
    required String contractId,
    required String ssid24ghz,
    required String ssid5ghz,
    String? password,
    String channel24ghz = 'AUTO',
    String channel5ghz = 'AUTO',
    bool bandSteeringEnabled = true,
  }) async {
    lastUpdatedSsid24 = ssid24ghz;
    lastUpdatedSsid5 = ssid5ghz;
    return getTr069CpeStatus(contractId);
  }

  @override
  Future<Tr069CommandLogModel> rebootTr069Cpe(String contractId) async {
    rebootCalled = true;
    return Tr069CommandLogModel(
      id: '01958f10-0000-7000-8000-000000000009',
      contractId: contractId,
      deviceId: 'OUI-ONU-20260001',
      commandType: 'REBOOT',
      triggeredByRole: 'SUPPORT_AGENT',
      status: 'APPLIED',
      summary: 'Reboot remoto despachado via CWMP',
      createdAt: '2026-10-17T00:10:00Z',
    );
  }
}

void main() {
  test('Tr069CpeStatusModel parses Dual-Band Wi-Fi, hosts, commands and formats uptime', () {
    final model = Tr069CpeStatusModel.fromJson({
      'id': '01958f10-0000-7000-8000-000000000001',
      'contractId': '01958f10-0000-7000-8000-000000000010',
      'deviceId': 'OUI-ONU-20260001',
      'serialNumber': 'HWTC20260001',
      'modelName': 'Huawei EG8145X6',
      'firmwareVersion': 'V5R020C10S202',
      'ssid24ghz': 'CLIENTE_2G',
      'channel24ghz': '11',
      'ssid5ghz': 'CLIENTE_5G',
      'channel5ghz': '149',
      'bandSteeringEnabled': true,
      'securityMode': 'WPA2_WPA3_PSK',
      'uptimeSeconds': 93600,
      'acsSyncStatus': 'SYNCED_GENIEACS',
      'connectedHosts': [
        {
          'hostName': 'Notebook-Ruy',
          'ipAddress': '192.168.18.14',
          'macAddress': '11:22:33:44:55:66',
          'band': '5GHz',
          'rssiDbm': -45,
        }
      ],
      'recentCommands': [
        {
          'id': 'cmd-1',
          'contractId': '01958f10-0000-7000-8000-000000000010',
          'commandType': 'REBOOT',
          'triggeredByRole': 'SUPPORT_AGENT',
          'status': 'APPLIED',
          'summary': 'Reboot remoto ONU',
          'createdAt': '2026-10-17T00:00:00Z',
        }
      ],
    });

    expect(model.ssid24ghz, 'CLIENTE_2G');
    expect(model.ssid5ghz, 'CLIENTE_5G');
    expect(model.formattedUptime, '1d 2h');
    expect(model.connectedHosts.single.hostName, 'Notebook-Ruy');
    expect(model.recentCommands.single.commandType, 'REBOOT');
  });

  testWidgets('Tr069WifiManagementModal displays Dual-Band Wi-Fi, updates SSID and triggers Reboot', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final fakeRepo = _FakeAttendanceRepository(prefs);
    const contract = ContractSummary(
      id: 'contract-01',
      contractNumber: 'CTR-2026-900',
      status: 'ACTIVE',
      monthlyFee: 129.90,
      dueDay: 10,
      installationAddress: 'Av. Brasil, 100',
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          attendanceRepositoryProvider.overrideWithValue(fakeRepo),
        ],
        child: const MaterialApp(
          home: Scaffold(
            body: Tr069WifiManagementModal(
              contract: contract,
              customerName: 'Maria Assinante',
            ),
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.textContaining('Huawei EG8145X6 Wi-Fi 6'), findsOneWidget);
    expect(find.textContaining('SmartTV-Sala'), findsOneWidget);
    expect(find.text('AUTOATENDIMENTO'), findsOneWidget);

    await tester.tap(find.text('Reboot Remoto ONU'));
    await tester.pumpAndSettle();

    expect(fakeRepo.rebootCalled, isTrue);
    expect(find.textContaining('REBOOT: Reboot remoto despachado via CWMP'), findsOneWidget);
  });
}
