import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/attendance/data/attendance_models.dart';

void main() {
  group('Onda I — Ciclo de Vida Pós-Venda de Contratos (Models & Pro-Rata)', () {
    test('calculates Pro-Rata Die upgrade preview on 30-day commercial base', () {
      // Upgrade from R$ 100.00 to R$ 160.00 (+R$ 60.00/mês) with 15 days remaining until dueDay 25
      final preview = ContractSummary.calculateProRataPreview(
        currentMonthlyFee: 100.00,
        newMonthlyFee: 160.00,
        dueDay: 25,
        referenceDate: DateTime(2026, 4, 10),
      );

      expect(preview.remainingDays, 15);
      expect(preview.proRataDelta, 30.00);
    });

    test('calculates Pro-Rata Die downgrade preview with negative adjustment', () {
      // Downgrade from R$ 190.00 to R$ 100.00 (-R$ 90.00/mês) with 10 days remaining until dueDay 20
      final preview = ContractSummary.calculateProRataPreview(
        currentMonthlyFee: 190.00,
        newMonthlyFee: 100.00,
        dueDay: 20,
        referenceDate: DateTime(2026, 4, 10),
      );

      expect(preview.remainingDays, 10);
      expect(preview.proRataDelta, -30.00);
    });

    test('parses ContractLifecycleActionResultModel and ContractLifecycleEventModel accurately', () {
      final json = {
        'contract': {
          'id': '0195f000-0000-7000-8000-000000000001',
          'contractNumber': 'CTR-2026-9001',
          'status': 'ACTIVE',
          'planId': '0195f000-0000-7000-8000-000000000002',
          'monthlyFee': 159.90,
          'pendingProRataAdjustment': 25.00,
          'dueDay': 15,
          'installationAddress': 'Av. Brasil, 500',
          'city': 'Altamira',
          'state': 'PA',
          'zipCode': '68371-000',
        },
        'lifecycleEvent': {
          'id': '0195f000-0000-7000-8000-000000000099',
          'contractId': '0195f000-0000-7000-8000-000000000001',
          'eventType': 'OWNERSHIP_TRANSFER',
          'previousCustomerId': '0195f000-0000-7000-8000-000000000010',
          'newCustomerId': '0195f000-0000-7000-8000-000000000020',
          'migratedInvoicesCount': 2,
          'signatureSessionId': '0195f000-0000-7000-8000-000000000077',
          'radiusCoaSynced': false,
          'notes': 'Transferência de titularidade com migração de faturas',
          'createdAt': '2026-10-14T21:15:00Z',
        },
        'message': 'Troca de titularidade concluída com 2 faturas migradas.',
      };

      final result = ContractLifecycleActionResultModel.fromJson(json);

      expect(result.contract.contractNumber, 'CTR-2026-9001');
      expect(result.contract.pendingProRataAdjustment, 25.00);
      expect(result.lifecycleEvent.eventType, 'OWNERSHIP_TRANSFER');
      expect(
        result.lifecycleEvent.eventTypeLabel,
        'Troca de Titularidade + Migração de Faturas',
      );
      expect(result.lifecycleEvent.migratedInvoicesCount, 2);
      expect(
        result.lifecycleEvent.signatureSessionId,
        '0195f000-0000-7000-8000-000000000077',
      );
      expect(result.message, contains('2 faturas migradas'));
    });
  });
}
