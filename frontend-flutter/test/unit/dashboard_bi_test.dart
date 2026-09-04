import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_model.dart';

void main() {
  group('DashboardBiModel Unit Tests', () {
    test('Deve desserializar JSON com dados reais do backend com precisão', () {
      final json = {
        "mrr": 939.00,
        "arr": 11268.00,
        "arpu": 93.90,
        "overdueAmount": 79.90,
        "defaultRate": 0.93,
        "pixConversionRate": 100.00,
        "totalReceivedMonth": 939.00,
        "totalCustomers": 13,
        "activeContracts": 10,
        "suspendedContracts": 1,
        "pendingInstallationContracts": 1,
        "canceledContractsLast30Days": 0,
        "churnRate": 0.00,
        "totalOnus": 1,
        "provisionedOnus": 1,
        "criticalSignalOnus": 0,
        "totalNetworkDevices": 2,
        "recentOverdueInvoices": [
          {
            "amount": 79.90,
            "dueDate": "2026-08-22",
            "contractId": "01a0674f-0271-7b5f-999d-9313eb6e3b34",
            "id": "01a0674f-0272-7057-98b0-c3195f447998"
          }
        ],
        "criticalSignalAlerts": []
      };

      final model = DashboardBiModel.fromJson(json);

      expect(model.mrr, equals(939.00));
      expect(model.arr, equals(11268.00));
      expect(model.arpu, equals(93.90));
      expect(model.overdueAmount, equals(79.90));
      expect(model.defaultRate, equals(0.93));
      expect(model.pixConversionRate, equals(100.00));
      expect(model.totalCustomers, equals(13));
      expect(model.activeContracts, equals(10));
      expect(model.recentOverdueInvoices.length, equals(1));
      expect(model.recentOverdueInvoices.first.amount, equals(79.90));
      expect(model.recentOverdueInvoices.first.dueDate, equals("2026-08-22"));
    });

    test('Deve tratar campos nulos com fallbacks seguros sem quebrar', () {
      final json = <String, dynamic>{};

      final model = DashboardBiModel.fromJson(json);

      expect(model.mrr, equals(0.0));
      expect(model.overdueAmount, equals(0.0));
      expect(model.totalCustomers, equals(0));
      expect(model.recentOverdueInvoices, isEmpty);
    });
  });
}
