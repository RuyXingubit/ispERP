import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/network/api_client.dart';
import 'package:isperp_app/core/storage/storage_service.dart';
import 'package:isperp_app/features/attendance/data/attendance_models.dart';
import 'package:isperp_app/features/attendance/data/attendance_notifier.dart';
import 'package:isperp_app/features/attendance/data/attendance_repository.dart';
import 'package:isperp_app/features/attendance/data/customer_search_model.dart';
import 'package:isperp_app/features/attendance/presentation/steps/step_customer_360_overview.dart';
import 'package:isperp_app/features/financial/data/cash_custody_repository.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _FakeAttendanceNotifier extends AttendanceNotifier {
  _FakeAttendanceNotifier(AttendanceState initialState, SharedPreferences prefs)
      : super(AttendanceRepository(ApiClient(StorageService(prefs)))) {
    state = initialState;
  }
}

void main() {
  group('Onda J — Régua de Notificação Ativa Omnichannel & Aviso Proativo PON', () {
    test('Deve desserializar BillingNotificationDispatchModel e mapear stageLabel (RGC Anatel)', () {
      final dMinus3 = BillingNotificationDispatchModel.fromJson({
        'id': '0195f000-0000-7000-8000-000000000001',
        'invoiceId': 'inv-1',
        'contractId': 'ctr-1',
        'customerId': 'cus-1',
        'customerName': 'Maria Silva',
        'stage': 'D_MINUS_3',
        'channel': 'WHATSAPP',
        'recipient': '5593991112233',
        'status': 'SENT',
        'dispatchedAt': '2026-10-14T10:00:00Z',
      });
      expect(dMinus3.stageLabel, contains('D-3'));

      final dPlus5 = BillingNotificationDispatchModel.fromJson({
        'id': '0195f000-0000-7000-8000-000000000002',
        'invoiceId': 'inv-1',
        'contractId': 'ctr-1',
        'customerId': 'cus-1',
        'customerName': 'Maria Silva',
        'stage': 'D_PLUS_5_ANATEL_NOTICE',
        'channel': 'WHATSAPP',
        'recipient': '5593991112233',
        'status': 'SENT',
        'dispatchedAt': '2026-10-14T10:00:00Z',
      });
      expect(dPlus5.stageLabel, contains('Anatel (D+5)'));

      final paymentConfirmed = BillingNotificationDispatchModel.fromJson({
        'id': '0195f000-0000-7000-8000-000000000003',
        'invoiceId': 'inv-1',
        'contractId': 'ctr-1',
        'customerId': 'cus-1',
        'customerName': 'Maria Silva',
        'stage': 'PAYMENT_CONFIRMED',
        'channel': 'WHATSAPP',
        'recipient': '5593991112233',
        'status': 'SENT',
        'dispatchedAt': '2026-10-14T10:00:00Z',
      });
      expect(paymentConfirmed.stageLabel, contains('Confirmação de Baixa'));
    });

    test('Deve desserializar BillingNotificationSweepResultModel com contadores de estágio', () {
      final sweep = BillingNotificationSweepResultModel.fromJson({
        'evaluatedInvoicesCount': 4,
        'dMinus3SentCount': 1,
        'dZeroSentCount': 2,
        'dPlus5AnatelSentCount': 1,
        'skippedDuplicateCount': 0,
        'dispatches': [
          {
            'id': 'disp-1',
            'invoiceId': 'inv-1',
            'contractId': 'ctr-1',
            'customerId': 'cus-1',
            'customerName': 'Carlos Andrade',
            'stage': 'D_ZERO',
            'channel': 'WHATSAPP',
            'recipient': '5593999990000',
            'status': 'SENT',
            'dispatchedAt': '2026-10-14T12:00:00Z',
          }
        ],
      });

      expect(sweep.evaluatedInvoicesCount, 4);
      expect(sweep.dMinus3SentCount, 1);
      expect(sweep.dZeroSentCount, 2);
      expect(sweep.dPlus5AnatelSentCount, 1);
      expect(sweep.dispatches.length, 1);
      expect(sweep.dispatches.first.stageLabel, contains('D0'));
    });

    testWidgets(
      'StepCustomer360Overview deve exibir Banner Proativo de Rompimento PON sem overflow quando há incidente ativo',
      (tester) async {
        SharedPreferences.setMockInitialValues({});
        final prefs = await SharedPreferences.getInstance();
        final stateWithIncident = AttendanceState(
          step: AttendanceStep.overview,
          selectedCustomer: const CustomerSearchModel(
            id: 'cus-100',
            name: 'Roberto Assinante FTTH',
            cpf: '12345678901',
            city: 'Altamira',
            state: 'PA',
          ),
          contracts: const [
            ContractSummary(
              id: 'ctr-100',
              contractNumber: 'CTR-2026-900',
              status: 'ACTIVE',
              monthlyFee: 119.90,
              dueDay: 10,
              installationAddress: 'Av. Brasil, 500',
            ),
          ],
          activeNetworkIncident: const ActiveNetworkIncidentModel(
            id: 'inc-pon-01',
            oltId: 'olt-01',
            ponPort: '0/1/3',
            incidentType: 'FIBER_CUT',
            status: 'IN_REPAIR',
            affectedCount: 24,
            description: 'Rompimento de backbone óptico na Av. Brasil — equipe de fusão no local.',
          ),
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              attendanceProvider.overrideWith((_) => _FakeAttendanceNotifier(stateWithIncident, prefs)),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: SizedBox(
                  width: 900,
                  height: 700,
                  child: StepCustomer360Overview(),
                ),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(
          find.textContaining('AVISO PROATIVO DE REDE: Rompimento / Manutenção Massiva PON'),
          findsOneWidget,
        );
        expect(
          find.textContaining('Rompimento de backbone óptico na Av. Brasil'),
          findsOneWidget,
        );
        expect(find.textContaining('Porta PON: 0/1/3'), findsOneWidget);
      },
    );
  });
}
