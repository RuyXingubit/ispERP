import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/features/financial/data/cash_custody_repository.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_model.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_provider.dart';
import 'package:isperp_app/features/financial/data/payables_and_accounts_repository.dart';
import 'package:isperp_app/features/financial/presentation/financial_dashboard_screen.dart';
import 'package:isperp_app/features/financial/presentation/my_cash_custody_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  group('Onda C (Etapa 1) — Contas a Pagar, Plano de Contas, Custódia Dual-Acceptance & Alçadas', () {
    test(
      'ChartOfAccountModel, ExpenseInstallmentModel e PayableInvoiceModel deserializam conforme OpenAPI',
      () {
        final treeJson = {
          'id': '0195f000-0000-7000-8000-000000000101',
          'code': '4.1',
          'name': 'Despesas Operacionais (OPEX)',
          'accountType': 'OPEX',
          'dreCategory': 'OPEX_ADMIN',
          'level': 1,
          'acceptsEntries': false,
          'active': true,
          'children': [
            {
              'id': '0195f000-0000-7000-8000-000000000102',
              'parentId': '0195f000-0000-7000-8000-000000000101',
              'parentCode': '4.1',
              'code': '4.1.01',
              'name': 'Aluguel de Postes Concessionária',
              'accountType': 'OPEX',
              'dreCategory': 'OPEX_POLES',
              'level': 2,
              'acceptsEntries': true,
              'active': true,
              'children': [],
            }
          ],
        };

        final root = ChartOfAccountModel.fromJson(treeJson);
        expect(root.code, '4.1');
        expect(root.acceptsEntries, isFalse);
        expect(root.children.length, 1);
        expect(root.children.first.code, '4.1.01');
        expect(root.children.first.dreCategory, 'OPEX_POLES');

        final payableJson = {
          'id': '0195f000-0000-7000-8000-000000000201',
          'supplierName': 'Equatorial Energia',
          'supplierDocument': '04895728000180',
          'chartOfAccountId': '0195f000-0000-7000-8000-000000000102',
          'chartOfAccountCode': '4.1.01',
          'chartOfAccountName': 'Aluguel de Postes Concessionária',
          'description': 'Compartilhamento de Infraestrutura de Postes - Out/2026',
          'invoiceNumber': 'NF-99881',
          'totalAmount': 18500.00,
          'issueDate': '2026-10-05',
          'status': 'PENDING',
          'projectId': '0195f000-0000-7000-8000-000000000301',
          'projectName': 'Expansão Bairro Esperança',
          'installments': [
            {
              'id': '0195f000-0000-7000-8000-000000000202',
              'payableInvoiceId': '0195f000-0000-7000-8000-000000000201',
              'installmentNumber': 1,
              'dueDate': '2026-10-25',
              'amount': 18500.00,
              'status': 'PENDING',
            }
          ],
        };

        final invoice = PayableInvoiceModel.fromJson(payableJson);
        expect(invoice.supplierName, 'Equatorial Energia');
        expect(invoice.totalAmount, 18500.00);
        expect(invoice.projectName, 'Expansão Bairro Esperança');
        expect(invoice.installments.length, 1);
        expect(invoice.installments.first.installmentNumber, 1);
        expect(invoice.installments.first.amount, 18500.00);
      },
    );

    test(
      'CashTransferModel, BankDepositModel e WorkOrderFeeModel deserializam conforme OpenAPI',
      () {
        final transfer = CashTransferModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000401',
          'senderUserId': '0195f000-0000-7000-8000-000000000010',
          'senderUserName': 'Carlos Técnico',
          'receiverUserId': '0195f000-0000-7000-8000-000000000011',
          'receiverUserName': 'Ana Caixa Central',
          'amount': 450.00,
          'reason': 'Passagem de turno vespertino',
          'status': 'PENDING_ACCEPTANCE',
        });
        expect(transfer.senderUserName, 'Carlos Técnico');
        expect(transfer.receiverUserName, 'Ana Caixa Central');
        expect(transfer.amount, 450.00);
        expect(transfer.status, 'PENDING_ACCEPTANCE');

        final fee = WorkOrderFeeModel.fromJson({
          'workOrderId': '0195f000-0000-7000-8000-000000000501',
          'protocol': 'OS-2026-0098',
          'customerId': '0195f000-0000-7000-8000-000000000601',
          'customerName': 'Maria Oliveira',
          'serviceType': 'TROCA_DE_COMODO',
          'standardFeeAmount': 120.00,
          'feeStatus': 'PENDING_WAIVER_APPROVAL',
          'waiverReason': 'Cliente fidelizada há 5 anos',
          'waiverRequestedByName': 'Lucas Atendimento',
        });
        expect(fee.protocol, 'OS-2026-0098');
        expect(fee.standardFeeAmount, 120.00);
        expect(fee.feeStatus, 'PENDING_WAIVER_APPROVAL');
        expect(fee.waiverReason, 'Cliente fidelizada há 5 anos');
      },
    );

    testWidgets(
      'FinancialDashboardScreen renderiza as abas de Contas a Pagar & Plano de Contas e Auditoria de Caixa & Alçadas',
      (tester) async {
        SharedPreferences.setMockInitialValues({});
        final prefs = await SharedPreferences.getInstance();

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              sharedPreferencesProvider.overrideWithValue(prefs),
              dashboardBiProvider.overrideWith(
                (ref) async => DashboardBiModel.fromJson({
                  'mrr': 95000.0,
                  'arpu': 110.0,
                  'activeContracts': 860,
                  'totalCustomers': 900,
                  'totalReceivedMonth': 88000.0,
                  'overdueAmount': 4200.0,
                  'defaultRate': 4.4,
                  'pixConversionRate': 78.5,
                  'recentOverdueInvoices': [],
                }),
              ),
              pendingExpenseInstallmentsProvider.overrideWith(
                (ref) async => const [
                  ExpenseInstallmentModel(
                    id: '0195f000-0000-7000-8000-000000000202',
                    payableInvoiceId: '0195f000-0000-7000-8000-000000000201',
                    installmentNumber: 1,
                    dueDate: '2026-10-25',
                    amount: 18500.00,
                    status: 'PENDING',
                  ),
                ],
              ),
              allPayableInvoicesProvider.overrideWith(
                (ref) async => const [
                  PayableInvoiceModel(
                    id: '0195f000-0000-7000-8000-000000000201',
                    supplierName: 'Equatorial Energia',
                    chartOfAccountId: '0195f000-0000-7000-8000-000000000102',
                    chartOfAccountCode: '4.1.01',
                    chartOfAccountName: 'Aluguel de Postes',
                    description: 'Compartilhamento de Postes Out/2026',
                    totalAmount: 18500.00,
                    issueDate: '2026-10-05',
                    status: 'PENDING',
                  ),
                ],
              ),
              chartOfAccountsTreeProvider.overrideWith(
                (ref) async => const [
                  ChartOfAccountModel(
                    id: '0195f000-0000-7000-8000-000000000102',
                    code: '4.1.01',
                    name: 'Aluguel de Postes Concessionária',
                    accountType: 'OPEX',
                    dreCategory: 'OPEX_POLES',
                    level: 2,
                    acceptsEntries: true,
                    active: true,
                  ),
                ],
              ),
              chartOfAccountsFlatProvider.overrideWith((ref) async => const []),
              allCashCustodiesProvider.overrideWith((ref) async => const []),
              pendingBankDepositsProvider.overrideWith(
                (ref) async => const [
                  BankDepositModel(
                    id: '0195f000-0000-7000-8000-000000000701',
                    depositorUserId: '0195f000-0000-7000-8000-000000000010',
                    depositorUserName: 'Carlos Técnico',
                    amount: 890.00,
                    bankName: 'Sicoob Credisul',
                    receiptFileUrl: 'https://storage.isperp.local/dep701.pdf',
                    status: 'PENDING_AUDIT',
                  ),
                ],
              ),
              pendingWorkOrderFeeWaiversProvider.overrideWith(
                (ref) async => const [
                  WorkOrderFeeModel(
                    workOrderId: '0195f000-0000-7000-8000-000000000501',
                    protocol: 'OS-2026-0098',
                    customerId: '0195f000-0000-7000-8000-000000000601',
                    customerName: 'Maria Oliveira',
                    serviceType: 'TROCA_DE_COMODO',
                    standardFeeAmount: 120.00,
                    feeStatus: 'PENDING_WAIVER_APPROVAL',
                    waiverReason: 'Cliente fidelizada há 5 anos',
                    waiverRequestedByName: 'Lucas Atendimento',
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

        // Navega para a aba Contas a Pagar & Plano de Contas
        await tester.tap(find.text('Contas a Pagar & Plano de Contas'));
        await tester.pumpAndSettle();

        expect(find.text('Cronograma de Parcelas Pendentes de Pagamento'), findsOneWidget);
        expect(find.textContaining('Parcela #1'), findsOneWidget);
        expect(find.text('Baixar Parcela'), findsOneWidget);

        // Alterna para Notas & Despesas
        await tester.tap(find.text('Notas & Despesas'));
        await tester.pumpAndSettle();
        expect(find.textContaining('Equatorial Energia'), findsOneWidget);

        // Alterna para Plano de Contas Hierárquico
        await tester.tap(find.text('Plano de Contas Hierárquico'));
        await tester.pumpAndSettle();
        expect(find.textContaining('4.1.01 — Aluguel de Postes Concessionária'), findsOneWidget);

        // Navega para a aba Auditoria de Caixa & Alçadas
        await tester.ensureVisible(find.text('Auditoria de Caixa & Alçadas'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Auditoria de Caixa & Alçadas'));
        await tester.pumpAndSettle();

        expect(find.textContaining('Sicoob Credisul — Depositante: Carlos Técnico'), findsOneWidget);
        expect(find.text('Aprovar Conciliação'), findsOneWidget);
        expect(find.textContaining('Protocolo OS-2026-0098'), findsOneWidget);
        expect(find.text('Aprovar Isenção'), findsOneWidget);
      },
    );

    testWidgets(
      'MyCashCustodyScreen renderiza ações de custódia e bandeja de repasses Dual-Acceptance',
      (tester) async {
        SharedPreferences.setMockInitialValues({});
        final prefs = await SharedPreferences.getInstance();

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              sharedPreferencesProvider.overrideWithValue(prefs),
              allCashCustodiesProvider.overrideWith(
                (ref) async => const [
                  CashCustodyModel(
                    id: '0195f000-0000-7000-8000-000000000801',
                    userId: '0195f8a0-0000-7000-8000-000000000001',
                    userName: 'Ana Caixa Central',
                    userEmail: 'ana@nexusfibra.com.br',
                    userRole: 'FINANCIAL',
                    currentBalance: 1250.00,
                  ),
                ],
              ),
              pendingBankDepositsProvider.overrideWith((ref) async => const []),
              pendingCashTransfersProvider.overrideWith(
                (ref, userId) async => const [
                  CashTransferModel(
                    id: '0195f000-0000-7000-8000-000000000401',
                    senderUserId: '0195f000-0000-7000-8000-000000000010',
                    senderUserName: 'Carlos Técnico',
                    receiverUserId: '0195f8a0-0000-7000-8000-000000000001',
                    receiverUserName: 'Ana Caixa Central',
                    amount: 450.00,
                    reason: 'Passagem de turno vespertino',
                    status: 'PENDING_ACCEPTANCE',
                  ),
                ],
              ),
            ],
            child: const MaterialApp(
              home: MyCashCustodyScreen(),
            ),
          ),
        );

        await tester.pumpAndSettle();

        expect(find.text('Liquidar Fatura em Espécie'), findsOneWidget);
        expect(find.text('Solicitar Repasse de Caixa'), findsOneWidget);
        expect(find.text('Informar Depósito Bancário'), findsOneWidget);
        expect(find.textContaining('De: Carlos Técnico → Para: Ana Caixa Central'), findsOneWidget);
        expect(find.text('Aceitar Repasse'), findsOneWidget);
        expect(find.text('Recusar Repasse'), findsOneWidget);
      },
    );
  });
}
