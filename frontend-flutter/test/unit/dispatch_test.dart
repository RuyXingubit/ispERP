import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/dispatch/data/dispatch_models.dart';
import 'package:isperp_app/features/dispatch/data/dispatch_notifier.dart';
import 'package:isperp_app/features/dispatch/data/dispatch_repository.dart';

class FakeDispatchRepository implements DispatchRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);

  final List<InstallationDemandModel> dummyDemands = [
    InstallationDemandModel(
      id: '01912345-0000-7000-8000-000000000001',
      workOrderId: '01912345-0000-7000-8000-000000000010',
      contractId: '01912345-0000-7000-8000-000000000100',
      contractNumber: 'CTR-2026-001',
      customerName: 'Ruy Barbosa Borges França',
      customerAddress: 'Av. Brigadeiro Eduardo Gomes, 3554',
      ctoName: 'CTO-ALT-01',
      ctoPortNumber: 2,
      estimatedDropMeters: 65,
      onuModelRequired: 'ONT Wi-Fi Dual-Band GPON Gigabit',
      fastConnectorsCount: 2,
      ptoRosetteCount: 1,
      status: MaterialDemandStatus.pendingAllocation,
    ),
    InstallationDemandModel(
      id: '01912345-0000-7000-8000-000000000002',
      workOrderId: '01912345-0000-7000-8000-000000000020',
      contractId: '01912345-0000-7000-8000-000000000200',
      contractNumber: 'CTR-2026-002',
      customerName: 'Maria Silva Santos',
      customerAddress: 'Rua Sete de Setembro, 120',
      estimatedDropMeters: 45,
      onuModelRequired: 'ONT Wi-Fi 6 AX3000',
      fastConnectorsCount: 2,
      ptoRosetteCount: 1,
      status: MaterialDemandStatus.allocatedVehicle,
    ),
  ];

  @override
  Future<List<InstallationDemandModel>> listDemands() async {
    return dummyDemands;
  }

  @override
  Future<InstallationDemandModel?> getDemand(String workOrderId) async {
    final found = dummyDemands.where((d) => d.workOrderId == workOrderId).toList();
    return found.isNotEmpty ? found.first : null;
  }

  @override
  Future<List<TechnicianCandidateModel>> listCandidates(String workOrderId) async {
    return [
      const TechnicianCandidateModel(
        technicianId: 'tech-001',
        technicianName: 'Pedro Henrique',
        vehicleWarehouseName: 'Fiorino 01 - Pedro',
        hasCompleteKit: true,
        hasOnu: true,
        hasDropCable: true,
        hasConnectors: true,
        dropCableBalanceMeters: 250,
        distanceKmToCustomer: 2.3,
        recommendedScore: 92.5,
      ),
      const TechnicianCandidateModel(
        technicianId: 'tech-002',
        technicianName: 'Carlos Ferreira',
        vehicleWarehouseName: 'Saveiro 02 - Carlos',
        hasCompleteKit: false,
        hasOnu: false,
        hasDropCable: true,
        hasConnectors: true,
        dropCableBalanceMeters: 100,
        distanceKmToCustomer: 5.8,
        recommendedScore: 45.0,
      ),
    ];
  }

  @override
  Future<bool> dispatchWorkOrder(String workOrderId, String technicianId) async {
    return true;
  }
}

void main() {
  group('Dispatch Models Unit Tests', () {
    test('MaterialDemandStatus deve converter strings e labels corretamente', () {
      expect(MaterialDemandStatus.fromString('PENDING_ALLOCATION'), equals(MaterialDemandStatus.pendingAllocation));
      expect(MaterialDemandStatus.fromString('ALLOCATED_VEHICLE'), equals(MaterialDemandStatus.allocatedVehicle));
      expect(MaterialDemandStatus.fromString('ALLOCATED_CENTRAL'), equals(MaterialDemandStatus.allocatedCentral));
      expect(MaterialDemandStatus.fromString('CONSUMED_IN_FIELD'), equals(MaterialDemandStatus.consumedInField));
      expect(MaterialDemandStatus.fromString(null), equals(MaterialDemandStatus.pendingAllocation));

      expect(MaterialDemandStatus.pendingAllocation.label, equals('Aguardando Triagem'));
      expect(MaterialDemandStatus.allocatedVehicle.label, equals('Alocado no Veículo'));
    });

    test('InstallationDemandModel deve desserializar JSON com precisão', () {
      final json = {
        'id': '01912345-0000-7000-8000-000000000001',
        'workOrderId': '01912345-0000-7000-8000-000000000010',
        'contractId': '01912345-0000-7000-8000-000000000100',
        'contractNumber': 'CTR-2026-001',
        'customerName': 'Ruy Barbosa Borges França',
        'customerAddress': 'Av. Brigadeiro Eduardo Gomes, 3554',
        'customerLatitude': -3.2033,
        'customerLongitude': -52.2064,
        'ctoName': 'CTO-ALT-01',
        'ctoPortNumber': 2,
        'estimatedDropMeters': 65,
        'onuModelRequired': 'ONT Wi-Fi Dual-Band GPON Gigabit',
        'fastConnectorsCount': 2,
        'ptoRosetteCount': 1,
        'status': 'PENDING_ALLOCATION',
      };

      final model = InstallationDemandModel.fromJson(json);
      expect(model.id, equals('01912345-0000-7000-8000-000000000001'));
      expect(model.customerName, equals('Ruy Barbosa Borges França'));
      expect(model.estimatedDropMeters, equals(65));
      expect(model.onuModelRequired, equals('ONT Wi-Fi Dual-Band GPON Gigabit'));
      expect(model.ctoPortNumber, equals(2));
      expect(model.status, equals(MaterialDemandStatus.pendingAllocation));
    });

    test('TechnicianCandidateModel deve desserializar JSON e pontuação recomendada', () {
      final json = {
        'technicianId': 'tech-001',
        'technicianName': 'Pedro Henrique',
        'vehicleWarehouseName': 'Fiorino 01',
        'hasCompleteKit': true,
        'hasOnu': true,
        'hasDropCable': true,
        'hasConnectors': true,
        'dropCableBalanceMeters': 300,
        'distanceKmToCustomer': 1.8,
        'recommendedScore': 95.0,
      };

      final model = TechnicianCandidateModel.fromJson(json);
      expect(model.technicianId, equals('tech-001'));
      expect(model.hasCompleteKit, isTrue);
      expect(model.hasOnu, isTrue);
      expect(model.dropCableBalanceMeters, equals(300));
      expect(model.distanceKmToCustomer, equals(1.8));
      expect(model.recommendedScore, equals(95.0));
    });
  });

  group('DispatchNotifier State Machine Tests', () {
    late FakeDispatchRepository fakeRepo;
    late DispatchNotifier notifier;

    setUp(() {
      fakeRepo = FakeDispatchRepository();
      notifier = DispatchNotifier(fakeRepo);
    });

    test('Deve inicializar carregando demandas e selecionando a primeira com candidatos', () async {
      await Future.delayed(const Duration(milliseconds: 10));
      expect(notifier.state.isLoadingDemands, isFalse);
      expect(notifier.state.demands.length, equals(2));
      expect(notifier.state.selectedDemand, isNotNull);
      expect(notifier.state.selectedDemand!.customerName, equals('Ruy Barbosa Borges França'));
      expect(notifier.state.candidates.length, equals(2));
      expect(notifier.state.candidates.first.hasCompleteKit, isTrue);
    });

    test('Deve filtrar demandas pelas abas', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      // Aba 0: Pendentes
      notifier.setFilterTab(0);
      expect(notifier.state.filteredDemands.length, equals(1));
      expect(notifier.state.filteredDemands.first.status, equals(MaterialDemandStatus.pendingAllocation));

      // Aba 1: Alocadas / Em campo
      notifier.setFilterTab(1);
      expect(notifier.state.filteredDemands.length, equals(1));
      expect(notifier.state.filteredDemands.first.status, equals(MaterialDemandStatus.allocatedVehicle));
    });

    test('Deve despachar O.S. para o técnico com sucesso e exibir mensagem', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      final success = await notifier.dispatchToTechnician('tech-001');
      expect(success, isTrue);
      expect(notifier.state.isDispatching, isFalse);
      expect(notifier.state.dispatchSuccessMessage, contains('despachada com sucesso para Pedro Henrique'));
    });
  });
}
