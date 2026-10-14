# Brainstorming Onda C — Decisões Consolidadas do `/grill-me` (v2)

**Data:** 2026-10-14  
**Status:** Aprovado pelo Usuário — Em Execução (Parte 1 de 2)

---

## 1. Resumo das Decisões Arquiteturais (`/grill-me`)

1. **Arquitetura de Navegação Financeira (`/financial` e `/my-cash-custody`):**
   - Expandir a rota `/financial` (`FinancialDashboardScreen`) em um **Cockpit Financeiro** com `TabBar` (`isScrollable: true`, `tabAlignment: TabAlignment.start`).
   - Evoluir `/my-cash-custody` (`MyCashCustodyScreen`) para suportar liquidação em espécie (`/settle`) e repasse físico entre colaboradores com aceite duplo (`/transfer/request`, `/transfer/pending`, `/transfer/{id}/respond`).

2. **Contas a Pagar & Plano de Contas:**
   - Visão estruturada em 3 sub-seções operacionais dentro da aba `Contas a Pagar & Plano de Contas`:
     1. **Parcelas Pendentes** (`GET /financial/payables/installments/pending` + `POST /financial/payables/installments/{id}/pay` com valor pago, meio de pagamento e comprovante).
     2. **Notas / Despesas** (`GET /financial/payables` + `POST /financial/payables` vinculado a `chartOfAccountId`, geração automática de `installmentsCount` e vínculo opcional com `projectId`).
     3. **Plano de Contas Hierárquico** (`GET /financial/chart-of-accounts/tree`, `GET /financial/chart-of-accounts`, `POST /financial/chart-of-accounts`, `PUT /financial/chart-of-accounts/{id}`).

3. **Custódia Física (Dual-Acceptance) & Auditoria do CFO:**
   - Em `/my-cash-custody`:
     - Liquidação em Espécie (`POST /financial/custody/cash/settle`: `INVOICE` ou `WORK_ORDER_FEE`).
     - Solicitar Repasse (`POST /financial/custody/cash/transfer/request`).
     - Bandeja de Repasses Pendentes (`GET /financial/custody/cash/transfer/pending` + `POST /financial/custody/cash/transfer/{id}/respond` com aceite ou justificativa de recusa).
   - Na aba `Auditoria de Caixa & Alçadas` em `/financial`:
     - Conferência de Depósitos Bancários na boca do caixa (`POST /financial/custody/cash/deposit/{id}/audit` com header `X-User-Id`).
     - Alçadas de Isenção de Taxa de O.S. (`GET /financial/work-orders/waiver/pending`, `POST /financial/work-orders/{id}/waiver/audit` com header `X-User-Id`, além de `POST /financial/work-orders/{id}/assign-fee` e `POST /financial/work-orders/waiver/request`).

4. **Cadência de Execução em 2 Etapas (Sob Demanda):**
   - **Etapa 1 (Agora):** Entregar `Contas a Pagar & Plano de Contas`, `Auditoria de Caixa & Alçadas` em `/financial`, e evolução completa de `/my-cash-custody` (Liquidação + Repasses Dual-Acceptance), com testes unitários/widget e `flutter analyze` zero erros. Pausar para revisão e aprovação do usuário.
   - **Etapa 2 (Após aprovação da Etapa 1):** Entregar `DRE & Desalavancagem 36M` e `Payback FTTH & Sentinela` em `/financial`, com testes unitários/widget e atualização do `docs/SDD.md`.
