# Brainstorming Onda C — Etapa 1 Concluída (v3)

**Data:** 2026-10-14  
**Status:** Etapa 1 Implementada e Testada (`122/122` testes passando, `flutter analyze` com `0` alertas) — Aguardando aprovação do usuário para iniciar a Etapa 2.

---

## 1. Entregáveis da Etapa 1

1. **Repositório de Contas a Pagar & Plano de Contas ([payables_and_accounts_repository.dart](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/data/payables_and_accounts_repository.dart)):**
   - `GET /financial/chart-of-accounts/tree`, `GET /financial/chart-of-accounts`, `POST /financial/chart-of-accounts`, `PUT /financial/chart-of-accounts/{id}` (`ChartOfAccountModel`).
   - `GET /financial/payables`, `POST /financial/payables`, `GET /financial/payables/installments/pending`, `POST /financial/payables/installments/{id}/pay` (`PayableInvoiceModel`, `ExpenseInstallmentModel`).

2. **Expansão do Repositório de Custódia & Alçadas ([cash_custody_repository.dart](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/data/cash_custody_repository.dart)):**
   - `POST /financial/custody/cash/settle` (Liquidação em espécie vinculada ao `X-User-Id`).
   - `POST /financial/custody/cash/transfer/request`, `GET /financial/custody/cash/transfer/pending`, `POST /financial/custody/cash/transfer/{id}/respond?accept=...` (Repasse físico Dual-Acceptance).
   - `POST /financial/custody/cash/deposit/{id}/audit` (Auditoria de depósito na boca do caixa pelo CFO).
   - `POST /financial/work-orders/{id}/assign-fee`, `POST /financial/work-orders/waiver/request`, `GET /financial/work-orders/waiver/pending`, `POST /financial/work-orders/{id}/waiver/audit` (Alçadas de taxas de O.S.).

3. **Telas e Componentes Operacionais:**
   - [payables_and_accounts_tab.dart](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/payables_and_accounts_tab.dart): Visão Mestre-Detalhe para Parcelas Pendentes (com baixa rápida), Notas & Despesas (com lançamento parcelado e vínculo com Plano de Contas / Projeto FTTH) e Árvore Hierárquica do Plano de Contas.
   - [cfo_audit_and_waivers_tab.dart](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/cfo_audit_and_waivers_tab.dart): Mesa do CFO para conferência de depósitos bancários e aprovação/rejeição de isenções de taxa de O.S.
   - [my_cash_custody_screen.dart](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/my_cash_custody_screen.dart): Liquidação em espécie, solicitação de repasse, submissão de comprovante de depósito e bandeja de aceite/recusa (Duplo Aceite).
   - [financial_dashboard_screen.dart](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/financial_dashboard_screen.dart): Cockpit Financeiro estruturado em 5 abas.

4. **Testes Automatizados ([onda_c_etapa_1_financial_test.dart](file:///Volumes/240/Code/ispERP/frontend-flutter/test/unit/onda_c_etapa_1_financial_test.dart)):**
   - 4 novos grupos de testes unitários e de widget validando serialização OpenAPI e fluxos de UI sem overflow (`122/122` passando).

---

## 2. Próxima Etapa (Etapa 2 — Aguardando Aprovação)
- **Aba 3 (`DRE & Desalavancagem 36M`):** `GET /financial/reports/dre` (Competência `ACCRUAL` e Caixa `CASH`), `GET /financial/deleveraging/projection` e Simulador de CAPEX `POST /financial/deleveraging/simulate`.
- **Aba 4 (`Payback FTTH & Sentinela`):** `GET /financial/network-projects/payback`, `POST /financial/network-projects`, `POST /financial/network-projects/{projectId}/assign-cto/{ctoId}`, `GET /financial/sentinel/alerts`, `POST /financial/sentinel/sweep` e `POST /financial/sentinel/alerts/{id}/resolve`.
- Testes automatizados da Etapa 2 e consolidação final do `docs/SDD.md`.
