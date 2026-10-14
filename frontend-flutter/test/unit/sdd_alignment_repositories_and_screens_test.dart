import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/financial/data/cash_custody_repository.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_model.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_provider.dart';
import 'package:isperp_app/features/financial/presentation/my_cash_custody_screen.dart';
import 'package:isperp_app/features/sales/data/sales_models.dart';
import 'package:isperp_app/features/sales/data/sales_repository.dart';
import 'package:isperp_app/features/sales/presentation/sales_dashboard_screen.dart';
import 'package:isperp_app/features/support/presentation/support_dashboard_screen.dart';
import 'package:isperp_app/features/technician/data/technician_repository.dart';
import 'package:isperp_app/features/technician/presentation/technician_dashboard_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:isperp_app/core/providers/app_providers.dart';

class _FakeSalesRepository extends SalesRepository {
  final List<CommercialPlan> mockPlans;
  _FakeSalesRepository(super.apiClient, this.mockPlans);

  @override
  Future<List<CommercialPlan>> getActivePlans() async => mockPlans;
}

void main() {
  group('SDD Alignment — Models & Screens Unit/Widget Tests', () {
    test('TechnicianWorkOrderModel deserializa WorkOrderResponse da OpenAPI corretamente', () {
      final json = {
        'id': '0195f000-0000-7000-8000-000000000099',
        'contractId': '0195f000-0000-7000-8000-000000000011',
        'customerId': '0195f000-0000-7000-8000-000000000022',
        'type': 'INSTALACAO',
        'status': 'SCHEDULED',
        'scheduledDate': '2026-10-15',
        'scheduledPeriod': 'MANHA',
        'technicianName': 'Roberto Alves',
        'onuSerial': 'ZTEGC89A12B3',
        'fiberSignalDbm': -19.2,
      };

      final model = TechnicianWorkOrderModel.fromJson(json);
      expect(model.id, '0195f000-0000-7000-8000-000000000099');
      expect(model.shortId, '0195F000');
      expect(model.typeLabel, 'Instalação FTTH');
      expect(model.statusLabel, 'Agendada');
      expect(model.onuSerial, 'ZTEGC89A12B3');
      expect(model.fiberSignalDbm, -19.2);
    });

    test('CashCustodyModel e BankDepositModel deserializam payloads de /financial/custody/cash', () {
      final custody = CashCustodyModel.fromJson({
        'id': '0195f000-0000-7000-8000-000000000101',
        'userId': '0195f000-0000-7000-8000-000000000202',
        'userName': 'Ana Paula',
        'userEmail': 'ana@provedor.com.br',
        'userRole': 'ATTENDANT',
        'currentBalance': 350.50,
      });
      expect(custody.userName, 'Ana Paula');
      expect(custody.userRole, 'ATTENDANT');
      expect(custody.currentBalance, 350.50);

      final deposit = BankDepositModel.fromJson({
        'id': '0195f000-0000-7000-8000-000000000303',
        'depositorUserId': '0195f000-0000-7000-8000-000000000202',
        'depositorUserName': 'Ana Paula',
        'amount': 350.50,
        'bankName': 'Banco do Brasil',
        'receiptFileUrl': 'https://storage/comprovante.jpg',
        'status': 'PENDING_AUDIT',
      });
      expect(deposit.bankName, 'Banco do Brasil');
      expect(deposit.amount, 350.50);
      expect(deposit.status, 'PENDING_AUDIT');
    });

    testWidgets('SalesDashboardScreen renderiza planos reais da API sem planos estáticos chumbados', (tester) async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sharedPreferencesProvider.overrideWithValue(prefs),
            dashboardBiProvider.overrideWith((ref) async => DashboardBiModel.fromJson(const {})),
            salesRepositoryProvider.overrideWith((ref) {
              final api = ref.watch(apiClientProvider);
              return _FakeSalesRepository(api, const [
                CommercialPlan(
                  id: 'plan-real-01',
                  name: 'Plano Empresarial 800 Mega',
                  downloadSpeed: 800,
                  uploadSpeed: 400,
                  price: 199.90,
                  svaIncluded: 'IP Fixo + SLA 4h',
                ),
              ]);
            }),
          ],
          child: const MaterialApp(home: SalesDashboardScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Plano Empresarial 800 Mega'), findsOneWidget);
      expect(find.text('Fibra 400 Mega'), findsNothing);
    });

    testWidgets('TechnicianDashboardScreen renderiza O.S. reais de technicianWorkOrdersProvider sem mock OS-TESTE-PROVEDOR', (tester) async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sharedPreferencesProvider.overrideWithValue(prefs),
            technicianWorkOrdersProvider.overrideWith((ref) async => const [
                  TechnicianWorkOrderModel(
                    id: '0195f000-1234-7000-8000-000000000001',
                    contractId: '0195f000-9999-7000-8000-000000000002',
                    customerId: '0195f000-8888-7000-8000-000000000003',
                    type: 'INSTALACAO',
                    status: 'SCHEDULED',
                    scheduledDate: '2026-10-15',
                    scheduledPeriod: 'TARDE',
                  ),
                ]),
          ],
          child: const MaterialApp(home: TechnicianDashboardScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('O.S. #0195F000'), findsOneWidget);
      expect(find.text('Executar O.S.'), findsOneWidget);
      expect(find.text('Ver Checklist de Campo'), findsNothing);
    });

    testWidgets('MyCashCustodyScreen renderiza dados reais sem João Silva ou 79.90', (tester) async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sharedPreferencesProvider.overrideWithValue(prefs),
            allCashCustodiesProvider.overrideWith((ref) async => const [
                  CashCustodyModel(
                    id: 'c-1',
                    userId: 'u-1',
                    userName: 'Operador Real Custódia',
                    userEmail: 'operador@provedor.com.br',
                    userRole: 'ATTENDANT',
                    currentBalance: 120.00,
                  ),
                ]),
            pendingBankDepositsProvider.overrideWith((ref) async => const []),
          ],
          child: const MaterialApp(home: MyCashCustodyScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Operador Real Custódia'), findsOneWidget);
      expect(find.text('João Silva'), findsNothing);
    });

    testWidgets('SupportDashboardScreen renderiza contadores e fila real de chamados', (tester) async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sharedPreferencesProvider.overrideWithValue(prefs),
            dashboardBiProvider.overrideWith((ref) async => DashboardBiModel.fromJson(const {})),
            technicianWorkOrdersProvider.overrideWith((ref) async => const []),
          ],
          child: const MaterialApp(home: SupportDashboardScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Central de Atendimento & Helpdesk'), findsOneWidget);
      expect(find.text('0 em espera'), findsOneWidget);
    });
  });
}
