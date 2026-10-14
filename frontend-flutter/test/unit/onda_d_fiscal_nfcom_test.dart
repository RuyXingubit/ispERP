import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_model.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_provider.dart';
import 'package:isperp_app/features/financial/data/fiscal_repository.dart';
import 'package:isperp_app/features/financial/presentation/financial_dashboard_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  group('Onda D — Fiscal, NFCom Mod. 62, Convênio 115 & Regime Transitions Models', () {
    test('FiscalCompanyModel, CertificateUploadResultModel & FiscalGatewayConfigModel parse correctly', () {
      final company = FiscalCompanyModel.fromJson({
        'id': '01912e73-b789-7000-8000-000000000001',
        'cnpj': '12.345.678/0001-90',
        'razaoSocial': 'Nexus Fibra Telecomunicações LTDA',
        'nomeFantasia': 'Nexus Fibra',
        'inscricaoEstadual': '15.123.456-7',
        'cnaePrincipal': '6110-8/03',
        'regimeTributario': 'SIMPLES_NACIONAL',
        'aliquotaIcms': 0.0,
        'aliquotaFust': 0.65,
        'aliquotaFunttel': 0.50,
        'aliquotaPis': 0.65,
        'aliquotaCofins': 3.00,
        'logradouro': 'Av. Principal',
        'numero': '100',
        'bairro': 'Centro',
        'cidade': 'Altamira',
        'uf': 'PA',
        'cep': '68370-000',
        'codigoIbge': '1500602',
        'nfcomAmbiente': 'PRODUCAO',
        'nfcomSerie': '1',
        'nfcomProximoNumero': 105,
        'hasCertificate': true,
        'certificateExpiresAt': '2027-09-15T23:59:59Z',
        'accountingName': 'Contabilidade Express',
        'accountingEmails': 'contabil@express.com.br',
        'accountingSendDay': 5,
        'accountingAutoSend': true,
        'fiscalConfirmed': true,
        'isActive': true,
      });

      expect(company.cnpj, '12.345.678/0001-90');
      expect(company.nfcomAmbiente, 'PRODUCAO');
      expect(company.hasCertificate, isTrue);
      expect(company.aliquotaCofins, 3.00);

      final certRes = CertificateUploadResultModel.fromJson({
        'success': true,
        'subjectCnpj': '12.345.678/0001-90',
        'subjectName': 'NEXUS FIBRA TELECOMUNICACOES LTDA:12345678000190',
        'validUntil': '2027-09-15T23:59:59Z',
        'message': 'Certificado A1 validado e instalado com sucesso.',
      });
      expect(certRes.success, isTrue);
      expect(certRes.subjectCnpj, '12.345.678/0001-90');

      final gw = FiscalGatewayConfigModel.fromJson({
        'companyId': '01912e73-b789-7000-8000-000000000001',
        'gatewayType': 'XINGUBIT_PAY',
        'environment': 'PRODUCAO',
        'baseUrl': 'https://pay.xingubit.com.br',
        'isActive': true,
      });
      expect(gw.gatewayType, 'XINGUBIT_PAY');
      expect(gw.environment, 'PRODUCAO');
    });

    test('NfcomRecordsPageModel, NfcomCancelResultModel & FiscalRegimeTransitionModel parse correctly', () {
      final page = NfcomRecordsPageModel.fromJson({
        'content': [
          {
            'id': '01912e73-b789-7000-8000-000000000010',
            'companyId': '01912e73-b789-7000-8000-000000000001',
            'customerId': '01912e73-b789-7000-8000-000000000020',
            'chaveAcesso': '15260912345678000190620010000001011234567890',
            'numero': 101,
            'serie': '1',
            'modelo': '62',
            'tipoEmissao': 'NORMAL',
            'ambiente': 'PRODUCAO',
            'status': 'AUTORIZADA',
            'protocoloAutorizacao': '115260000012345',
            'valorTotal': 119.90,
            'valorIcms': 0.0,
            'valorFust': 0.78,
            'valorFunttel': 0.60,
            'xmlAutorizado': '<NFCom><infNFCom Id="NFCom152609..."/></NFCom>',
          }
        ],
        'totalElements': 1,
        'totalPages': 1,
        'size': 15,
        'number': 0,
      });

      expect(page.totalElements, 1);
      expect(page.content.first.numero, 101);
      expect(page.content.first.status, 'AUTORIZADA');
      expect(page.content.first.valorTotal, 119.90);

      final cancelRes = NfcomCancelResultModel.fromJson({
        'success': true,
        'chaveAcesso': '15260912345678000190620010000001011234567890',
        'protocoloCancelamento': '115260000099999',
      });
      expect(cancelRes.success, isTrue);
      expect(cancelRes.protocoloCancelamento, '115260000099999');

      final tr = FiscalRegimeTransitionModel.fromJson({
        'id': '01912e73-b789-7000-8000-000000000030',
        'companyId': '01912e73-b789-7000-8000-000000000001',
        'previousRegime': 'SIMPLES_NACIONAL',
        'newRegime': 'LUCRO_PRESUMIDO',
        'effectiveDate': '2027-01-01',
        'aliquotaIcms': 18.0,
        'aliquotaPis': 0.65,
        'aliquotaCofins': 3.00,
        'status': 'SCHEDULED',
        'notes': 'Desenquadramento programado para o próximo exercício',
        'createdAt': '2026-10-14T15:00:00Z',
      });
      expect(tr.previousRegime, 'SIMPLES_NACIONAL');
      expect(tr.newRegime, 'LUCRO_PRESUMIDO');
      expect(tr.status, 'SCHEDULED');
    });
  });

  group('Onda D — FinancialDashboardScreen 6th Tab (Fiscal & NFCom Mod. 62) Widget Test', () {
    testWidgets('Navigates to Fiscal & NFCom Mod. 62 tab and renders company, NFCom records and transitions',
        (tester) async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sharedPreferencesProvider.overrideWithValue(prefs),
            dashboardBiProvider.overrideWith((ref) async {
              return DashboardBiModel.fromJson({});
            }),
            activeFiscalCompanyProvider.overrideWith((ref) async {
              return const FiscalCompanyModel(
                id: 'comp-1',
                cnpj: '12.345.678/0001-90',
                razaoSocial: 'Nexus Fibra Telecomunicações LTDA',
                inscricaoEstadual: '15.123.456-7',
                cnaePrincipal: '6110-8/03',
                regimeTributario: 'SIMPLES_NACIONAL',
                aliquotaIcms: 0.0,
                aliquotaFust: 0.65,
                aliquotaFunttel: 0.50,
                aliquotaPis: 0.65,
                aliquotaCofins: 3.0,
                logradouro: 'Av. Brasil',
                numero: '100',
                bairro: 'Centro',
                cidade: 'Altamira',
                uf: 'PA',
                cep: '68370-000',
                codigoIbge: '1500602',
                nfcomAmbiente: 'PRODUCAO',
                nfcomSerie: '1',
                nfcomProximoNumero: 200,
                hasCertificate: true,
                certificateExpiresAt: '2027-10-01',
                accountingSendDay: 5,
                accountingAutoSend: true,
                fiscalConfirmed: true,
                isActive: true,
              );
            }),
            activeFiscalGatewayConfigProvider.overrideWith((ref) async {
              return const FiscalGatewayConfigModel(
                companyId: 'comp-1',
                gatewayType: 'XINGUBIT_PAY',
                environment: 'PRODUCAO',
                baseUrl: 'https://pay.xingubit.com.br',
                isActive: true,
              );
            }),
            nfcomRecordsPageProvider(0).overrideWith((ref) async {
              return const NfcomRecordsPageModel(
                content: [
                  NfcomRecordModel(
                    id: 'rec-101',
                    companyId: 'comp-1',
                    customerId: 'cust-1',
                    chaveAcesso: '15260912345678000190620010000001011234567890',
                    numero: 101,
                    serie: '1',
                    modelo: '62',
                    tipoEmissao: 'NORMAL',
                    ambiente: 'PRODUCAO',
                    status: 'AUTORIZADA',
                    protocoloAutorizacao: '115260000012345',
                    valorTotal: 99.90,
                    valorIcms: 0.0,
                    valorFust: 0.65,
                    valorFunttel: 0.50,
                  ),
                ],
                totalElements: 1,
                totalPages: 1,
                size: 15,
                number: 0,
              );
            }),
            fiscalRegimeTransitionsProvider.overrideWith((ref) async {
              return const [
                FiscalRegimeTransitionModel(
                  id: 'tr-1',
                  companyId: 'comp-1',
                  previousRegime: 'SIMPLES_NACIONAL',
                  newRegime: 'LUCRO_PRESUMIDO',
                  effectiveDate: '2027-01-01',
                  aliquotaIcms: 18.0,
                  aliquotaPis: 0.65,
                  aliquotaCofins: 3.0,
                  status: 'SCHEDULED',
                  notes: 'Virada anual',
                  createdAt: '2026-10-14T15:00:00Z',
                ),
              ];
            }),
          ],
          child: const MaterialApp(
            home: FinancialDashboardScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      final tab6 = find.text('Fiscal & NFCom Mod. 62');
      await tester.ensureVisible(tab6);
      await tester.tap(tab6);
      await tester.pumpAndSettle();

      expect(
        find.textContaining('Nexus Fibra Telecomunicações LTDA'),
        findsOneWidget,
      );
      expect(
        find.textContaining('NFCom Nº 101'),
        findsOneWidget,
      );
      expect(
        find.textContaining('SIMPLES_NACIONAL  ➔  LUCRO_PRESUMIDO'),
        findsOneWidget,
      );
    });
  });
}
