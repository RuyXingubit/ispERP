import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/sales/data/sales_models.dart';
import 'package:isperp_app/features/sales/data/sales_onboarding_notifier.dart';
import 'package:isperp_app/features/sales/data/sales_repository.dart';

class FakeSalesRepository implements SalesRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);

  @override
  Future<List<CommercialPlan>> getActivePlans() async {
    return [
      const CommercialPlan(
        id: '01912345-0000-7000-8000-000000000001',
        name: 'Fibra 500 Mega',
        downloadSpeed: 500,
        uploadSpeed: 250,
        price: 99.90,
        description: 'Plano com Wi-Fi 6 incluso',
      ),
      const CommercialPlan(
        id: '01912345-0000-7000-8000-000000000002',
        name: 'Fibra 800 Mega',
        downloadSpeed: 800,
        uploadSpeed: 400,
        price: 139.90,
      ),
    ];
  }

  @override
  Future<CepLookupModel?> lookupCep(String rawCep) async {
    if (rawCep.contains('68371000') || rawCep.contains('68371-000')) {
      return const CepLookupModel(
        cep: '68371-000',
        street: 'Avenida Djalma Dutra',
        neighborhood: 'Centro',
        city: 'Altamira',
        state: 'PA',
        latitude: -3.2033,
        longitude: -52.2064,
      );
    }
    return null;
  }

  @override
  Future<List<CepLookupModel>> searchAddress(String query) async {
    if (query.toLowerCase().contains('djalma')) {
      return [
        const CepLookupModel(
          cep: '68371-000',
          street: 'Avenida Djalma Dutra',
          neighborhood: 'Centro',
          city: 'Altamira',
          state: 'PA',
          latitude: -3.2033,
          longitude: -52.2064,
        ),
        const CepLookupModel(
          cep: '53110-471',
          street: 'Travessa Primeira Djalma Dutra',
          neighborhood: 'Salgadinho',
          city: 'Olinda',
          state: 'PE',
          latitude: -8.0269,
          longitude: -34.8682,
        ),
      ];
    }
    return [];
  }

  @override
  Future<CepLookupModel?> reverseGeocode(double lat, double lng) async {
    if ((lat - (-3.2107)).abs() < 0.1 && (lng - (-52.2371)).abs() < 0.1) {
      return const CepLookupModel(
        cep: '68371-000',
        street: 'Passagem Elza',
        neighborhood: 'Bela Vista',
        city: 'Altamira',
        state: 'PA',
        latitude: -3.210714,
        longitude: -52.237143,
      );
    }
    return null;
  }

  @override
  Future<FtthFeasibilityModel?> checkFeasibility(
    double latitude,
    double longitude, {
    double maxDistanceMeters = 200.0,
  }) async {
    return const FtthFeasibilityModel(
      viable: true,
      viableCtosCount: 2,
      nearbyCtos: [
        FeasibleCtoItem(
          ctoName: 'CTO-ALT-01',
          distanceMeters: 45.2,
          freePorts: 4,
          hasCapacity: true,
        ),
      ],
    );
  }

  @override
  Future<SaleResult?> submitSale(CreateSalePayload payload) async {
    return SaleResult(
      id: '01912345-9999-7000-8000-000000000999',
      status: 'SUBMITTED',
      customerName: payload.customerName,
      customerCpf: payload.customerCpf,
    );
  }
}

void main() {
  group('Sales Models & CpfUtils Tests', () {
    test('CpfUtils deve validar CPFs matematicamente e rejeitar dígitos falsos', () {
      expect(CpfUtils.isValid(''), isFalse);
      expect(CpfUtils.isValid('123'), isFalse);
      expect(CpfUtils.isValid('11111111111'), isFalse);
      expect(CpfUtils.isValid('00000000000'), isFalse);
      expect(CpfUtils.isValid('12345678900'), isFalse);
      expect(CpfUtils.isValid('52998224725'), isTrue);
      expect(CpfUtils.isValid('529.982.247-25'), isTrue);
    });

    test('CpfUtils deve formatar CPF e CEP com pontuação correta', () {
      expect(CpfUtils.format('52998224725'), equals('529.982.247-25'));
      expect(CpfUtils.formatCep('68371000'), equals('68371-000'));
      expect(CpfUtils.clean('529.982.247-25'), equals('52998224725'));
    });

    test('CepLookupModel deve desserializar JSON do GeoCep com precisão', () {
      final json = {
        'cep': '68371-000',
        'logradouro': 'Travessa 10',
        'bairro': 'Sudam I',
        'localidade': 'Altamira',
        'uf': 'PA',
        'latitude': -3.205,
        'longitude': -52.208,
      };

      final model = CepLookupModel.fromJson(json);
      expect(model.cep, equals('68371-000'));
      expect(model.street, equals('Travessa 10'));
      expect(model.city, equals('Altamira'));
      expect(model.state, equals('PA'));
      expect(model.latitude, equals(-3.205));
    });

    test('CepLookupModel deve suportar coordenadas aninhadas e logradouro alternativo', () {
      final json = {
        'cep': '68371-000',
        'endereco': 'Passagem Elza, 3295',
        'bairro': 'Bela Vista',
        'cidade': 'Altamira',
        'uf': 'PA',
        'coordenadas': {
          'latitude': -3.210714,
          'longitude': -52.237143,
        },
      };

      final model = CepLookupModel.fromJson(json);
      expect(model.street, equals('Passagem Elza, 3295'));
      expect(model.city, equals('Altamira'));
      expect(model.latitude, equals(-3.210714));
      expect(model.longitude, equals(-52.237143));
    });

    test('FtthFeasibilityModel deve desserializar viabilidade e caixas CTO', () {
      final json = {
        'viable': true,
        'viableCtosCount': 1,
        'nearbyCtos': [
          {
            'cto': {'name': 'CTO-CENTRO-05'},
            'distanceMeters': 62.5,
            'freePorts': 3,
            'hasCapacity': true,
          }
        ],
      };

      final model = FtthFeasibilityModel.fromJson(json);
      expect(model.viable, isTrue);
      expect(model.nearbyCtos.length, equals(1));
      expect(model.nearbyCtos.first.ctoName, equals('CTO-CENTRO-05'));
      expect(model.nearbyCtos.first.freePorts, equals(3));
    });
  });

  group('SalesOnboardingNotifier State Machine & Smart Search Tests', () {
    late FakeSalesRepository fakeRepo;
    late SalesOnboardingNotifier notifier;

    setUp(() {
      fakeRepo = FakeSalesRepository();
      notifier = SalesOnboardingNotifier(fakeRepo);
    });

    test('Deve iniciar no passo feasibility com planos carregados', () async {
      await Future.delayed(const Duration(milliseconds: 10));
      expect(notifier.state.step, equals(SalesOnboardingStep.feasibility));
      expect(notifier.state.plans.length, equals(2));
      expect(notifier.state.selectedPlan?.name, equals('Fibra 500 Mega'));
      expect(notifier.state.preferredDueDate, equals(10));
    });

    test('Busca inteligente GeoCEP por CEP numérico', () async {
      await notifier.performSmartSearch('68371-000');
      expect(notifier.state.street, equals('Avenida Djalma Dutra'));
      expect(notifier.state.city, equals('Altamira'));
      expect(notifier.state.state, equals('PA'));
      expect(notifier.state.feasibility, isNotNull);
      expect(notifier.state.feasibility!.viable, isTrue);
    });

    test('Busca inteligente GeoCEP por Coordenadas GPS (Geocodificação Reversa)', () async {
      await notifier.performSmartSearch('-3.2107, -52.2371');
      expect(notifier.state.street, equals('Passagem Elza'));
      expect(notifier.state.neighborhood, equals('Bela Vista'));
      expect(notifier.state.city, equals('Altamira'));
      expect(notifier.state.feasibility, isNotNull);
      expect(notifier.state.feasibility!.viable, isTrue);
    });

    test('Busca inteligente GeoCEP por Nome da Rua com sugestões e seleção', () async {
      await notifier.performSmartSearch('Djalma Dutra');
      expect(notifier.state.searchSuggestions.length, equals(2));

      // Seleciona a primeira sugestão (Altamira)
      await notifier.selectSuggestion(notifier.state.searchSuggestions.first);
      expect(notifier.state.searchSuggestions, isEmpty);
      expect(notifier.state.street, equals('Avenida Djalma Dutra'));
      expect(notifier.state.city, equals('Altamira'));
      expect(notifier.state.feasibility, isNotNull);
    });

    test('Deve impedir avanço para próximo passo se endereço estiver incompleto', () {
      notifier.setStreet('');
      notifier.setNumber('');
      notifier.nextStep();
      expect(notifier.state.step, equals(SalesOnboardingStep.feasibility));
      expect(notifier.state.errorMessage, isNotNull);
    });

    test('Deve avançar esteira completa de venda até submissão com sucesso', () async {
      // 1. Passo Feasibility
      notifier.setStreet('Rua Sete');
      notifier.setNumber('123');
      notifier.setNeighborhood('Bela Vista');
      notifier.setCity('Altamira');
      notifier.setStateUf('PA');
      notifier.nextStep();
      expect(notifier.state.step, equals(SalesOnboardingStep.planSelection));

      // 2. Passo PlanSelection
      await Future.delayed(const Duration(milliseconds: 10));
      notifier.setPreferredDueDate(15);
      notifier.nextStep();
      expect(notifier.state.step, equals(SalesOnboardingStep.customerData));

      // 3. Passo CustomerData com validação
      notifier.setCustomerCpf('52998224725');
      notifier.setCustomerName('Maria da Silva Santos');
      notifier.setCustomerPhone('93991234567');
      notifier.setCustomerEmail('maria@teste.com');
      notifier.setNotificationChannel('WHATSAPP');
      notifier.nextStep();
      expect(notifier.state.step, equals(SalesOnboardingStep.confirmation));

      // 4. Submissão da Venda
      await notifier.submitSale();
      expect(notifier.state.isSubmitting, isFalse);
      expect(notifier.state.saleResult, isNotNull);
      expect(notifier.state.saleResult!.status, equals('SUBMITTED'));
      expect(notifier.state.saleResult!.customerName, equals('Maria da Silva Santos'));

      // 5. Reset
      notifier.reset();
      expect(notifier.state.step, equals(SalesOnboardingStep.feasibility));
      expect(notifier.state.customerName, isEmpty);
    });
  });
}
