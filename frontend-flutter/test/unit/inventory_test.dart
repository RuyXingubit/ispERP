import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/dispatch/data/dispatch_models.dart';
import 'package:isperp_app/features/inventory/data/inventory_models.dart';
import 'package:isperp_app/features/inventory/data/inventory_notifier.dart';
import 'package:isperp_app/features/inventory/data/inventory_repository.dart';

class FakeInventoryRepository implements InventoryRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);

  final List<WarehouseModel> dummyWarehouses = [
    const WarehouseModel(
      id: '01a0674e-eb97-7373-b812-6b3fd5205f59',
      code: 'DEP-ATM-CENTRAL',
      name: 'Depósito Central Altamira',
      city: 'Altamira',
      state: 'PA',
      responsibleName: 'João Almoxarife',
      responsibleCpf: '123.456.789-00',
    ),
    const WarehouseModel(
      id: '01a0674e-eb97-7e9d-85a6-c0b7662db2ca',
      code: 'ESC-VTX-APOIO',
      name: 'Ponto de Apoio Vitória do Xingu',
      city: 'Vitória do Xingu',
      state: 'PA',
      responsibleName: 'Renata Supervisora',
      responsibleCpf: '987.654.321-99',
    ),
  ];

  final List<InventoryItemModel> dummyItems = [
    const InventoryItemModel(
      id: '01a0674e-eb55-7a47-ae2c-5ab0e1b06675',
      code: 'DROP-OPT-1FO',
      name: 'Cabo Drop Óptico Compacto 1FO',
      category: 'CABO_DROP',
      quantityInStock: 4900,
      minQuantity: 500,
      unit: 'METROS',
    ),
    const InventoryItemModel(
      id: '01a0674e-eb55-74cd-8e83-1c037e3ed0e0',
      code: 'ONT-WIFI6-XPON',
      name: 'ONU / ONT XPON Wi-Fi 6 Gigabit',
      category: 'ONU_ONT',
      quantityInStock: 5,
      minQuantity: 20, // Crítico
      unit: 'UN',
    ),
  ];

  final List<InstallationDemandModel> dummyDemands = [
    InstallationDemandModel(
      id: '01912345-0000-7000-8000-000000000001',
      workOrderId: '01912345-0000-7000-8000-000000000010',
      contractId: '01912345-0000-7000-8000-000000000100',
      contractNumber: 'CTR-2026-001',
      customerName: 'Ruy Barbosa Borges França',
      customerAddress: 'Av. Brigadeiro Eduardo Gomes, 3554',
      estimatedDropMeters: 65,
      onuModelRequired: 'ONT Wi-Fi Dual-Band GPON Gigabit',
      fastConnectorsCount: 2,
      ptoRosetteCount: 1,
      status: MaterialDemandStatus.pendingAllocation,
    ),
  ];

  final List<StockTransferModel> dummyTransfers = [
    const StockTransferModel(
      id: '01a0674e-trf-0001',
      code: 'TRF-98214',
      originWarehouseId: '01a0674e-eb97-7373-b812-6b3fd5205f59',
      destinationWarehouseId: '01a0674e-eb97-7e9d-85a6-c0b7662db2ca',
      carrierName: 'João Técnico',
      carrierDocument: '123.456.789-00',
      status: TransferStatus.inTransit,
    ),
  ];

  @override
  Future<List<WarehouseModel>> listWarehouses() async => dummyWarehouses;

  @override
  Future<List<InventoryItemModel>> listInventoryItems() async => dummyItems;

  @override
  Future<List<InstallationDemandModel>> listInstallationDemands() async => dummyDemands;

  @override
  Future<List<StockTransferModel>> listTransfers() async => dummyTransfers;

  @override
  Future<bool> confirmStockAllocation(String workOrderId, {String? warehouseId}) async => true;

  @override
  Future<InventoryItemModel?> registerStockEntry(StockEntryPayload payload) async {
    return InventoryItemModel(
      id: '01a0-new-item',
      code: payload.itemCode,
      name: payload.itemName,
      category: payload.category,
      quantityInStock: payload.quantity,
      minQuantity: 10,
      unit: payload.unit,
    );
  }

  @override
  Future<StockTransferModel?> createTransfer({
    required String originWarehouseId,
    required String destinationWarehouseId,
    required String carrierName,
    required String carrierDocument,
    String? carrierUserId,
    String carrierType = 'COLABORADOR',
    String? notes,
  }) async {
    return StockTransferModel(
      id: '01a0-new-transfer',
      code: 'TRF-12399',
      originWarehouseId: originWarehouseId,
      destinationWarehouseId: destinationWarehouseId,
      carrierName: carrierName,
      carrierDocument: carrierDocument,
      status: TransferStatus.pending,
    );
  }

  @override
  Future<bool> dispatchTransfer(String transferId, {String? dispatchPhotoUrl, String? userId}) async => true;

  @override
  Future<bool> confirmReceiptTransfer(String transferId, {String? receiptPhotoUrl, String? userId}) async => true;

  @override
  Future<bool> checkoutMaterialForWorkOrder(MaterialCheckoutPayload payload) async => true;

  @override
  Future<MaterialCheckinResult?> checkinMaterialForWorkOrder(MaterialCheckinPayload payload) async {
    final expected = payload.initialMetersOrQty - payload.consumedMetersOrQty;
    final divergence = payload.actualRemainingMetersOrQty - expected;
    return MaterialCheckinResult(
      logId: '01a0-log-1',
      workOrderId: payload.workOrderId,
      technicianUserId: payload.technicianUserId,
      status: divergence != 0 ? 'DIVERGENT' : 'CONFORMANT',
      hasDivergence: divergence != 0,
      expectedRemaining: expected,
      actualRemaining: payload.actualRemainingMetersOrQty,
      divergenceQuantity: divergence,
      beforePhotoUrl: payload.beforePhotoUrl,
      installedPhotoUrl: payload.installedPhotoUrl,
      returnPhotoUrl: payload.returnPhotoUrl,
      notes: payload.notes,
    );
  }
}

void main() {
  group('Inventory Models Unit Tests', () {
    test('WarehouseModel deve desserializar JSON com dados do responsável', () {
      final json = {
        'id': '01a0674e-eb97-7373-b812-6b3fd5205f59',
        'code': 'DEP-ATM-CENTRAL',
        'name': 'Depósito Central Altamira',
        'city': 'Altamira',
        'state': 'PA',
        'responsibleName': 'João Almoxarife',
        'responsibleCpf': '123.456.789-00',
        'active': true,
      };

      final model = WarehouseModel.fromJson(json);
      expect(model.id, equals('01a0674e-eb97-7373-b812-6b3fd5205f59'));
      expect(model.code, equals('DEP-ATM-CENTRAL'));
      expect(model.responsibleName, equals('João Almoxarife'));
      expect(model.responsibleCpf, equals('123.456.789-00'));
    });

    test('InventoryItemModel deve detectar nível crítico de estoque', () {
      const normalItem = InventoryItemModel(
        id: '1',
        code: 'CABO',
        name: 'Cabo Drop',
        category: 'CABO',
        quantityInStock: 1000,
        minQuantity: 200,
      );
      expect(normalItem.isCriticalStock, isFalse);

      const criticalItem = InventoryItemModel(
        id: '2',
        code: 'ONT',
        name: 'ONT Wi-Fi 6',
        category: 'ONU',
        quantityInStock: 2,
        minQuantity: 10,
      );
      expect(criticalItem.isCriticalStock, isTrue);
    });

    test('TransferStatus deve converter rótulos corretamente', () {
      expect(TransferStatus.fromString('IN_TRANSIT'), equals(TransferStatus.inTransit));
      expect(TransferStatus.fromString('RECEIVED'), equals(TransferStatus.received));
      expect(TransferStatus.fromString('PENDING'), equals(TransferStatus.pending));
      expect(TransferStatus.inTransit.label, equals('Em Trânsito'));
      expect(TransferStatus.received.label, equals('Recebido / Conferido'));
    });
  });

  group('InventoryNotifier State Machine Tests', () {
    late FakeInventoryRepository fakeRepo;
    late InventoryNotifier notifier;

    setUp(() {
      fakeRepo = FakeInventoryRepository();
      notifier = InventoryNotifier(fakeRepo);
    });

    test('Deve inicializar carregando depósitos, itens, demandas e transferências', () async {
      await Future.delayed(const Duration(milliseconds: 10));
      expect(notifier.state.isLoading, isFalse);
      expect(notifier.state.warehouses.length, equals(2));
      expect(notifier.state.items.length, equals(2));
      expect(notifier.state.demands.length, equals(1));
      expect(notifier.state.transfers.length, equals(1));
      expect(notifier.state.pendingDemandsCount, equals(1));
      expect(notifier.state.criticalItemsCount, equals(1));
      expect(notifier.state.selectedWarehouse?.code, equals('DEP-ATM-CENTRAL'));
    });

    test('Deve alternar de aba e selecionar base de atendimento', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      notifier.setTab(1);
      expect(notifier.state.selectedTab, equals(1));

      notifier.selectWarehouse(fakeRepo.dummyWarehouses[1]);
      expect(notifier.state.selectedWarehouse?.city, equals('Vitória do Xingu'));
    });

    test('Deve confirmar materiais no almoxarifado com sucesso', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      final success = await notifier.confirmStockForWorkOrder('01912345-0000-7000-8000-000000000010');
      expect(success, isTrue);
      expect(notifier.state.successMessage, contains('confirmados e separados com sucesso'));
    });

    test('Deve registrar entrada de novo lote de material', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      const payload = StockEntryPayload(
        itemCode: 'BOB-FIBRA-12FO',
        itemName: 'Bobina de Fibra 12FO AS80',
        category: 'CABO_FIBRA',
        quantity: 10,
        unit: 'BOB',
      );

      final success = await notifier.registerStockEntry(payload);
      expect(success, isTrue);
      expect(notifier.state.successMessage, contains('Entrada de 10 BOB'));
    });

    test('Deve autorizar retirada de material vinculado à O.S.', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      const payload = MaterialCheckoutPayload(
        workOrderId: '01912345-0000-7000-8000-000000000010',
        technicianUserId: 'tech-001',
        quantityOrMeters: 2000,
      );

      final success = await notifier.checkoutMaterial(payload);
      expect(success, isTrue);
      expect(notifier.state.successMessage, contains('Retirada autorizada e vinculada à O.S.'));
    });

    test('Deve apurar devolução com divergência e registrar ressalva', () async {
      await Future.delayed(const Duration(milliseconds: 10));

      // Inicial: 2000, Consumido: 500 -> Esperava 1500. Apurado: 1200 (faltam 300)
      const payload = MaterialCheckinPayload(
        workOrderId: '01912345-0000-7000-8000-000000000010',
        technicianUserId: 'tech-001',
        initialMetersOrQty: 2000,
        consumedMetersOrQty: 500,
        actualRemainingMetersOrQty: 1200,
      );

      final result = await notifier.checkinMaterial(payload);
      expect(result, isNotNull);
      expect(result!.hasDivergence, isTrue);
      expect(result.divergenceQuantity, equals(-300));
      expect(result.status, equals('DIVERGENT'));
      expect(notifier.state.successMessage, contains('RESSALVA DE DIVERGÊNCIA'));
    });
  });
}
