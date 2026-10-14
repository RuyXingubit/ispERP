# Brainstorming Onda C — Gestão Financeira Avançada & Blindagem Patrimonial (v1)

**Data:** 2026-10-14  
**Status:** Em Entrevista (`/grill-me`)  
**Escopo:** Integração completa no `frontend-flutter` dos endpoints já implementados no backend Java 25 e contratados em `contracts/openapi/domains/financial/financial.yaml`.

---

## 1. Mapeamento Completo de Contratos OpenAPI e Backend (Onda C)

### 1.1 Plano de Contas Hierárquico (`/financial/chart-of-accounts`)
- `GET /financial/chart-of-accounts/tree` — Árvore hierárquica completa (`ChartOfAccountDto` com `children`)
- `GET /financial/chart-of-accounts` — Lista plana de contas ativas
- `POST /financial/chart-of-accounts` — Criação de conta (`ChartOfAccountRequest`: `parentId`, `code`, `name`, `accountType`, `dreCategory`, `acceptsEntries`)
- `PUT /financial/chart-of-accounts/{id}` — Atualização de conta contábil

### 1.2 Contas a Pagar & Parcelas (`/financial/payables`)
- `GET /financial/payables` — Lista todas as notas/despesas (`PayableInvoiceDto` + `installments`)
- `POST /financial/payables` — Lança despesa parcelada (`PayableInvoiceRequest`: `supplierName`, `supplierDocument`, `chartOfAccountId`, `description`, `invoiceNumber`, `totalAmount`, `issueDate`, `installmentsCount`, `firstDueDate`, `projectId`)
- `GET /financial/payables/installments/pending` — Lista parcelas pendentes (`ExpenseInstallmentDto`)
- `POST /financial/payables/installments/{id}/pay` — Liquida parcela (`paidAmount`, `paymentMethod`, `receiptUrl`)

### 1.3 Custódia Física, Repasses, Liquidação & Alçadas de Taxas de O.S.
- **Custódia de Caixa (`/financial/custody/cash/*`):**
  - `POST /financial/custody/cash/settle` — Recebimento em espécie de fatura ou taxa de O.S. (`CashSettlementRequest`: `targetType` `INVOICE`|`WORK_ORDER_FEE`, `targetId`, `amount`, `notes`)
  - `POST /financial/custody/cash/transfer/request` — Solicitação de repasse físico de espécie entre colaboradores (`CashTransferRequest`: `toUserId`, `amount`, `notes`)
  - `GET /financial/custody/cash/transfer/pending` — Lista repasses aguardando aceite do destinatário
  - `POST /financial/custody/cash/transfer/{id}/respond` — Aceite ou recusa do repasse (`CashTransferResponseRequest`: `accepted`, `rejectionReason`)
  - `POST /financial/custody/cash/deposit/{id}/audit` — Auditoria pelo CFO de depósito bancário na boca do caixa (`BankDepositAuditRequest`: `approved`, `notes`, `rejectionReason`)
- **Taxas de O.S. & Alçadas de Isenção (`/financial/work-orders/*`):**
  - `POST /financial/work-orders/{id}/assign-fee?amount=` — Vincula taxa financeira à O.S.
  - `POST /financial/work-orders/waiver/request` — Solicita isenção de taxa (`WorkOrderFeeWaiverRequest`: `workOrderId`, `reason`)
  - `GET /financial/work-orders/waiver/pending` — Lista pedidos de isenção pendentes de aprovação (`WorkOrderFeeDto`)
  - `POST /financial/work-orders/{id}/waiver/audit` — Aprova ou rejeita isenção (`WorkOrderFeeAuditRequest`: `approved`, `auditNotes`)

### 1.4 DRE Telecom (`/financial/reports/dre`)
- `GET /financial/reports/dre?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&method=ACCRUAL|CASH`
- Retorna `DreReportDto`: Receita Bruta, Deduções de Impostos, Receita Líquida, Custos Diretos (Link/Interconexão), Margem de Contribuição, OPEX (Pessoal, Infraestrutura/Postes/Frota, Administrativo, Comercial), Total OPEX, EBITDA e Margem EBITDA %, Resultado Financeiro, Lucro Líquido e Margem Líquida %.

### 1.5 Motor de Desalavancagem 36M & Simulador de CAPEX (`/financial/deleveraging/*`)
- `GET /financial/deleveraging/projection` — Retorna `DeleveragingProjectionDto` (EBITDA atual, MRR, Churn %, Caixa Inicial, Pior Mês projetado e saldo mínimo, Mês de Break-Even, Meses até Liberdade Financeira e `timeline` de 36 meses)
- `POST /financial/deleveraging/simulate` — Simula impacto de novo CAPEX/empréstimo (`SimulationRequest`: `description`, `totalAmount`, `installmentsCount`, `firstDueDate` -> `SimulationResponse`: viabilidade `feasible`, parcela mensal, pior mês simulado, impacto no caixa, atraso em meses na liberdade financeira e diagnóstico de risco)

### 1.6 Payback de Projetos de Expansão FTTH (`/financial/network-projects/*`)
- `GET /financial/network-projects/payback` — Lista projetos FTTH com CAPEX alocado, CTOs vinculadas, clientes ativos, MRR gerado, % de ocupação, Payback em meses e ROI % (`NetworkProjectPaybackDto`)
- `POST /financial/network-projects` — Cria projeto de expansão FTTH (`NetworkProjectRequest`: `name`, `code`, `neighborhood`, `estimatedCapex`, `expectedSubscribers`, `startDate`, `completionDate`)
- `POST /financial/network-projects/{projectId}/assign-cto/{ctoId}` — Vincula CTO da rede óptica ao projeto financeiro

### 1.7 Auditoria Pericial Sentinela (`/financial/sentinel/*`)
- `GET /financial/sentinel/alerts` — Lista alertas não resolvidos (`SentinelAuditLogDto`: `severity` `INFO`|`WARNING`|`CRITICAL`|`FATAL`, `category` `REVENUE_LEAKAGE`|`ORPHAN_ONU`|`PRICE_TAMPERING`|`UNAUTHORIZED_WAIVER`|`STALE_CASH_CUSTODY`, `title`, `description`, `financialImpactEstimate`, `recommendedAction`)
- `POST /financial/sentinel/sweep` — Dispara varredura imediata de discrepâncias RADIUS/OLT vs Contratos/Faturas
- `POST /financial/sentinel/alerts/{id}/resolve` — Resolve e arquiva alerta com justificativa (`SentinelResolveRequest`: `resolutionNotes`)

---

## 2. Pontos de Decisão para a Entrevista (`/grill-me`)

1. **Arquitetura de Navegação Financeira (`/financial` e `/my-cash-custody`)**
2. **Fluxo Operacional de Custódia Física (Dual-Acceptance) e Mesa de Alçadas do CFO**
3. **Experiência da DRE Telecom (Competência vs Caixa) e Simulador de Desalavancagem 36M**
4. **Cadência de Execução e Validação Automatizada**
