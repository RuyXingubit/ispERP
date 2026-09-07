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

    test('OverdueInvoiceItem deve calcular dias de atraso e estágio da régua de cobrança', () {
      final now = DateTime.now();
      String formatDate(DateTime dt) =>
          "${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}";

      // Tolerância: vencido há 3 dias
      final itemTolerance = OverdueInvoiceItem(
        id: 'inv-01',
        contractId: 'ctr-01',
        amount: 89.90,
        dueDate: formatDate(now.subtract(const Duration(days: 3))),
      );
      expect(itemTolerance.daysOverdue, greaterThanOrEqualTo(2));
      expect(itemTolerance.daysOverdue, lessThanOrEqualTo(4));
      expect(itemTolerance.dunningStageLabel, contains('Tolerância'));

      // Notificação: vencido há 10 dias
      final itemNotice = OverdueInvoiceItem(
        id: 'inv-02',
        contractId: 'ctr-02',
        amount: 99.90,
        dueDate: formatDate(now.subtract(const Duration(days: 10))),
      );
      expect(itemNotice.dunningStageLabel, contains('Notificação Formal'));

      // Redução QoS: vencido há 20 dias
      final itemReduction = OverdueInvoiceItem(
        id: 'inv-03',
        contractId: 'ctr-03',
        amount: 120.00,
        dueDate: formatDate(now.subtract(const Duration(days: 20))),
      );
      expect(itemReduction.dunningStageLabel, contains('Redução de Velocidade'));

      // Bloqueio Total: vencido há 45 dias
      final itemBlock = OverdueInvoiceItem(
        id: 'inv-04',
        contractId: 'ctr-04',
        amount: 150.00,
        dueDate: formatDate(now.subtract(const Duration(days: 45))),
      );
      expect(itemBlock.dunningStageLabel, contains('Bloqueio Total'));

      // Rescisão: vencido há 65 dias
      final itemChurn = OverdueInvoiceItem(
        id: 'inv-05',
        contractId: 'ctr-05',
        amount: 200.00,
        dueDate: formatDate(now.subtract(const Duration(days: 65))),
      );
      expect(itemChurn.dunningStageLabel, contains('Desconexão / Serasa'));
    });
  });
}
