import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'sales_models.dart';
import 'sales_repository.dart';

/// Passos da esteira guiada de Venda Expressa.
enum SalesOnboardingStep {
  feasibility,
  planSelection,
  customerData,
  confirmation,
}

/// Estado imutável do Onboarding de Venda Expressa.
class SalesOnboardingState {
  final SalesOnboardingStep step;

  // 1. Endereço e Viabilidade Inteligente GeoCEP
  final String searchQuery;
  final bool isSearchingAddress;
  final List<CepLookupModel> searchSuggestions;
  final String cep;
  final bool isSearchingCep;
  final String street;
  final String number;
  final String complement;
  final String neighborhood;
  final String city;
  final String state;
  final double? latitude;
  final double? longitude;
  final bool isCheckingFeasibility;
  final FtthFeasibilityModel? feasibility;

  // 2. Plano e Vencimento
  final List<CommercialPlan> plans;
  final bool isLoadingPlans;
  final CommercialPlan? selectedPlan;
  final int preferredDueDate;

  // 3. Dados do Assinante
  final String customerCpf;
  final bool isCpfValid;
  final String customerName;
  final String customerPhone;
  final String customerEmail;
  final String notificationChannel;

  // 4. Submissão e Resultado
  final bool isSubmitting;
  final SaleResult? saleResult;
  final String? errorMessage;

  const SalesOnboardingState({
    this.step = SalesOnboardingStep.feasibility,
    this.searchQuery = '',
    this.isSearchingAddress = false,
    this.searchSuggestions = const [],
    this.cep = '',
    this.isSearchingCep = false,
    this.street = '',
    this.number = '',
    this.complement = '',
    this.neighborhood = '',
    this.city = '',
    this.state = '',
    this.latitude,
    this.longitude,
    this.isCheckingFeasibility = false,
    this.feasibility,
    this.plans = const [],
    this.isLoadingPlans = false,
    this.selectedPlan,
    this.preferredDueDate = 10,
    this.customerCpf = '',
    this.isCpfValid = false,
    this.customerName = '',
    this.customerPhone = '',
    this.customerEmail = '',
    this.notificationChannel = 'WHATSAPP',
    this.isSubmitting = false,
    this.saleResult,
    this.errorMessage,
  });

  SalesOnboardingState copyWith({
    SalesOnboardingStep? step,
    String? searchQuery,
    bool? isSearchingAddress,
    List<CepLookupModel>? searchSuggestions,
    String? cep,
    bool? isSearchingCep,
    String? street,
    String? number,
    String? complement,
    String? neighborhood,
    String? city,
    String? state,
    double? latitude,
    double? longitude,
    bool? isCheckingFeasibility,
    FtthFeasibilityModel? feasibility,
    List<CommercialPlan>? plans,
    bool? isLoadingPlans,
    CommercialPlan? selectedPlan,
    int? preferredDueDate,
    String? customerCpf,
    bool? isCpfValid,
    String? customerName,
    String? customerPhone,
    String? customerEmail,
    String? notificationChannel,
    bool? isSubmitting,
    SaleResult? saleResult,
    String? errorMessage,
    bool clearError = false,
  }) {
    return SalesOnboardingState(
      step: step ?? this.step,
      searchQuery: searchQuery ?? this.searchQuery,
      isSearchingAddress: isSearchingAddress ?? this.isSearchingAddress,
      searchSuggestions: searchSuggestions ?? this.searchSuggestions,
      cep: cep ?? this.cep,
      isSearchingCep: isSearchingCep ?? this.isSearchingCep,
      street: street ?? this.street,
      number: number ?? this.number,
      complement: complement ?? this.complement,
      neighborhood: neighborhood ?? this.neighborhood,
      city: city ?? this.city,
      state: state ?? this.state,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      isCheckingFeasibility: isCheckingFeasibility ?? this.isCheckingFeasibility,
      feasibility: feasibility ?? this.feasibility,
      plans: plans ?? this.plans,
      isLoadingPlans: isLoadingPlans ?? this.isLoadingPlans,
      selectedPlan: selectedPlan ?? this.selectedPlan,
      preferredDueDate: preferredDueDate ?? this.preferredDueDate,
      customerCpf: customerCpf ?? this.customerCpf,
      isCpfValid: isCpfValid ?? this.isCpfValid,
      customerName: customerName ?? this.customerName,
      customerPhone: customerPhone ?? this.customerPhone,
      customerEmail: customerEmail ?? this.customerEmail,
      notificationChannel: notificationChannel ?? this.notificationChannel,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      saleResult: saleResult ?? this.saleResult,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }
}

/// Notifier gerenciador do fluxo guiado de Venda Expressa.
class SalesOnboardingNotifier extends StateNotifier<SalesOnboardingState> {
  final SalesRepository _repository;

  SalesOnboardingNotifier(this._repository) : super(const SalesOnboardingState()) {
    loadActivePlans();
  }

  /// Carrega catálogo de planos comerciais ativos.
  Future<void> loadActivePlans() async {
    state = state.copyWith(isLoadingPlans: true, clearError: true);
    try {
      final plans = await _repository.getActivePlans();
      CommercialPlan? defaultSelected = state.selectedPlan;
      if (defaultSelected == null && plans.isNotEmpty) {
        defaultSelected = plans.first;
      }
      state = state.copyWith(
        plans: plans,
        isLoadingPlans: false,
        selectedPlan: defaultSelected,
      );
    } catch (e) {
      state = state.copyWith(
        isLoadingPlans: false,
        errorMessage: 'Não foi possível carregar os planos: $e',
      );
    }
  }

  /// Busca Inteligente GeoCEP (Auto-detecta: CEP, Coordenadas GPS ou Nome da Rua).
  Future<void> performSmartSearch(String input) async {
    final cleanInput = input.trim();
    if (cleanInput.isEmpty) return;

    state = state.copyWith(
      searchQuery: cleanInput,
      isSearchingAddress: true,
      searchSuggestions: const [],
      clearError: true,
    );

    // 1. Detecção de Coordenadas GPS (ex: "-3.2107, -52.2371" ou "-3.2107 -52.2371")
    final coordParts = cleanInput.split(RegExp(r'[,;\s]+')).where((s) => s.isNotEmpty).toList();
    if (coordParts.length == 2) {
      final lat = double.tryParse(coordParts[0]);
      final lon = double.tryParse(coordParts[1]);
      if (lat != null && lon != null && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
        try {
          final res = await _repository.reverseGeocode(lat, lon);
          if (res != null) {
            await _applyAddress(res);
            state = state.copyWith(isSearchingAddress: false);
            return;
          }
        } catch (_) {}
      }
    }

    // 2. Detecção de CEP (8 dígitos)
    final digitsOnly = CpfUtils.clean(cleanInput);
    if (digitsOnly.length == 8) {
      try {
        final res = await _repository.lookupCep(digitsOnly);
        if (res != null) {
          await _applyAddress(res);
          state = state.copyWith(isSearchingAddress: false);
          return;
        }
      } catch (_) {}
    }

    // 3. Busca Textual por Nome de Rua / Logradouro / Bairro
    try {
      final results = await _repository.searchAddress(cleanInput);
      if (results.isNotEmpty) {
        if (results.length == 1) {
          // Apenas um resultado: preenche automaticamente
          await _applyAddress(results.first);
          state = state.copyWith(isSearchingAddress: false, searchSuggestions: const []);
        } else {
          // Múltiplos resultados: lista sugestões para o usuário escolher
          state = state.copyWith(
            isSearchingAddress: false,
            searchSuggestions: results,
          );
        }
        return;
      }
    } catch (_) {}

    state = state.copyWith(
      isSearchingAddress: false,
      errorMessage: 'Nenhum endereço encontrado para "$cleanInput". Verifique a grafia ou informe o CEP.',
    );
  }

  /// Seleciona uma das sugestões retornadas pela busca de rua.
  Future<void> selectSuggestion(CepLookupModel suggestion) async {
    await _applyAddress(suggestion);
    state = state.copyWith(searchSuggestions: const []);
  }

  void clearSuggestions() {
    state = state.copyWith(searchSuggestions: const []);
  }

  Future<void> _applyAddress(CepLookupModel res) async {
    state = state.copyWith(
      cep: res.cep.isNotEmpty ? res.cep : state.cep,
      street: res.street.isNotEmpty ? res.street : state.street,
      neighborhood: res.neighborhood.isNotEmpty ? res.neighborhood : state.neighborhood,
      city: res.city.isNotEmpty ? res.city : state.city,
      state: res.state.isNotEmpty ? res.state : state.state,
      latitude: res.latitude,
      longitude: res.longitude,
    );

    // Se possui coordenadas geográficas válidas, checa viabilidade de fibra imediatamente
    if (res.latitude != null && res.longitude != null) {
      await checkFeasibility(res.latitude!, res.longitude!);
    }
  }

  /// Atualiza e busca CEP automaticamente quando completar 8 dígitos.
  Future<void> setCep(String raw) async {
    state = state.copyWith(cep: raw, clearError: true);
    final cleanDigits = CpfUtils.clean(raw);
    if (cleanDigits.length == 8) {
      await searchCep(cleanDigits);
    }
  }

  /// Dispara busca de CEP no backend GeoCep.
  Future<void> searchCep(String cleanCep) async {
    state = state.copyWith(isSearchingCep: true, clearError: true);
    try {
      final result = await _repository.lookupCep(cleanCep);
      if (result != null) {
        await _applyAddress(result);
        state = state.copyWith(isSearchingCep: false);
      } else {
        state = state.copyWith(
          isSearchingCep: false,
          errorMessage: 'CEP não encontrado na base de endereçamento.',
        );
      }
    } catch (_) {
      state = state.copyWith(
        isSearchingCep: false,
        errorMessage: 'Erro ao consultar CEP.',
      );
    }
  }

  /// Checagem matemática de viabilidade óptica contra CTOs reais.
  Future<void> checkFeasibility(double lat, double lng) async {
    state = state.copyWith(isCheckingFeasibility: true, clearError: true);
    try {
      final res = await _repository.checkFeasibility(lat, lng);
      state = state.copyWith(
        isCheckingFeasibility: false,
        feasibility: res,
      );
    } catch (_) {
      state = state.copyWith(
        isCheckingFeasibility: false,
      );
    }
  }

  // Atualização manual de campos de endereço
  void setStreet(String val) => state = state.copyWith(street: val);
  void setNumber(String val) => state = state.copyWith(number: val);
  void setComplement(String val) => state = state.copyWith(complement: val);
  void setNeighborhood(String val) => state = state.copyWith(neighborhood: val);
  void setCity(String val) => state = state.copyWith(city: val);
  void setStateUf(String val) => state = state.copyWith(state: val);

  // Seleção de Plano e Condição
  void selectPlan(CommercialPlan plan) => state = state.copyWith(selectedPlan: plan);
  void setPreferredDueDate(int day) => state = state.copyWith(preferredDueDate: day);

  // Dados do Assinante
  void setCustomerCpf(String raw) {
    final clean = CpfUtils.clean(raw);
    final isValid = CpfUtils.isValid(clean);
    state = state.copyWith(
      customerCpf: clean,
      isCpfValid: isValid,
      clearError: true,
    );
  }

  void setCustomerName(String val) => state = state.copyWith(customerName: val);
  void setCustomerPhone(String val) => state = state.copyWith(customerPhone: val);
  void setCustomerEmail(String val) => state = state.copyWith(customerEmail: val);
  void setNotificationChannel(String val) => state = state.copyWith(notificationChannel: val);

  /// Validação antes de avançar cada etapa.
  bool canAdvanceFromCurrentStep() {
    switch (state.step) {
      case SalesOnboardingStep.feasibility:
        return state.street.trim().isNotEmpty &&
            state.number.trim().isNotEmpty &&
            state.city.trim().isNotEmpty &&
            state.state.trim().isNotEmpty;

      case SalesOnboardingStep.planSelection:
        return state.selectedPlan != null;

      case SalesOnboardingStep.customerData:
        return state.isCpfValid &&
            state.customerName.trim().length >= 3 &&
            state.customerPhone.replaceAll(RegExp(r'\D'), '').length >= 10;

      case SalesOnboardingStep.confirmation:
        return state.saleResult != null;
    }
  }

  /// Avança para o próximo passo.
  void nextStep() {
    state = state.copyWith(clearError: true);
    switch (state.step) {
      case SalesOnboardingStep.feasibility:
        if (!canAdvanceFromCurrentStep()) {
          state = state.copyWith(errorMessage: 'Preencha o endereço completo (Rua, Número, Cidade e UF).');
          return;
        }
        state = state.copyWith(step: SalesOnboardingStep.planSelection);
        break;

      case SalesOnboardingStep.planSelection:
        if (state.selectedPlan == null) {
          state = state.copyWith(errorMessage: 'Selecione um plano comercial para continuar.');
          return;
        }
        state = state.copyWith(step: SalesOnboardingStep.customerData);
        break;

      case SalesOnboardingStep.customerData:
        if (!state.isCpfValid) {
          state = state.copyWith(errorMessage: 'Informe um CPF válido com 11 dígitos.');
          return;
        }
        if (state.customerName.trim().length < 3) {
          state = state.copyWith(errorMessage: 'Informe o nome completo do titular.');
          return;
        }
        if (state.customerPhone.replaceAll(RegExp(r'\D'), '').length < 10) {
          state = state.copyWith(errorMessage: 'Informe um telefone/WhatsApp válido com DDD.');
          return;
        }
        state = state.copyWith(step: SalesOnboardingStep.confirmation);
        break;

      case SalesOnboardingStep.confirmation:
        break;
    }
  }

  /// Retorna ao passo anterior.
  void previousStep() {
    state = state.copyWith(clearError: true);
    switch (state.step) {
      case SalesOnboardingStep.feasibility:
        break;
      case SalesOnboardingStep.planSelection:
        state = state.copyWith(step: SalesOnboardingStep.feasibility);
        break;
      case SalesOnboardingStep.customerData:
        state = state.copyWith(step: SalesOnboardingStep.planSelection);
        break;
      case SalesOnboardingStep.confirmation:
        if (state.saleResult == null) {
          state = state.copyWith(step: SalesOnboardingStep.customerData);
        }
        break;
    }
  }

  /// Submete a venda ao backend Spring Boot.
  Future<void> submitSale({String? sellerName}) async {
    if (state.selectedPlan == null) return;

    state = state.copyWith(isSubmitting: true, clearError: true);

    final fullAddress = '${state.street.trim()}, ${state.number.trim()}'
        '${state.complement.trim().isNotEmpty ? " - ${state.complement.trim()}" : ""}'
        ' - ${state.neighborhood.trim()}';

    final payload = CreateSalePayload(
      planId: state.selectedPlan!.id,
      customerName: state.customerName.trim(),
      customerCpf: state.customerCpf.trim(),
      customerEmail: state.customerEmail.trim().isNotEmpty ? state.customerEmail.trim() : null,
      customerPhone: state.customerPhone.replaceAll(RegExp(r'\D'), ''),
      installationAddress: fullAddress,
      city: state.city.trim(),
      state: state.state.trim().toUpperCase(),
      zipCode: state.cep.replaceAll(RegExp(r'\D'), ''),
      preferredDueDate: state.preferredDueDate,
      notificationChannel: state.notificationChannel,
      sellerName: sellerName,
    );

    try {
      final res = await _repository.submitSale(payload);
      state = state.copyWith(
        isSubmitting: false,
        saleResult: res,
      );
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Falha ao processar venda: $e',
      );
    }
  }

  /// Reinicia o wizard para nova venda.
  void reset() {
    state = const SalesOnboardingState();
    loadActivePlans();
  }
}

/// Provider do gerenciador de Venda Expressa.
final salesOnboardingProvider =
    StateNotifierProvider<SalesOnboardingNotifier, SalesOnboardingState>((ref) {
  final repo = ref.watch(salesRepositoryProvider);
  return SalesOnboardingNotifier(repo);
});
