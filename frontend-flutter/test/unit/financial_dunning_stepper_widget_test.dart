import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/models/user_role.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/core/widgets/process_lifecycle_stepper.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_model.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_provider.dart';
import 'package:isperp_app/features/financial/presentation/financial_dashboard_screen.dart';

class FakeAuthNotifier extends StateNotifier<AuthState> implements AuthNotifier {
  FakeAuthNotifier([UserRole role = UserRole.financial])
      : super(AuthState(
          isAuthenticated: true,
          role: role,
          email: 'financeiro@provedor.com.br',
          name: 'Operador Financeiro',
        ));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  final now = DateTime.now();
  String formatDate(DateTime dt) =>
      "${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}";

  final sampleInvoices = [
    OverdueInvoiceItem(
      id: 'inv-tol1',
      contractId: 'ctr-01',
      amount: 89.90,
      dueDate: formatDate(now.subtract(const Duration(days: 3))),
    ),
    OverdueInvoiceItem(
      id: 'inv-not2',
      contractId: 'ctr-02',
      amount: 119.90,
      dueDate: formatDate(now.subtract(const Duration(days: 10))),
    ),
    OverdueInvoiceItem(
      id: 'inv-red3',
      contractId: 'ctr-03',
      amount: 149.90,
      dueDate: formatDate(now.subtract(const Duration(days: 20))),
    ),
    OverdueInvoiceItem(
      id: 'inv-blk4',
      contractId: 'ctr-04',
      amount: 199.90,
      dueDate: formatDate(now.subtract(const Duration(days: 40))),
    ),
  ];

  final mockBiData = DashboardBiModel(
    mrr: 148500.00,
    arr: 1782000.00,
    arpu: 118.80,
    overdueAmount: 559.60,
    defaultRate: 3.2,
    pixConversionRate: 94.5,
    totalReceivedMonth: 154200.50,
    totalCustomers: 1240,
    activeContracts: 1250,
    suspendedContracts: 15,
    pendingInstallationContracts: 8,
    canceledContractsLast30Days: 4,
    churnRate: 0.32,
    totalOnus: 1250,
    provisionedOnus: 1245,
    criticalSignalOnus: 2,
    totalNetworkDevices: 45,
    recentOverdueInvoices: sampleInvoices,
  );

  Widget buildTestScreen({
    DashboardBiModel? biModel,
    UserRole role = UserRole.financial,
  }) {
    return ProviderScope(
      overrides: [
        dashboardBiProvider.overrideWith((ref) => Future.value(biModel ?? mockBiData)),
        authProvider.overrideWith((ref) => FakeAuthNotifier(role)),
      ],
      child: const MaterialApp(
        home: Scaffold(
          body: FinancialDashboardScreen(),
        ),
      ),
    );
  }

  group('FinancialDashboardScreen - Régua de Adimplência & Dunning Stepper', () {
    testWidgets('deve renderizar os indicadores financeiros e a esteira de régua de cobrança', (tester) async {
      tester.view.physicalSize = const Size(1400, 1000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(buildTestScreen());
      await tester.pumpAndSettle();

      // Indicadores
      expect(find.text('Gestão Financeira & Tesouraria'), findsOneWidget);
      expect(find.textContaining('154.200,50'), findsOneWidget);
      expect(find.text('Recebido no Mês'), findsOneWidget);
      expect(find.text('Inadimplência Total'), findsOneWidget);

      // Título da esteira de Dunning
      expect(find.text('Régua de Adimplência & Cobrança Legal (Dunning / RGC Anatel)'), findsOneWidget);

      // As 6 etapas da esteira
      expect(find.text('1. Notificação Preventiva (D-3)'), findsOneWidget);
      expect(find.text('2. Tolerância (D0 a D+5)'), findsOneWidget);
      expect(find.text('3. Notificação Formal (D+10)'), findsOneWidget);
      expect(find.text('4. Redução de Banda (D+15)'), findsOneWidget);
      expect(find.text('5. Bloqueio Total (D+30)'), findsOneWidget);
      expect(find.text('6. Desconexão & Serasa (D+60)'), findsOneWidget);

      // Componente ProcessLifecycleStepper
      expect(find.byType(ProcessLifecycleStepper), findsOneWidget);
    });

    testWidgets('deve abrir modal com POP da etapa ativa ao clicar no botão Ver POP', (tester) async {
      tester.view.physicalSize = const Size(1400, 1000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(buildTestScreen());
      await tester.pumpAndSettle();

      // Procura botão 'Ver POP' na esteira
      final popButton = find.text('Ver POP');
      expect(popButton, findsOneWidget);

      await tester.tap(popButton);
      await tester.pumpAndSettle();

      // O modal de POP deve estar aberto com diretrizes da Anatel
      expect(find.textContaining('POP-DUN-'), findsOneWidget);
      expect(find.text('Entendido / Fechar'), findsOneWidget);

      // Fecha o modal
      await tester.tap(find.text('Entendido / Fechar'));
      await tester.pumpAndSettle();

      expect(find.text('Entendido / Fechar'), findsNothing);
    });

    testWidgets('deve filtrar faturas pelos chips de estágio da régua de cobrança', (tester) async {
      tester.view.physicalSize = const Size(1400, 1000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(buildTestScreen());
      await tester.pumpAndSettle();

      // No filtro 'Todas as Vencidas (4)' aparecem as faturas
      expect(find.text('inv-tol1'), findsOneWidget);
      expect(find.text('inv-blk4'), findsOneWidget);

      // Clica no chip 'Bloqueio Total'
      final blockChip = find.textContaining('Bloqueio Total (30-59d)');
      await tester.tap(blockChip);
      await tester.pumpAndSettle();

      // Agora somente a fatura em Bloqueio Total deve estar na lista filtrada
      expect(find.text('inv-blk4'), findsOneWidget);
      expect(find.text('inv-tol1'), findsNothing);
      expect(find.text('inv-not2'), findsNothing);
    });

    testWidgets('deve selecionar uma fatura ao clicar nela e focar a esteira de cobrança nela', (tester) async {
      tester.view.physicalSize = const Size(1800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(buildTestScreen());
      await tester.pumpAndSettle();

      // Clica na linha ou no botão 'Auditar Régua' correspondente à fatura inv-red3
      final auditarButtons = find.text('Auditar Régua');
      expect(auditarButtons, findsWidgets);

      // Toca no terceiro botão (inv-red3, 20 dias)
      await tester.tap(auditarButtons.at(2));
      await tester.pumpAndSettle();

      // O cabeçalho de auditoria deve indicar a fatura selecionada
      expect(find.textContaining('inv-red3'), findsWidgets);
      expect(find.textContaining('Redução de Velocidade'), findsWidgets);
    });

    testWidgets('deve exibir mensagem vazia limpa quando não houver inadimplência', (tester) async {
      tester.view.physicalSize = const Size(1400, 1000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      final cleanBi = DashboardBiModel(
        mrr: 100000.0,
        arr: 1200000.0,
        arpu: 100.0,
        overdueAmount: 0.0,
        defaultRate: 0.0,
        pixConversionRate: 100.0,
        totalReceivedMonth: 100000.0,
        totalCustomers: 500,
        activeContracts: 500,
        suspendedContracts: 0,
        pendingInstallationContracts: 0,
        canceledContractsLast30Days: 0,
        churnRate: 0.0,
        totalOnus: 500,
        provisionedOnus: 500,
        criticalSignalOnus: 0,
        totalNetworkDevices: 20,
        recentOverdueInvoices: const [],
      );

      await tester.pumpWidget(buildTestScreen(biModel: cleanBi));
      await tester.pumpAndSettle();

      expect(
        find.text('Nenhuma fatura encontrada para o filtro de cobrança selecionado.'),
        findsOneWidget,
      );
    });
  });
}
