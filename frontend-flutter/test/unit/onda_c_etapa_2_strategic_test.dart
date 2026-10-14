import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/features/financial/data/cash_custody_repository.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_model.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_provider.dart';
import 'package:isperp_app/features/financial/data/payables_and_accounts_repository.dart';
import 'package:isperp_app/features/financial/data/strategic_finance_repository.dart';
import 'package:isperp_app/features/financial/presentation/financial_dashboard_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  group('Onda C (Etapa 2) — DRE Telecom, Desalavancagem 36M, Payback FTTH & Sentinela', () {
    test(
      'DreReportModel, DeleveragingProjectionModel e InvestmentSimulationResultModel deserializam conforme OpenAPI',
      () {
        final dre = DreReportModel.fromJson({
          'periodStart': '2026-10-01',
          'periodEnd': '2026-10-31',
          'accountingMethod': 'ACCRUAL',
          'grossRevenue': 120000.0,
          'taxDeductions': 14400.0,
          'netRevenue': 105600.0,
          'directCostsInterconnection': 18000.0,
          'contributionMargin': 87600.0,
          'opexPersonnel': 28000.0,
          'opexInfrastructure': 12000.0,
          'opexAdministrative': 6500.0,
          'opexCommercial': 4100.0,
          'totalOpex': 50600.0,
          'ebitda': 37000.0,
          'ebitdaMarginPercentage': 35.03,
          'financialResult': -3000.0,
          'netIncome': 34000.0,
          'netMarginPercentage': 32.19,
        });
        expect(dre.accountingMethod, 'ACCRUAL');
        expect(dre.grossRevenue, 120000.0);
        expect(dre.ebitda, 37000.0);
        expect(dre.netIncome, 34000.0);

        final proj = DeleveragingProjectionModel.fromJson({
          'currentEbitda': 37000.0,
          'currentMrr': 120000.0,
          'monthlyChurnRatePercentage': 1.2,
          'startingCashBalance': 55000.0,
          'worstMonthYearMonth': '2027-02',
          'worstMonthProjectedBalance': 14200.0,
          'breakEvenYearMonth': '2027-08',
          'monthsUntilFreedom': 18,
          'timeline': [
            {
              'yearMonth': '2026-11',
              'projectedMrr': 121500.0,
              'projectedDirectCosts': 18000.0,
              'projectedOpex': 50600.0,
              'projectedDebtInstallments': 22000.0,
              'projectedNetCashFlow': 30900.0,
              'cumulativeCashBalance': 85900.0,
              'isNegativeBalance': false,
            }
          ],
        });
        expect(proj.worstMonthYearMonth, '2027-02');
        expect(proj.monthsUntilFreedom, 18);
        expect(proj.timeline.length, 1);
        expect(proj.timeline.first.yearMonth, '2026-11');

        final sim = InvestmentSimulationResultModel.fromJson({
          'feasible': true,
          'monthlyInstallmentAmount': 4000.0,
          'simulatedWorstMonth': '2027-02',
          'simulatedWorstBalance': 6200.0,
          'balanceImpactAtWorstMonth': -8000.0,
          'originalBreakEvenMonth': '2027-08',
          'simulatedBreakEvenMonth': '2027-09',
          'delayInMonthsForFreedom': 1,
          'riskAnalysisSummary': 'Investimento absorvido com margem de segurança.',
        });
        expect(sim.feasible, isTrue);
        expect(sim.monthlyInstallmentAmount, 4000.0);
        expect(sim.delayInMonthsForFreedom, 1);
      },
    );

    test(
      'NetworkProjectPaybackModel e SentinelAuditAlertModel deserializam conforme OpenAPI',
      () {
        final project = NetworkProjectPaybackModel.fromJson({
          'projectId': '0195f000-0000-7000-8000-000000000901',
          'name': 'Expansão Bairro Esperança - Fase 1',
          'neighborhood': 'Esperança',
          'city': 'Santarém',
          'status': 'ACTIVE',
          'budgetAmount': 65000.0,
          'targetSubscribers': 300,
          'startDate': '2026-06-01',
          'ctoCount': 12,
          'totalPorts': 192,
          'occupiedPorts': 144,
          'penetrationRate': 75.0,
          'monthlyRevenue': 14400.0,
          'accumulatedRevenue': 72000.0,
          'paybackMonthsEstimated': 5,
          'isPaybackAchieved': true,
          'commercialPriority': 'META_ATINGIDA',
        });
        expect(project.name, 'Expansão Bairro Esperança - Fase 1');
        expect(project.ctoCount, 12);
        expect(project.isPaybackAchieved, isTrue);

        final alert = SentinelAuditAlertModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000902',
          'auditType': 'ONU_ORFA_SEM_CONTRATO',
          'severity': 'CRITICAL',
          'title': 'ONU Autorizada na OLT sem Contrato Ativo no ERP',
          'description': 'Serial HWTC889911AA trafegando 420 GB sem faturamento vinculado.',
          'geminiAnalysis': 'Indício de ativação clandestina fora do fluxo de O.S.',
          'recommendedAction': 'Bloquear porta GPON imediatamente e auditar técnico.',
          'resolved': false,
        });
        expect(alert.severity, 'CRITICAL');
        expect(alert.auditType, 'ONU_ORFA_SEM_CONTRATO');
        expect(alert.resolved, isFalse);
      },
    );

    testWidgets(
      'FinancialDashboardScreen renderiza as abas DRE & Desalavancagem 36M e Payback FTTH & Sentinela',
      (tester) async {
        SharedPreferences.setMockInitialValues({});
        final prefs = await SharedPreferences.getInstance();

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              sharedPreferencesProvider.overrideWithValue(prefs),
              dashboardBiProvider.overrideWith(
                (ref) async => DashboardBiModel.fromJson({
                  'mrr': 120000.0,
                  'arpu': 110.0,
                  'activeContracts': 1090,
                  'totalCustomers': 1120,
                  'totalReceivedMonth': 115000.0,
                  'overdueAmount': 3500.0,
                  'defaultRate': 2.9,
                  'pixConversionRate': 82.0,
                  'recentOverdueInvoices': [],
                }),
              ),
              pendingExpenseInstallmentsProvider.overrideWith((ref) async => const []),
              allPayableInvoicesProvider.overrideWith((ref) async => const []),
              chartOfAccountsTreeProvider.overrideWith((ref) async => const []),
              chartOfAccountsFlatProvider.overrideWith((ref) async => const []),
              allCashCustodiesProvider.overrideWith((ref) async => const []),
              pendingBankDepositsProvider.overrideWith((ref) async => const []),
              pendingWorkOrderFeeWaiversProvider.overrideWith((ref) async => const []),
              dreReportProvider.overrideWith(
                (ref, params) async => DreReportModel(
                  periodStart: params.startDate,
                  periodEnd: params.endDate,
                  accountingMethod: params.method,
                  grossRevenue: 120000.0,
                  taxDeductions: 14400.0,
                  netRevenue: 105600.0,
                  directCostsInterconnection: 18000.0,
                  contributionMargin: 87600.0,
                  opexPersonnel: 28000.0,
                  opexInfrastructure: 12000.0,
                  opexAdministrative: 6500.0,
                  opexCommercial: 4100.0,
                  totalOpex: 50600.0,
                  ebitda: 37000.0,
                  ebitdaMarginPercentage: 35.0,
                  financialResult: -3000.0,
                  netIncome: 34000.0,
                  netMarginPercentage: 32.2,
                ),
              ),
              deleveragingProjectionProvider.overrideWith(
                (ref) async => const DeleveragingProjectionModel(
                  currentEbitda: 37000.0,
                  currentMrr: 120000.0,
                  monthlyChurnRatePercentage: 1.2,
                  startingCashBalance: 55000.0,
                  worstMonthYearMonth: '2027-02',
                  worstMonthProjectedBalance: 14200.0,
                  breakEvenYearMonth: '2027-08',
                  monthsUntilFreedom: 18,
                  timeline: [
                    MonthlyProjectionPointModel(
                      yearMonth: '2026-11',
                      projectedMrr: 121500.0,
                      projectedDirectCosts: 18000.0,
                      projectedOpex: 50600.0,
                      projectedDebtInstallments: 22000.0,
                      projectedNetCashFlow: 30900.0,
                      cumulativeCashBalance: 85900.0,
                      isNegativeBalance: false,
                    ),
                  ],
                ),
              ),
              networkProjectsPaybackProvider.overrideWith(
                (ref) async => const [
                  NetworkProjectPaybackModel(
                    projectId: '0195f000-0000-7000-8000-000000000901',
                    name: 'Expansão Bairro Esperança - Fase 1',
                    neighborhood: 'Esperança',
                    city: 'Santarém',
                    status: 'ACTIVE',
                    budgetAmount: 65000.0,
                    targetSubscribers: 300,
                    startDate: '2026-06-01',
                    ctoCount: 12,
                    totalPorts: 192,
                    occupiedPorts: 144,
                    penetrationRate: 75.0,
                    monthlyRevenue: 14400.0,
                    accumulatedRevenue: 72000.0,
                    paybackMonthsEstimated: 5,
                    isPaybackAchieved: true,
                    commercialPriority: 'META_ATINGIDA',
                  ),
                ],
              ),
              activeSentinelAlertsProvider.overrideWith(
                (ref) async => const [
                  SentinelAuditAlertModel(
                    id: '0195f000-0000-7000-8000-000000000902',
                    auditType: 'ONU_ORFA_SEM_CONTRATO',
                    severity: 'CRITICAL',
                    title: 'ONU Autorizada na OLT sem Contrato Ativo no ERP',
                    description: 'Serial HWTC889911AA trafegando 420 GB sem faturamento vinculado.',
                    geminiAnalysis: 'Indício de ativação clandestina fora do fluxo de O.S.',
                    recommendedAction: 'Bloquear porta GPON imediatamente e auditar técnico.',
                    resolved: false,
                  ),
                ],
              ),
            ],
            child: const MaterialApp(
              home: FinancialDashboardScreen(),
            ),
          ),
        );

        await tester.pumpAndSettle();

        // Navega para a aba DRE & Desalavancagem 36M
        await tester.ensureVisible(find.text('DRE & Desalavancagem 36M'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('DRE & Desalavancagem 36M'));
        await tester.pumpAndSettle();

        expect(find.textContaining('(=) Receita Operacional Bruta'), findsOneWidget);
        expect(find.textContaining('(=) EBITDA (35.0%)'), findsOneWidget);
        expect(find.text('1. Pior Mês do Caixa'), findsOneWidget);
        expect(find.text('2027-02'), findsOneWidget);
        expect(find.text('18 meses'), findsOneWidget);

        // Alterna regime para Caixa (CASH)
        await tester.tap(find.text('Regime de Caixa (CASH)'));
        await tester.pumpAndSettle();
        expect(find.textContaining('DRE Telecom Oficial (Regime de Caixa)'), findsOneWidget);

        // Navega para a aba Payback FTTH & Sentinela
        await tester.ensureVisible(find.text('Payback FTTH & Sentinela'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Payback FTTH & Sentinela'));
        await tester.pumpAndSettle();

        expect(find.textContaining('Expansão Bairro Esperança - Fase 1'), findsOneWidget);
        expect(find.text('Status: PAYBACK ATINGIDO'), findsOneWidget);
        expect(find.textContaining('ONU Autorizada na OLT sem Contrato Ativo no ERP'), findsOneWidget);
        expect(find.text('Resolver Alerta'), findsOneWidget);
      },
    );
  });
}
