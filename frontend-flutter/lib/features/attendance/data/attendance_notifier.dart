import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'attendance_models.dart';
import 'attendance_repository.dart';
import 'customer_search_model.dart';

/// Passos do fluxo guiado de atendimento.
enum AttendanceStep {
  channelAndSearch,
  overview,
  payment,
  receiptEvidence,
}

/// Estado imutável do atendimento guiado.
class AttendanceState {
  final AttendanceStep step;
  final AttendanceChannel channel;
  final AttendanceIntent? selectedIntent;
  final String searchQuery;
  final bool isSearching;
  final List<CustomerSearchModel> searchResults;
  final CustomerSearchModel? selectedCustomer;
  final List<ContractSummary> contracts;
  final ContractSummary? selectedContract;
  final List<PendingInvoice> invoices;
  final PendingInvoice? selectedInvoice;
  final LiveWorkOrder? liveWorkOrder;
  final String paymentMethod;
  final bool isProcessingPayment;
  final bool paymentSuccess;
  final bool paymentReceiptPending;
  final String? errorMessage;

  const AttendanceState({
    this.step = AttendanceStep.channelAndSearch,
    this.channel = AttendanceChannel.presential,
    this.selectedIntent,
    this.searchQuery = '',
    this.isSearching = false,
    this.searchResults = const [],
    this.selectedCustomer,
    this.contracts = const [],
    this.selectedContract,
    this.invoices = const [],
    this.selectedInvoice,
    this.liveWorkOrder,
    this.paymentMethod = 'DEBIT_CARD',
    this.isProcessingPayment = false,
    this.paymentSuccess = false,
    this.paymentReceiptPending = false,
    this.errorMessage,
  });

  AttendanceState copyWith({
    AttendanceStep? step,
    AttendanceChannel? channel,
    AttendanceIntent? selectedIntent,
    String? searchQuery,
    bool? isSearching,
    List<CustomerSearchModel>? searchResults,
    CustomerSearchModel? selectedCustomer,
    List<ContractSummary>? contracts,
    ContractSummary? selectedContract,
    List<PendingInvoice>? invoices,
    PendingInvoice? selectedInvoice,
    LiveWorkOrder? liveWorkOrder,
    String? paymentMethod,
    bool? isProcessingPayment,
    bool? paymentSuccess,
    bool? paymentReceiptPending,
    String? errorMessage,
  }) {
    return AttendanceState(
      step: step ?? this.step,
      channel: channel ?? this.channel,
      selectedIntent: selectedIntent ?? this.selectedIntent,
      searchQuery: searchQuery ?? this.searchQuery,
      isSearching: isSearching ?? this.isSearching,
      searchResults: searchResults ?? this.searchResults,
      selectedCustomer: selectedCustomer ?? this.selectedCustomer,
      contracts: contracts ?? this.contracts,
      selectedContract: selectedContract ?? this.selectedContract,
      invoices: invoices ?? this.invoices,
      selectedInvoice: selectedInvoice ?? this.selectedInvoice,
      liveWorkOrder: liveWorkOrder ?? this.liveWorkOrder,
      paymentMethod: paymentMethod ?? this.paymentMethod,
      isProcessingPayment: isProcessingPayment ?? this.isProcessingPayment,
      paymentSuccess: paymentSuccess ?? this.paymentSuccess,
      paymentReceiptPending: paymentReceiptPending ?? this.paymentReceiptPending,
      errorMessage: errorMessage,
    );
  }
}

/// Notifier do fluxo de atendimento guiado.
class AttendanceNotifier extends StateNotifier<AttendanceState> {
  final AttendanceRepository _repository;

  AttendanceNotifier(this._repository) : super(const AttendanceState());

  /// Define o canal de entrada do atendimento (Presencial, Telefone, WhatsApp, E-mail).
  void setChannel(AttendanceChannel channel) {
    state = state.copyWith(channel: channel);
  }

  /// Executa a busca em tempo real de clientes por CPF ou Nome.
  Future<void> searchCustomers(String query) async {
    state = state.copyWith(searchQuery: query, isSearching: true, errorMessage: null);

    if (query.trim().isEmpty) {
      state = state.copyWith(isSearching: false, searchResults: []);
      return;
    }

    final results = await _repository.searchCustomers(query);
    state = state.copyWith(
      isSearching: false,
      searchResults: results,
    );
  }

  /// Seleciona o cliente e carrega automaticamente seu raio-x (contratos, faturas e O.S. ativa).
  Future<void> selectCustomer(CustomerSearchModel customer) async {
    state = state.copyWith(
      selectedCustomer: customer,
      isSearching: true,
      errorMessage: null,
    );

    final contracts = await _repository.getCustomerContracts(customer.id);
    final invoices = await _repository.getCustomerInvoices(customer.id);
    final selectedContract = contracts.isNotEmpty ? contracts.first : null;

    LiveWorkOrder? liveWo;
    if (selectedContract != null) {
      liveWo = await _repository.getContractWorkOrder(selectedContract.id);
    }

    final pendingInvoices = invoices.where((i) => i.status.toUpperCase() != 'PAID').toList();
    final selectedInvoice = pendingInvoices.isNotEmpty ? pendingInvoices.first : null;

    state = state.copyWith(
      step: AttendanceStep.overview,
      isSearching: false,
      contracts: contracts,
      selectedContract: selectedContract,
      invoices: pendingInvoices,
      selectedInvoice: selectedInvoice,
      liveWorkOrder: liveWo,
    );
  }

  /// Define a intenção do cliente no atendimento.
  void selectIntent(AttendanceIntent intent) {
    state = state.copyWith(selectedIntent: intent);
    if (intent == AttendanceIntent.payment) {
      state = state.copyWith(step: AttendanceStep.payment);
    }
  }

  /// Altera a fatura selecionada para pagamento.
  void selectInvoice(PendingInvoice invoice) {
    state = state.copyWith(selectedInvoice: invoice);
  }

  /// Altera a forma de pagamento (DEBIT_CARD, CREDIT_CARD, PIX, CASH).
  void setPaymentMethod(String method) {
    state = state.copyWith(paymentMethod: method);
  }

  /// Executa a liquidação da fatura no backend e entra no passo de comprovação/custódia.
  Future<bool> processPayment() async {
    final invoice = state.selectedInvoice;
    if (invoice == null) {
      state = state.copyWith(errorMessage: 'Nenhuma fatura selecionada.');
      return false;
    }

    state = state.copyWith(isProcessingPayment: true, errorMessage: null);

    final success = await _repository.payInvoice(
      invoiceId: invoice.id,
      amount: invoice.amount,
      paymentMethod: state.paymentMethod,
    );

    if (success) {
      final isCard = state.paymentMethod == 'DEBIT_CARD' || state.paymentMethod == 'CREDIT_CARD';
      state = state.copyWith(
        isProcessingPayment: false,
        paymentSuccess: true,
        paymentReceiptPending: isCard,
        step: AttendanceStep.receiptEvidence,
      );
      return true;
    } else {
      state = state.copyWith(
        isProcessingPayment: false,
        errorMessage: 'Falha ao processar recebimento no servidor.',
      );
      return false;
    }
  }

  /// Avança para a etapa anterior ou fecha.
  void goBack() {
    switch (state.step) {
      case AttendanceStep.overview:
        state = state.copyWith(step: AttendanceStep.channelAndSearch);
        break;
      case AttendanceStep.payment:
        state = state.copyWith(step: AttendanceStep.overview);
        break;
      case AttendanceStep.receiptEvidence:
        state = state.copyWith(step: AttendanceStep.payment);
        break;
      case AttendanceStep.channelAndSearch:
        break;
    }
  }

  /// Reseta o fluxo para um novo atendimento.
  void reset() {
    state = const AttendanceState();
  }
}

/// Provider reativo do fluxo de atendimento.
final attendanceProvider = StateNotifierProvider<AttendanceNotifier, AttendanceState>((ref) {
  final repo = ref.watch(attendanceRepositoryProvider);
  return AttendanceNotifier(repo);
});
