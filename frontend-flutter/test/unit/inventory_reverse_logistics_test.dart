import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/models/user_role.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/core/widgets/process_lifecycle_stepper.dart';
import 'package:isperp_app/features/dispatch/data/dispatch_models.dart';
import 'package:isperp_app/features/inventory/data/inventory_models.dart';
import 'package:isperp_app/features/inventory/data/inventory_notifier.dart';
import 'package:isperp_app/features/inventory/data/inventory_repository.dart';
import 'package:isperp_app/features/inventory/presentation/inventory_screen.dart';

class FakeAuthNotifier extends StateNotifier<AuthState> implements AuthNotifier {
  FakeAuthNotifier([UserRole role = UserRole.admin])
      : super(AuthState(
          isAuthenticated: true,
          role: role,
          email: 'almoxarifado@provedor.com.br',
          name: 'Almoxarife Sede',
        ));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class FakeReverseLogisticsRepo implements InventoryRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);

  final List<InstallationDemandModel> demands = [
    InstallationDemandModel(
      id: '01912345-0000-7000-8000-000000000001',
      workOrderId: '01912345-0000-7000-8000-000000000010',
      contractId: '01912345-0000-7000-8000-000000000100',
      contractNumber: 'CTR-2026-001',
      customerName: 'Cliente Instalacao Normal',
      customerAddress: 'Rua das Palmeiras, 100',
      estimatedDropMeters: 50,
      onuModelRequired: 'ONT Wi-Fi Dual-Band GPON Gigabit',
      fastConnectorsCount: 2,
      ptoRosetteCount: 1,
      status: MaterialDemandStatus.pendingAllocation,
      workOrderType: 'INSTALACAO',
    ),
    InstallationDemandModel(
      id: '01912345-0000-7000-8000-000000000099',
      workOrderId: '01912345-0000-7000-8000-000000000090',
      contractId: '01912345-0000-7000-8000-000000000900',
      contractNumber: 'CTR-2026-099',
      customerName: 'Assinante Cancelado Comodato',
      customerAddress: 'Av. Brasil, 450 - Centro',
      estimatedDropMeters: 0,
      onuModelRequired: 'ONT Wi-Fi 6 AX3000 GPON',
      fastConnectorsCount: 0,
      ptoRosetteCount: 0,
      status: MaterialDemandStatus.allocatedCentral,
      workOrderType: 'RETIRADA',
      allocatedTechnicianName: 'Marcos Coletor',
    ),
  ];

  @override
  Future<List<WarehouseModel>> listWarehouses() async => [
        const WarehouseModel(
          id: 'wh-01',
          code: 'DEP-CENTRAL',
          name: 'Almoxarifado Central',
          city: 'Altamira',
          state: 'PA',
        ),
      ];

  @override
  Future<List<InventoryItemModel>> listInventoryItems() async => [];

  @override
  Future<List<InstallationDemandModel>> listInstallationDemands() async => demands;

  @override
  Future<List<StockTransferModel>> listTransfers() async => [];

  @override
  Future<List<CollaboratorModel>> listCollaborators() async => [];

  @override
  Future<bool> confirmStockAllocation(String workOrderId, {String? warehouseId}) async => true;
}

void main() {
  group('Logística Reversa & Offboarding de Comodato - Unit & State Tests', () {
    late FakeReverseLogisticsRepo fakeRepo;
    late InventoryNotifier notifier;

    setUp(() {
      fakeRepo = FakeReverseLogisticsRepo();
      notifier = InventoryNotifier(fakeRepo);
    });

    test('InventoryState deve separar e contabilizar ordens de recolhimento de comodato', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      expect(notifier.state.demands.length, equals(2));
      expect(notifier.state.reverseLogisticsCount, equals(1));
      expect(notifier.state.reverseLogisticsDemands.length, equals(1));

      final removal = notifier.state.reverseLogisticsDemands.first;
      expect(removal.isRemoval, isTrue);
      expect(removal.isInstallation, isFalse);
      expect(removal.customerName, equals('Assinante Cancelado Comodato'));
      expect(removal.allocatedTechnicianName, equals('Marcos Coletor'));
    });

    test('Deve processar recebimento em quarentena de comodato no Almoxarifado', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      final success = await notifier.processReverseLogisticsCheckin('01912345-0000-7000-8000-000000000090');
      expect(success, isTrue);
      expect(notifier.state.successMessage, contains('Equipamento de comodato recebido e transferido para Quarentena'));
    });
  });

  group('Logística Reversa - Widget Tests (InventoryScreen)', () {
    Widget buildTestScreen({
      required FakeReverseLogisticsRepo repo,
      UserRole role = UserRole.admin,
      int initialTab = 4,
    }) {
      final notifier = InventoryNotifier(repo);
      notifier.setTab(initialTab);

      return ProviderScope(
        overrides: [
          inventoryRepositoryProvider.overrideWithValue(repo),
          inventoryProvider.overrideWith((ref) => notifier),
          authProvider.overrideWith((ref) => FakeAuthNotifier(role)),
        ],
        child: const MaterialApp(
          home: Scaffold(
            body: InventoryScreen(),
          ),
        ),
      );
    }

    testWidgets('deve renderizar a 5ª aba de Logística Reversa e métrica no header', (tester) async {
      tester.view.physicalSize = const Size(1800, 1200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      final repo = FakeReverseLogisticsRepo();
      await tester.pumpWidget(buildTestScreen(repo: repo, initialTab: 0));
      await tester.pumpAndSettle();

      // Métricas do header
      expect(find.text('Logística Reversa'), findsOneWidget);

      // Botão da 5ª aba
      expect(find.text('Logística Reversa (Comodato)'), findsOneWidget);
    });

    testWidgets('deve renderizar a esteira de 6 passos de offboarding ao abrir a aba de Logística Reversa', (tester) async {
      tester.view.physicalSize = const Size(1800, 1200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      final repo = FakeReverseLogisticsRepo();
      await tester.pumpWidget(buildTestScreen(repo: repo, initialTab: 4));
      await tester.pumpAndSettle();

      // Título e subtítulo
      expect(find.text('Logística Reversa & Offboarding de Comodato'), findsOneWidget);
      expect(find.text('Esteira de Logística Reversa & Offboarding de Comodato'), findsOneWidget);

      // As 6 etapas da esteira
      expect(find.text('1. Rescisão & Solicitação'), findsOneWidget);
      expect(find.text('2. Despacho & Rota de Coleta'), findsOneWidget);
      expect(find.text('3. Coleta Domiciliar & Termo'), findsOneWidget);
      expect(find.text('4. Entrada em Quarentena'), findsOneWidget);
      expect(find.text('5. Higienização & Reteste Óptico'), findsOneWidget);
      expect(find.text('6. Reintegração / Sucata'), findsOneWidget);

      // Detalhes da O.S. selecionada
      expect(find.textContaining('Assinante Cancelado Comodato'), findsWidgets);
      expect(find.textContaining('ONT Wi-Fi 6 AX3000 GPON'), findsWidgets);

      // Componente ProcessLifecycleStepper
      expect(find.byType(ProcessLifecycleStepper), findsOneWidget);
    });

    testWidgets('deve abrir o modal de POP da etapa ao clicar no botão Ver POP', (tester) async {
      tester.view.physicalSize = const Size(1800, 1200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      final repo = FakeReverseLogisticsRepo();
      await tester.pumpWidget(buildTestScreen(repo: repo, initialTab: 4));
      await tester.pumpAndSettle();

      // Localiza botão Ver POP
      final popButton = find.text('Ver POP');
      expect(popButton, findsOneWidget);

      await tester.tap(popButton);
      await tester.pumpAndSettle();

      // Modal com Procedimento Operacional Padrão
      expect(find.textContaining('POP-REV-'), findsOneWidget);
      expect(find.text('Entendido / Fechar'), findsOneWidget);

      await tester.tap(find.text('Entendido / Fechar'));
      await tester.pumpAndSettle();

      expect(find.text('Entendido / Fechar'), findsNothing);
    });

    testWidgets('deve exibir botões operacionais liberados para Almoxarife (ADMIN/STOCKIST)', (tester) async {
      tester.view.physicalSize = const Size(1800, 1200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      final repo = FakeReverseLogisticsRepo();
      await tester.pumpWidget(buildTestScreen(repo: repo, role: UserRole.admin, initialTab: 4));
      await tester.pumpAndSettle();

      // Ações operacionais
      expect(find.text('Receber na Quarentena (Almoxarifado)'), findsOneWidget);
      expect(find.text('Aprovar Teste de Bancada & Reteste Óptico'), findsOneWidget);
      expect(find.text('Reintegrar ao Saldo Ativo'), findsOneWidget);
      expect(find.text('Baixa como Sucata / Descarte'), findsOneWidget);
    });

    testWidgets('deve exibir mensagem limpa quando não houver ordens de comodato para recolhimento', (tester) async {
      tester.view.physicalSize = const Size(1800, 1200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      final repo = FakeReverseLogisticsRepo();
      // Remove a ordem de comodato
      repo.demands.removeWhere((d) => d.isRemoval);

      await tester.pumpWidget(buildTestScreen(repo: repo, initialTab: 4));
      await tester.pumpAndSettle();

      expect(
        find.text('Nenhuma ordem de recolhimento de comodato pendente no momento.'),
        findsOneWidget,
      );
    });
  });
}
