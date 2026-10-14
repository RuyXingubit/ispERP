import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:isperp_app/core/network/api_client.dart';
import 'package:isperp_app/core/storage/storage_service.dart';
import 'package:isperp_app/core/theme/app_theme.dart';
import 'package:isperp_app/features/inventory/data/inventory_models.dart';
import 'package:isperp_app/features/sales/data/electronic_signature_models.dart';
import 'package:isperp_app/features/sales/data/electronic_signature_repository.dart';
import 'package:isperp_app/features/sales/presentation/contract_forensic_signature_modal.dart';
import 'package:isperp_app/features/technician/data/technician_execution_models.dart';
import 'package:isperp_app/features/technician/data/technician_execution_repository.dart';
import 'package:isperp_app/features/technician/presentation/technician_dashboard_screen.dart';

class FakeTechnicianExecutionRepository extends TechnicianExecutionRepository {
  FakeTechnicianExecutionRepository(super.apiClient);

  bool provisionCalled = false;
  bool completeCalled = false;
  bool failRemovalCalled = false;

  @override
  Future<List<Map<String, dynamic>>> listWorkOrders() async {
    return [
      {
        'id': '01950000-0000-7000-8000-000000000001',
        'contractId': '01950000-0000-7000-8000-000000000010',
        'customerId': '01950000-0000-7000-8000-000000000020',
        'customerName': 'Maria de Fátima Oliveira',
        'installationAddress': 'Av. Brasil, 500 - Centro',
        'type': 'INSTALACAO',
        'status': 'EM_ANDAMENTO',
        'scheduledPeriod': 'MANHA',
      },
      {
        'id': '01950000-0000-7000-8000-000000000002',
        'contractId': '01950000-0000-7000-8000-000000000011',
        'customerId': '01950000-0000-7000-8000-000000000021',
        'customerName': 'João Inadimplente',
        'installationAddress': 'Rua 7 de Setembro, 120',
        'type': 'RETIRADA',
        'status': 'EM_ANDAMENTO',
        'scheduledPeriod': 'TARDE',
      },
    ];
  }

  @override
  Future<List<OltUnprovisionedOnuModel>> listUnprovisionedOnus(
    String workOrderId,
  ) async {
    return const [
      OltUnprovisionedOnuModel(
        networkDeviceId: '01950000-0000-7000-8000-000000000099',
        oltName: 'OLT-HUAWEI-CENTRAL',
        slotNumber: 1,
        portNumber: 4,
        ponName: 'PON 1/4',
        onuSerial: 'ZTEGC89A12B3',
        onuMac: '48:57:02:11:22:33',
        rxPowerDbm: -19.45,
      ),
    ];
  }

  @override
  Future<ProvisionOnuResult> provisionOnu({
    required String workOrderId,
    required String onuSerial,
    int vlanId = 100,
    String? pppoeUsername,
    String? pppoePassword,
  }) async {
    provisionCalled = true;
    return ProvisionOnuResult(
      success: true,
      message: 'ONU $onuSerial autorizada na VLAN $vlanId com sucesso.',
    );
  }

  @override
  Future<RadiusSessionStatusModel> getRadiusStatus(String workOrderId) async {
    return RadiusSessionStatusModel(
      workOrderId: workOrderId,
      authenticated: true,
      username: 'maria.oliveira@provedor.com.br',
      framedIpAddress: '100.64.12.45',
      macAddress: '48:57:02:11:22:33',
      nasIpAddress: '10.0.0.1',
      uptimeSeconds: 185,
    );
  }

  @override
  Future<Map<String, dynamic>> completeInstallation(
    String workOrderId,
    TechnicianExecutionCompletePayload payload,
  ) async {
    completeCalled = true;
    return {'id': workOrderId, 'status': 'COMPLETED'};
  }

  @override
  Future<List<WarehouseModel>> listWarehouses() async {
    return const [
      WarehouseModel(
        id: '01950000-0000-7000-8000-000000000050',
        code: 'ALM-CENTRAL',
        name: 'Almoxarifado Central',
        city: 'Altamira',
        state: 'PA',
      ),
    ];
  }

  @override
  Future<LegalCollectionResultModel> failRemoval(
    String workOrderId,
    FailRemovalPayload payload,
  ) async {
    failRemovalCalled = true;
    return LegalCollectionResultModel(
      id: '01950000-0000-7000-8000-000000000077',
      customerId: '01950000-0000-7000-8000-000000000021',
      contractId: '01950000-0000-7000-8000-000000000011',
      workOrderId: workOrderId,
      totalDebtInvoices: 219.80,
      equipmentIndemnityAmount: 420.00,
      totalClaimAmount: 639.80,
      status: 'PENDING_BUREAU_SUBMISSION',
      evidenceNotes: payload.unsuccessReason,
    );
  }
}

class FakeElectronicSignatureRepository extends ElectronicSignatureRepository {
  FakeElectronicSignatureRepository(super.apiClient);

  final List<SignatureSessionModel> _sessions = [];
  bool fallbackSelected = false;

  @override
  Future<List<SignatureSessionModel>> listContractSignatures(
    String contractId,
  ) async {
    return List.unmodifiable(_sessions);
  }

  @override
  Future<SignatureSessionModel> createSignatureSession({
    required String contractId,
    String? templateId,
    double symbolicAmount = 1.00,
  }) async {
    final created = SignatureSessionModel(
      id: '01950000-0000-7000-8000-000000000801',
      contractId: contractId,
      token: 'sig_token_forense_001',
      signatureUrl: 'https://pay.xingubit.com.br/sign/sig_token_forense_001',
      status: SignatureStatusEnum.pending,
      symbolicAmount: symbolicAmount,
      pixTxid: 'SIG20261014001',
      pixCopyPaste:
          '00020101021226800014br.gov.bcb.pix2558pix.xingubit.com.br/qr/SIG20261014001',
      documentSha256Hash:
          'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    );
    _sessions.insert(0, created);
    return created;
  }

  @override
  Future<SignatureSessionModel> getSignatureStatus(String token) async {
    return SignatureSessionModel(
      id: '01950000-0000-7000-8000-000000000801',
      contractId: '01950000-0000-7000-8000-000000000010',
      token: token,
      signatureUrl: 'https://pay.xingubit.com.br/sign/$token',
      status: SignatureStatusEnum.signed,
      symbolicAmount: 1.00,
      pixTxid: 'SIG20261014001',
      pixEndToEndId: 'E18236120202610141800abcdef',
      payerName: 'Maria de Fátima Oliveira',
      payerCpfCnpj: '***.456.789-**',
      payerBankName: 'Banco do Brasil S.A.',
      documentSha256Hash:
          '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
      forensicCertificatePdfUrl: '/api/public/signatures/$token/pdf',
    );
  }

  @override
  Future<SignatureSessionModel> selectFallbackMethod({
    required String token,
    required FallbackMethodEnum fallbackMethod,
    String? justification,
  }) async {
    fallbackSelected = true;
    return SignatureSessionModel(
      id: '01950000-0000-7000-8000-000000000801',
      contractId: '01950000-0000-7000-8000-000000000010',
      token: token,
      signatureUrl: 'https://pay.xingubit.com.br/sign/$token',
      status: SignatureStatusEnum.pending,
      symbolicAmount: 1.00,
      fallbackMethod: fallbackMethod,
      rejectionReason:
          'Alternativa oficial de assinatura selecionada (${fallbackMethod.apiValue}): ${justification ?? ""}',
    );
  }
}

void main() {
  late SharedPreferences prefs;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    prefs = await SharedPreferences.getInstance();
  });

  group('Onda F - Modelos de Execução de Campo & Assinatura Forense', () {
    test(
      'OltUnprovisionedOnuModel classifica qualidade de sinal óptico corretamente',
      () {
        final optimalOnu = OltUnprovisionedOnuModel.fromJson(const {
          'networkDeviceId': 'dev-1',
          'oltName': 'OLT-01',
          'slotNumber': 1,
          'portNumber': 2,
          'onuSerial': 'ZTEG11223344',
          'rxPowerDbm': -19.2,
        });
        expect(optimalOnu.isSignalOptimal, isTrue);
        expect(optimalOnu.isSignalCritical, isFalse);
        expect(optimalOnu.displayPon, 'PON 1/2');

        final criticalOnu = OltUnprovisionedOnuModel.fromJson(const {
          'networkDeviceId': 'dev-1',
          'oltName': 'OLT-01',
          'slotNumber': 1,
          'portNumber': 3,
          'ponName': 'GPON 0/1/3',
          'onuSerial': 'HWTC99887766',
          'rxPowerDbm': -28.4,
        });
        expect(criticalOnu.isSignalOptimal, isFalse);
        expect(criticalOnu.isSignalCritical, isTrue);
        expect(criticalOnu.displayPon, 'GPON 0/1/3');
      },
    );

    test('RadiusSessionStatusModel formata uptime em minutos e horas', () {
      final shortSession = RadiusSessionStatusModel.fromJson(const {
        'workOrderId': 'wo-1',
        'authenticated': true,
        'uptimeSeconds': 125,
      });
      expect(shortSession.formattedUptime, '2m 5s');

      final longSession = RadiusSessionStatusModel.fromJson(const {
        'workOrderId': 'wo-1',
        'authenticated': true,
        'uptimeSeconds': 7320,
      });
      expect(longSession.formattedUptime, '2h 2m');
    });

    test(
      'SignatureSessionModel interpreta status e métodos de fallback MP 2.200-2',
      () {
        final session = SignatureSessionModel.fromJson(const {
          'id': 'sig-1',
          'contractId': 'ctr-1',
          'token': 'tok-123',
          'signatureUrl': 'https://pay.xingubit.com.br/sign/tok-123',
          'status': 'REJECTED_DIVERGENT_DOCUMENT',
          'symbolicAmount': 1.0,
          'fallbackMethod': 'GOV_BR',
        });

        expect(session.isRejected, isTrue);
        expect(session.isSigned, isFalse);
        expect(session.fallbackMethod, FallbackMethodEnum.govBr);
      },
    );
  });

  group('Onda F - Widgets de Execução Técnica de Campo e Assinatura Forense', () {
    testWidgets(
      'Executa fluxo completo OLT Auto-Find -> Provisionar -> RADIUS -> Laudo e Logística Reversa',
      (tester) async {
        tester.view.physicalSize = const Size(1280, 800);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);

        final fakeTechRepo = FakeTechnicianExecutionRepository(
          ApiClient(StorageService(prefs)),
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              technicianExecutionRepositoryProvider.overrideWithValue(
                fakeTechRepo,
              ),
            ],
            child: MaterialApp(
              theme: AppTheme.darkTheme,
              home: const TechnicianDashboardScreen(),
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('Maria de Fátima Oliveira'), findsOneWidget);
        expect(find.text('João Inadimplente'), findsOneWidget);

        // Abre modal de execução para O.S. de INSTALACAO
        await tester.tap(
          find.widgetWithText(
            ElevatedButton,
            'Executar O.S.',
          ),
        );
        await tester.pumpAndSettle();

        // Etapa 1: Auto-Find exibe ONU ZTEGC89A12B3
        expect(find.text('SN: ZTEGC89A12B3'), findsOneWidget);
        await tester.tap(find.widgetWithText(ElevatedButton, 'Usar ONU'));
        await tester.pumpAndSettle();

        // Etapa 2: Provisionar 1-Clique
        expect(
          find.text('Provisionamento em 1-Clique na OLT'),
          findsOneWidget,
        );
        await tester.tap(
          find.widgetWithText(ElevatedButton, 'Provisionar ONU na OLT'),
        );
        await tester.pumpAndSettle();
        expect(fakeTechRepo.provisionCalled, isTrue);
        expect(
          find.textContaining('ONU ZTEGC89A12B3 autorizada'),
          findsOneWidget,
        );

        // Etapa 3: Validação RADIUS
        await tester.tap(
          find.widgetWithText(OutlinedButton, 'Ir para Validação RADIUS'),
        );
        await tester.pumpAndSettle();
        expect(
          find.text('AUTENTICADO ONLINE NO FREERADIUS'),
          findsOneWidget,
        );
        expect(find.text('100.64.12.45'), findsOneWidget);

        // Etapa 4: Laudo & Assinatura
        await tester.tap(
          find.widgetWithText(
            OutlinedButton,
            'Avançar para Laudo & Assinatura',
          ),
        );
        await tester.pumpAndSettle();

        await tester.enterText(
          find.widgetWithText(TextField, 'Nome Legível do Recebedor *'),
          'Maria de Fátima Oliveira',
        );
        await tester.tap(
          find.widgetWithText(
            ElevatedButton,
            'Concluir O.S. e Ativar Contrato',
          ),
        );
        await tester.pumpAndSettle();
        expect(fakeTechRepo.completeCalled, isTrue);

        // Agora abre O.S. de RETIRADA e testa Retirada Infrutífera / SPC
        await tester.tap(
          find.widgetWithText(ElevatedButton, 'Executar Logística Reversa'),
        );
        await tester.pumpAndSettle();

        await tester.tap(
          find.widgetWithText(
            ChoiceChip,
            'Retirada Infrutífera (Acionar SPC / Jurídico)',
          ),
        );
        await tester.pumpAndSettle();

        await tester.tap(
          find.widgetWithText(
            ElevatedButton,
            'Registrar Retirada Infrutífera & Gerar Cobrança SPC',
          ),
        );
        await tester.pumpAndSettle();

        expect(fakeTechRepo.failRemovalCalled, isTrue);
        expect(
          find.textContaining('Cobrança Jurídica Acionada'),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'Gera sessão de Assinatura Eletrônica Forense via PIX Simbólico e sincroniza status',
      (tester) async {
        tester.view.physicalSize = const Size(1280, 800);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);

        final fakeSigRepo = FakeElectronicSignatureRepository(
          ApiClient(StorageService(prefs)),
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              electronicSignatureRepositoryProvider.overrideWithValue(
                fakeSigRepo,
              ),
            ],
            child: MaterialApp(
              theme: AppTheme.darkTheme,
              home: const Scaffold(
                body: ContractForensicSignatureModal(
                  contractId: '01950000-0000-7000-8000-000000000010',
                  contractNumber: 'CTR-2026-00891',
                  customerName: 'Maria de Fátima Oliveira',
                ),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(
          find.text('Nenhuma sessão de assinatura ativa para este contrato'),
          findsOneWidget,
        );

        // Gera sessão de assinatura via PIX Simbólico (R$ 1,00)
        await tester.tap(
          find.widgetWithText(
            ElevatedButton,
            'Gerar Assinatura Forense via PIX Simbólico (R\$ 1,00)',
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('Aguardando PIX Simbólico'), findsOneWidget);
        expect(
          find.textContaining('SHA-256 do Documento: e3b0c44298fc1c149afbf4c8'),
          findsOneWidget,
        );

        // Registra Fallback MP 2.200-2
        final fallbackBtn = find.widgetWithText(
          OutlinedButton,
          'Registrar Fallback MP 2.200-2',
        );
        await tester.ensureVisible(fallbackBtn);
        await tester.pumpAndSettle();
        await tester.tap(fallbackBtn);
        await tester.pumpAndSettle();
        expect(fakeSigRepo.fallbackSelected, isTrue);

        // Sincroniza status para SIGNED
        final syncBtn = find.widgetWithText(
          OutlinedButton,
          'Sincronizar Status',
        );
        await tester.ensureVisible(syncBtn);
        await tester.pumpAndSettle();
        await tester.tap(syncBtn);
        await tester.pumpAndSettle();

        expect(find.text('Assinado com Validade Forense'), findsOneWidget);
        expect(
          find.textContaining('EndToEndId: E18236120202610141800abcdef'),
          findsOneWidget,
        );
      },
    );
  });
}
