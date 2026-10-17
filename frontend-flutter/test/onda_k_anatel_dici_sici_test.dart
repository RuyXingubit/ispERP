import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/financial/data/fiscal_repository.dart';
import 'package:isperp_app/features/financial/presentation/fiscal_and_nfcom_tab.dart';

void main() {
  group('Onda K — Exportador Regulatório Anatel DICI / SICI', () {
    test('AnatelDiciReportModel.fromJson mapeia grupos IBGE, PF/PJ/Governo e CSV com precisão', () {
      final json = {
        'yearMonth': '2026-10',
        'companyCnpj': '12.345.678/0001-99',
        'companyName': 'XinguBit Fibra Telecom Ltda',
        'totalAccessesInService': 4,
        'activeAccessesCount': 3,
        'suspendedAccessesCount': 1,
        'pfAccessesCount': 2,
        'pjAccessesCount': 1,
        'governmentAccessesCount': 1,
        'municipalitiesCount': 2,
        'csvPreviewContent':
            'CNPJ;ANO;MES;CODIGO_IBGE;MUNICIPIO;UF;TIPO_CLIENTE;TECNOLOGIA;MEIO_TRANSMISSAO;VELOCIDADE_DOWN_MBPS;VELOCIDADE_UP_MBPS;ACESSOS_ATIVOS;ACESSOS_SUSPENSOS;ACESSOS_TOTAL\n'
                '12345678000199;2026;10;1500602;Altamira;PA;PF;FTTH;FIBRA;400;200;1;1;2\n',
        'rows': [
          {
            'ibgeCode': '1500602',
            'municipalityName': 'Altamira',
            'uf': 'PA',
            'customerType': 'PF',
            'planName': 'Fibra 400 Mega',
            'downloadSpeedMbps': 400,
            'uploadSpeedMbps': 200,
            'technology': 'FTTH',
            'transmissionMedium': 'FIBRA',
            'activeCount': 1,
            'suspendedCount': 1,
            'totalInServiceCount': 2,
            'ibgeResolutionSource': 'GEOCEP_API',
          },
          {
            'ibgeCode': '1508357',
            'municipalityName': 'Vitória do Xingu',
            'uf': 'PA',
            'customerType': 'PJ',
            'planName': 'Fibra Empresarial 800 Mega',
            'downloadSpeedMbps': 800,
            'uploadSpeedMbps': 400,
            'technology': 'FTTH',
            'transmissionMedium': 'FIBRA',
            'activeCount': 1,
            'suspendedCount': 0,
            'totalInServiceCount': 1,
            'ibgeResolutionSource': 'LOCAL_FALLBACK',
          },
        ],
      };

      final model = AnatelDiciReportModel.fromJson(json);
      expect(model.yearMonth, '2026-10');
      expect(model.totalAccessesInService, 4);
      expect(model.activeAccessesCount, 3);
      expect(model.suspendedAccessesCount, 1);
      expect(model.pfAccessesCount, 2);
      expect(model.pjAccessesCount, 1);
      expect(model.governmentAccessesCount, 1);
      expect(model.municipalitiesCount, 2);
      expect(model.rows.length, 2);
      expect(model.rows[0].ibgeCode, '1500602');
      expect(model.rows[0].ibgeResolutionSource, 'GEOCEP_API');
      expect(model.rows[1].ibgeCode, '1508357');
      expect(model.rows[1].ibgeResolutionSource, 'LOCAL_FALLBACK');
      expect(model.csvPreviewContent, contains('1500602;Altamira;PA;PF;FTTH'));
    });

    testWidgets(
      'FiscalAndNfcomTab renderiza painel Anatel DICI / SICI com tabela IBGE e modal CSV sem overflow',
      (tester) async {
        tester.view.physicalSize = const Size(1440, 1000);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);

        const mockReport = AnatelDiciReportModel(
          yearMonth: '2026-10',
          companyCnpj: '12.345.678/0001-99',
          companyName: 'XinguBit Fibra Telecom Ltda',
          totalAccessesInService: 3,
          activeAccessesCount: 2,
          suspendedAccessesCount: 1,
          pfAccessesCount: 2,
          pjAccessesCount: 1,
          governmentAccessesCount: 0,
          municipalitiesCount: 2,
          csvPreviewContent:
              'CNPJ;ANO;MES;CODIGO_IBGE;MUNICIPIO;UF;TIPO_CLIENTE;TECNOLOGIA;MEIO_TRANSMISSAO;VELOCIDADE_DOWN_MBPS;VELOCIDADE_UP_MBPS;ACESSOS_ATIVOS;ACESSOS_SUSPENSOS;ACESSOS_TOTAL\n'
              '12345678000199;2026;10;1500602;Altamira;PA;PF;FTTH;FIBRA;400;200;1;1;2\n',
          rows: [
            AnatelDiciAccessGroupModel(
              ibgeCode: '1500602',
              municipalityName: 'Altamira',
              uf: 'PA',
              customerType: 'PF',
              planName: 'Fibra 400 Mega',
              downloadSpeedMbps: 400,
              uploadSpeedMbps: 200,
              technology: 'FTTH',
              transmissionMedium: 'FIBRA',
              activeCount: 1,
              suspendedCount: 1,
              totalInServiceCount: 2,
              ibgeResolutionSource: 'GEOCEP_API',
            ),
            AnatelDiciAccessGroupModel(
              ibgeCode: '1508357',
              municipalityName: 'Vitória do Xingu',
              uf: 'PA',
              customerType: 'PJ',
              planName: 'Fibra Empresarial 800 Mega',
              downloadSpeedMbps: 800,
              uploadSpeedMbps: 400,
              technology: 'FTTH',
              transmissionMedium: 'FIBRA',
              activeCount: 1,
              suspendedCount: 0,
              totalInServiceCount: 1,
              ibgeResolutionSource: 'LOCAL_FALLBACK',
            ),
          ],
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              activeFiscalCompanyProvider.overrideWith((ref) async => null),
              activeFiscalGatewayConfigProvider.overrideWith(
                (ref) async => const FiscalGatewayConfigModel(
                  companyId: 'comp-1',
                  gatewayType: 'XINGUBIT_PAY',
                  environment: 'HOMOLOGACAO',
                  baseUrl: 'https://pay.xingubit.com.br',
                  isActive: true,
                ),
              ),
              nfcomRecordsPageProvider.overrideWith(
                (ref, page) async => const NfcomRecordsPageModel(
                  content: [],
                  totalElements: 0,
                  totalPages: 1,
                  size: 15,
                  number: 0,
                ),
              ),
              fiscalRegimeTransitionsProvider.overrideWith((ref) async => []),
              anatelDiciReportProvider.overrideWith((ref, ym) async => mockReport),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: FiscalAndNfcomTab(),
              ),
            ),
          ),
        );

        await tester.pumpAndSettle();

        expect(
          find.text(
            'Exportador Regulatório Anatel DICI / SICI — Coleta Mensal de Acessos SCM',
          ),
          findsOneWidget,
        );
        expect(find.text('1500602'), findsOneWidget);
        expect(find.text('Altamira/PA'), findsOneWidget);
        expect(find.text('1508357'), findsOneWidget);
        expect(find.text('Vitória do Xingu/PA'), findsOneWidget);
        expect(find.text('GeoCEP API'), findsOneWidget);
        expect(find.text('Fallback Local'), findsOneWidget);

        // Abre modal de exportação CSV
        await tester.tap(find.text('Exportar CSV Anatel DICI (;)'));
        await tester.pumpAndSettle();

        expect(
          find.textContaining('Arquivo Oficial Anatel DICI / SICI'),
          findsOneWidget,
        );
        expect(
          find.textContaining('12345678000199;2026;10;1500602;Altamira;PA;PF'),
          findsOneWidget,
        );
      },
    );
  });
}
