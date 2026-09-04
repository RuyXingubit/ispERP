import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/attendance/data/attendance_models.dart';
import 'package:isperp_app/features/attendance/data/attendance_notifier.dart';
import 'package:isperp_app/features/attendance/data/attendance_repository.dart';
import 'package:isperp_app/features/attendance/data/customer_search_model.dart';

class FakeAttendanceRepository implements AttendanceRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);

  @override
  Future<List<CustomerSearchModel>> searchCustomers(String query) async {
    if (query.contains('João')) {
      return [
        const CustomerSearchModel(
          id: '01a0674f-0001-7000-8000-000000000001',
          name: 'João Silva',
          cpf: '12345678901',
          city: 'Altamira',
          state: 'PA',
        ),
      ];
    }
    return [];
  }

  @override
  Future<List<ContractSummary>> getCustomerContracts(String customerId) async {
    return [
      const ContractSummary(
        id: '01a0674f-0002-7000-8000-000000000002',
        contractNumber: 'CTR-1001',
        status: 'ACTIVE',
        monthlyFee: 99.90,
        dueDay: 10,
        installationAddress: 'Rua 10, 50, Centro',
      ),
    ];
  }

  @override
  Future<List<PendingInvoice>> getCustomerInvoices(String customerId) async {
    return [
      const PendingInvoice(
        id: '01a0674f-0003-7000-8000-000000000003',
        contractId: '01a0674f-0002-7000-8000-000000000002',
        amount: 79.90,
        dueDate: '2026-08-22',
        status: 'PENDING',
      ),
    ];
  }

  @override
  Future<LiveWorkOrder?> getContractWorkOrder(String contractId) async {
    return const LiveWorkOrder(
      id: '01a0674f-0004-7000-8000-000000000004',
      type: 'REPAIR',
      status: 'IN_PROGRESS',
      technicianName: 'Pedro Alcântara',
      scheduledDate: '2026-09-04',
      scheduledPeriod: 'TARDE',
    );
  }

  @override
  Future<bool> payInvoice({
    required String invoiceId,
    required double amount,
    required String paymentMethod,
  }) async {
    return true;
  }
}

void main() {
  group('Attendance Models Unit Tests', () {
    test('Deve formatar CPF de 11 dígitos com pontos e traço', () {
      const customer = CustomerSearchModel(
        id: '123',
        name: 'Maria Clara',
        cpf: '52998224725',
      );
      expect(customer.formattedCpf, '529.982.247-25');
    });

    test('Deve calcular isOverdue de PendingInvoice corretamente', () {
      final invoicePast = PendingInvoice.fromJson({
        'id': 'inv-1',
        'contractId': 'ctr-1',
        'amount': 99.90,
        'dueDate': '2025-01-01',
        'status': 'PENDING',
      });
      expect(invoicePast.isOverdue, true);

      final invoiceFuture = PendingInvoice.fromJson({
        'id': 'inv-2',
        'contractId': 'ctr-1',
        'amount': 99.90,
        'dueDate': '2030-12-31',
        'status': 'PENDING',
      });
      expect(invoiceFuture.isOverdue, false);
    });

    test('Deve mapear status amigável de LiveWorkOrder', () {
      const woProgress = LiveWorkOrder(
        id: 'wo-1',
        type: 'REPAIR',
        status: 'IN_PROGRESS',
      );
      expect(woProgress.friendlyStatus, 'Técnico em deslocamento / atendimento');

      const woScheduled = LiveWorkOrder(
        id: 'wo-2',
        type: 'INSTALLATION',
        status: 'SCHEDULED',
      );
      expect(woScheduled.friendlyStatus, 'Agendada na rota');
    });
  });

  group('Attendance State Machine Tests', () {
    late FakeAttendanceRepository repository;
    late AttendanceNotifier notifier;

    setUp(() {
      repository = FakeAttendanceRepository();
      notifier = AttendanceNotifier(repository);
    });

    test('Deve inicializar no passo channelAndSearch com canal presencial', () {
      expect(notifier.state.step, AttendanceStep.channelAndSearch);
      expect(notifier.state.channel, AttendanceChannel.presential);
    });

    test('Deve alterar o canal de atendimento para WhatsApp', () {
      notifier.setChannel(AttendanceChannel.whatsapp);
      expect(notifier.state.channel, AttendanceChannel.whatsapp);
    });

    test('Deve buscar e selecionar cliente avançando para o raio-x contextual', () async {
      await notifier.searchCustomers('João');
      expect(notifier.state.searchResults, hasLength(1));
      expect(notifier.state.searchResults.first.name, 'João Silva');

      final customer = notifier.state.searchResults.first;
      await notifier.selectCustomer(customer);

      expect(notifier.state.step, AttendanceStep.overview);
      expect(notifier.state.selectedCustomer?.name, 'João Silva');
      expect(notifier.state.contracts, hasLength(1));
      expect(notifier.state.invoices, hasLength(1));
      expect(notifier.state.liveWorkOrder?.technicianName, 'Pedro Alcântara');
    });

    test('Deve avançar da seleção de intenção para pagamento e liquidar', () async {
      const customer = CustomerSearchModel(id: 'c1', name: 'João', cpf: '123');
      await notifier.selectCustomer(customer);

      notifier.selectIntent(AttendanceIntent.payment);
      expect(notifier.state.step, AttendanceStep.payment);

      notifier.setPaymentMethod('DEBIT_CARD');
      expect(notifier.state.paymentMethod, 'DEBIT_CARD');

      final success = await notifier.processPayment();
      expect(success, true);
      expect(notifier.state.step, AttendanceStep.receiptEvidence);
      expect(notifier.state.paymentReceiptPending, true);
    });

    test('Deve retornar passos com goBack e limpar com reset', () {
      notifier.selectIntent(AttendanceIntent.payment);
      expect(notifier.state.step, AttendanceStep.payment);

      notifier.goBack();
      expect(notifier.state.step, AttendanceStep.overview);

      notifier.reset();
      expect(notifier.state.step, AttendanceStep.channelAndSearch);
      expect(notifier.state.selectedCustomer, isNull);
    });
  });
}
