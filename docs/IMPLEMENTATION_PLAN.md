# Plano de Implementação e Evolução — ispERP (Alinhado ao SDD v2.0)

**Data de Atualização:** 14 de Outubro de 2026  
**Documento Mestre de Referência:** [`docs/SDD.md`](file:///Volumes/240/Code/ispERP/docs/SDD.md)

---

## 1. Visão Geral do Estado Atual (100% Concluído)

O **ispERP** opera com sua arquitetura 100% implementada, testada e auditada nas 4 stacks:
1. **Contratos (`contracts/openapi/`):** 22 domínios OpenAPI 3.0.3 cobrindo 100% dos 53 controllers do Backend (zero endpoints fora do contrato e zero exposição direta de `@Entity`).
2. **Backend (`backend/`):** Java 25, Spring Boot 4.1.1, PostgreSQL 17 com Flyway `V1` a `V36`, UUIDv7 nativo (`uuid-creator`), Transactional Outbox, FreeRADIUS v3 integrado, Assinatura Eletrônica Forense (MP 2.200-2 + PIX BACEN) e suíte completa de testes automatizados.
3. **Frontends Web (`frontend-customer/` e `frontend-site/`):** Aplicações leves em Vanilla TypeScript + Vite 6 + HTML5/CSS3 (zero dependências de runtime), atendendo a Central do Assinante (login CPF+PIN, 2ª via Pix, download de PDF/NFCom, detalhamento de descontos/multa/juros, planos de upgrade, Desbloqueio em Confiança 48h e Página Pública de Assinatura Eletrônica Forense `/sign/{token}`) e o Site Institucional (vitrine dinâmica de planos `GET /plans/active`).
4. **Frontend Operacional (`frontend-flutter/`):** Aplicativo multiplataforma em Flutter 3.x (Riverpod + GoRouter + Dio) atendendo todas as personas internas do provedor com zero dados fictícios e 146 testes unitários/widget passando.

---

## 2. Fase 1 — Alinhamento SDD, Infraestrutura e Fechamento do Ciclo de Vida MVP `[CONCLUÍDO]`

### Entrega 1.1 — Documentação SDD & Governança 100% OpenAPI `[CONCLUÍDO]`
- [x] Criação do documento mestre [`docs/SDD.md`](file:///Volumes/240/Code/ispERP/docs/SDD.md).
- [x] Atualização de [`.agents/rules/api-first-contracts.md`](file:///Volumes/240/Code/ispERP/.agents/rules/api-first-contracts.md), [`README.md`](file:///Volumes/240/Code/ispERP/README.md), [`docs/API_FIRST_AND_CONTRACTS.md`](file:///Volumes/240/Code/ispERP/docs/API_FIRST_AND_CONTRACTS.md) e [`docs/IMPLEMENTATION_PLAN.md`](file:///Volumes/240/Code/ispERP/docs/IMPLEMENTATION_PLAN.md) removendo referências ao antigo React/Orval e aplicando sobriedade terminológica.
- [x] Migração dos 8 endpoints pendentes (`UserController`, `InventoryController`, `AssetCustodyController`, `WorkOrderController`, `TechnicianFieldExecutionController`) para `contracts/openapi/` e eliminação de retornos diretos de `@Entity`.

### Entrega 1.2 — Infraestrutura Docker & CI/CD `[CONCLUÍDO]`
- [x] Correção de [`.github/workflows/deploy-pages.yml`](file:///Volumes/240/Code/ispERP/.github/workflows/deploy-pages.yml) (`actions/checkout@v4`, `actions/setup-node@v4`, `concurrency`, `timeout-minutes` e `paths-ignore`).
- [x] Conversão de [`docker/Dockerfile.backend`](file:///Volumes/240/Code/ispERP/docker/Dockerfile.backend) para multi-stage build determinístico (Gradle JDK 25 -> JRE 25) e remoção do script obsoleto `docker/wait-for-mysql.sh`.
- [x] Correção do conflito de portas `80/443` e configuração Nginx em [`docker-compose.prod.yml`](file:///Volumes/240/Code/ispERP/docker-compose.prod.yml) e adição do serviço para `frontend-customer`.

### Entrega 1.3 — Saneamento de Bugs, RBAC e Integração Real das 5 Telas do `frontend-flutter` `[CONCLUÍDO]`
- [x] Correção da rota `/radius/nas` em [`nas_repository.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/admin/data/nas_repository.dart).
- [x] Mapeamento completo dos 11 papéis RBAC do Backend em [`user_role.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/core/models/user_role.dart).
- [x] Remoção de 100% dos dados fictícios/hardcoded e integração com os endpoints reais nas 5 telas principais:
  1. [`sales_dashboard_screen.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/sales/presentation/sales_dashboard_screen.dart) ➔ consome `GET /plans/active` e `GET /sales`.
  2. [`technician_dashboard_screen.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/technician/presentation/technician_dashboard_screen.dart) ➔ consome `GET /work-orders`, `GET /technician/execution/{id}/unprovisioned-onus`, `POST /technician/execution/{id}/provision`, `GET /technician/execution/{id}/radius-status` e `POST /technician/execution/{id}/complete`.
  3. [`support_dashboard_screen.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/support/presentation/support_dashboard_screen.dart) ➔ consome `GET /helpdesk/tickets`, `POST /helpdesk/tickets` e busca integrada ao `AttendanceHubModal`.
  4. [`financial_dashboard_screen.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/financial_dashboard_screen.dart) ➔ conectado ao Desbloqueio em Confiança (`POST /billing/dunning/trust-unblock/attendant`).
  5. [`my_cash_custody_screen.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/my_cash_custody_screen.dart) ➔ consome `GET /financial/custody/cash/user/{userId}` e `POST /financial/custody/cash/deposit`.

---

## 3. Ondas de Evolução no `frontend-flutter` e `frontend-customer` `[100% CONCLUÍDO]`

- [x] **Onda A — Governança, Configurações Unificadas, Planos, Contratos e Backup (`[CONCLUÍDO]`):**
  - Tela unificada de Configurações do Provedor (`Empresa`, `Site`, `SMTP`, `WhatsApp`, `Storage S3`).
  - CRUD de Planos de Internet (`/plans`) e Templates de Contrato (`/contracts/templates`).
  - Painel de Backup & Disaster Recovery (`/financial/backup/*`).
- [x] **Onda B — Engenharia de Rede, Topologia FTTH, RADIUS Sessions, IPAM, CGNAT e Marco Civil (`[CONCLUÍDO]`):**
  - Cadastro e gestão de OLTs, Portas PON, Cabos, CEOs, CTOs e ONUs (`/ftth/*`, `/network-devices`, `/onus`).
  - Monitoramento NOC e Incidentes PON (`/ftth/monitoring/*`).
  - Sessões PPPoE Ativas, Histórico e Desconexão PoD (`/radius/sessions/*`, `/radius/disconnect`).
  - IPAM (`/ipam/*`), CGNAT (`/cgnat/*`) e Marco Civil da Internet (`/marco-civil/*`).
- [x] **Onda C — Gestão Financeira Avançada, DRE e Custódia de Caixa (`[CONCLUÍDO]`):**
  - Plano de Contas Hierárquico (`/financial/chart-of-accounts`) e Contas a Pagar (`/financial/payables`).
  - Conferência de Custódia de Caixa pelo Financeiro (`/financial/custody/cash/*`) e Alçadas de Taxas de O.S. (`/financial/work-orders/*`).
  - Relatório DRE Contábil/Caixa (`/financial/reports/dre`), Projeção de Desalavancagem 36M (`/financial/deleveraging/*`), Payback & Expansão de Rede FTTH (`/financial/network-projects/*`) e Auditoria de Desvios (`/financial/sentinel/*`).
- [x] **Onda D — Fiscal & Conformidade Tributária (`[CONCLUÍDO]`):**
  - Emissão e gestão de NFCom Modelo 62 (`/fiscal/records`, `/fiscal/invoices/{id}/emit`, `/fiscal/records/{recordId}/cancel`) integrada à **Xingubit Pay (`pay.xingubit.com.br`)**.
  - Parametrização Fiscal e Certificado Digital A1 (`/fiscal/company`, `/fiscal/company/{id}/certificate`).
  - Exportação do Convênio ICMS 115/03 (`/fiscal/convenio115/*`) e Transição de Regime Tributário (`/fiscal/regimes/transitions/*`).
- [x] **Onda E — Esteira de Chamados N1 / N2 com Protocolo Anatel e SLA (`[CONCLUÍDO]`):**
  - Central de Atendimento & Helpdesk Anatel em 3 abas (`Fila Geral N1`, `Fila Escalada N2 / NOC`, `Ordens de Serviço de Campo`), busca direta por protocolo (`/helpdesk/tickets/protocol/{protocol}`), timeline de interações e notas internas (`/interactions`), escalonamento N2 (`/escalate-n2`), resolução remota (`/resolve-n2`), abertura de O.S. (`/escalate-work-order`), encerramento com CSAT (`/close`) e etapa integrada no Hub de Atendimento 360.
- [x] **Onda F — Execução Técnica de Campo OLT/RADIUS/Retirada & Assinatura Eletrônica Forense no Flutter (`[CONCLUÍDO]`):**
  - Modal guiado de 4 etapas na O.S. de Campo (`Auto-Find OLT`, `Provisionamento 1-Clique`, `Validação PPPoE/FreeRADIUS`, `Laudo Final & Assinatura`) e fluxo completo de Logística Reversa em O.S. de `RETIRADA` (`/complete-removal` e `/fail-removal`).
  - Modal de Assinatura Eletrônica Forense no Pós-Venda e no Raio-X 360 (`POST /contracts/signatures`, `GET /contracts/{id}/signatures`, `GET /public/signatures/{token}/status`, `POST /public/signatures/{token}/fallback`).
- [x] **Onda G — Portal Público de Assinatura Forense pelo Cliente (`/sign/{token}`) e Completude da Central do Assinante (`frontend-customer/`) (`[CONCLUÍDO]`):**
  - Página pública de Assinatura Eletrônica Forense em [`frontend-customer/`](file:///Volumes/240/Code/ispERP/frontend-customer/src/main.ts) com leitura integral do contrato renderizado, hash SHA-256, checkbox obrigatório de aceite, liberação do PIX simbólico `R$ 1,00` (creditado na 1ª fatura), polling automático a cada 5s (`GET /public/signatures/{token}/status`), download do PDF Assinado + Certificado Forense (`/public/signatures/{token}/pdf`) e escolha de contingência MP 2.200-2 pelo próprio cliente.
  - Exibição no Dashboard logado do código/status do Contrato ativo, detalhamento de descontos/multa/juros e botão **2ª Via PDF / Nota Fiscal NFCom** nas faturas, e vitrine de Planos Disponíveis para Upgrade.

---

## 4. Evolução Estratégica ISP (Ondas H a M — Execução Faseada Sob Demanda)

Conforme documentado em [`docs/brainstorming/evolucao_futura_isp_erp/`](file:///Volumes/240/Code/ispERP/docs/brainstorming/evolucao_futura_isp_erp/v4_consolidacao_onda_h.md), as 6 frentes de alta prioridade para operação de provedores de internet (ISPs) foram faseadas em ondas isoladas:

- [x] **Onda H — Composição Multi-Item de Planos/Faturas (`SCM` + Múltiplos `SVA` / `Locação de Equipamento`) e Tributação Seletiva NFCom Modelo 62 (`[CONCLUÍDO]`):**
  - **Contrato OpenAPI (`contracts/openapi/`):** Adição de `PlanCompositionCategory` (`SCM`, `EQUIPMENT_RENTAL`, `SVA_DIGITAL_CONTENT`, `SVA_STREAMING_APP`, `SVA_OTHER`), `PlanCompositionItemDto`, `InvoiceCompositionItemDto`, `scmAmount`, `svaAmount`, `compositionItems` em `plans` e `billing`, além de `scmTaxableAmount` e `svaExemptAmount` em `NfcomRecordResponse` (`fiscal`).
  - **Backend Java 25 + Flyway `V37` (`backend/`):** Criação das tabelas `plan_composition_items` e `invoice_composition_items` ([`V37__add_plan_and_invoice_multi_item_composition.sql`](file:///Volumes/240/Code/ispERP/backend/src/main/resources/db/migration/V37__add_plan_and_invoice_multi_item_composition.sql)), validação em [`PlanService`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/PlanService.java) garantindo que a soma dos itens em `R$` corresponda ao preço total do plano, snapshot de itens na emissão de faturas ([`InvoiceService`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/InvoiceService.java)) e tributação seletiva em [`FiscalService`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/FiscalService.java) (a NFCom Modelo 62 transmitida via `pay.xingubit.com.br` tributa exclusivamente a soma dos itens `SCM` para ICMS, FUST, FUNTTEL, PIS e COFINS, registrando a parcela `SVA / Locação` em `svaExemptAmount`).
  - **Frontends (`frontend-flutter/` e `frontend-customer/`):** Editor dinâmico multi-item de composição fiscal em [`catalog_and_templates_screen.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/admin/presentation/catalog_and_templates_screen.dart), exibição de `Base SCM (NFCom)` vs `Isento SVA/Locação` em [`fiscal_and_nfcom_tab.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/fiscal_and_nfcom_tab.dart) e detalhamento itemizado nas faturas da Central do Assinante ([`main.ts`](file:///Volumes/240/Code/ispERP/frontend-customer/src/main.ts)).

- [x] **Onda I — Ciclo de Vida Pós-Venda de Contratos (`[CONCLUÍDO]`):**
  - **Contrato OpenAPI (`contracts/openapi/`):** Adição dos endpoints `POST /contracts/{id}/change-plan`, `POST /contracts/{id}/change-address`, `POST /contracts/{id}/transfer-ownership` e `GET /contracts/{id}/lifecycle-events`, além dos schemas `ContractLifecycleEventType`, `ContractPlanChangeRequest`, `ContractAddressChangeRequest`, `ContractOwnershipTransferRequest`, `ContractLifecycleEventResponse`, `ContractLifecycleActionResponse` e do campo `pendingProRataAdjustment` em `ContractResponse`.
  - **Backend Java 25 + Flyway `V38` (`backend/`):** Criação da tabela `contract_lifecycle_events` e coluna `pending_pro_rata_adjustment` ([`V38__add_contract_lifecycle_events_and_prorata.sql`](file:///Volumes/240/Code/ispERP/backend/src/main/resources/db/migration/V38__add_contract_lifecycle_events_and_prorata.sql)), implementação em [`ContractService`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/ContractService.java) e [`InvoiceService`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/InvoiceService.java):
    1. **Upgrade / Downgrade de Plano:** Cálculo *Pro-Rata Die* (base comercial 30 dias) dos dias restantes até o próximo `dueDay`, ajuste automático na fatura `PENDING` aberta do ciclo (ou acúmulo em `pendingProRataAdjustment` para a próxima fatura gerada) e aplicação imediata da nova velocidade via CoA no FreeRADIUS (`RadiusLifecycleService.syncContractToRadius`).
    2. **Mudança de Endereço:** Atualização de endereço/CTO/porta e abertura automática de `WorkOrder` (`type = INSTALACAO`, `status = PENDING_SCHEDULE`) com suporte a taxa configurável (`FeeStatus.BILLABLE`) ou solicitação de isenção comercial/fidelidade (`FeeStatus.PENDING_WAIVER_APPROVAL`).
    3. **Troca de Titularidade:** Transferência do contrato para o novo titular migrando automaticamente todas as faturas `PENDING` e `OVERDUE` do contrato para o novo `customerId` (CPF/CNPJ), preservando a infraestrutura técnica (CTO/porta/ONU) e gerando nova sessão de **Assinatura Eletrônica Forense** (`ContractSignatureSession`).
  - **Frontends (`frontend-flutter/` e `frontend-customer/`):** Modal completo de 4 abas [`contract_post_sale_lifecycle_modal.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/attendance/presentation/contract_post_sale_lifecycle_modal.dart) integrado ao Raio-X 360 ([`step_customer_360_overview.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/attendance/presentation/steps/step_customer_360_overview.dart)) e botão interativo de **Upgrade Imediato (Pro-Rata + CoA)** na Central do Assinante ([`main.ts`](file:///Volumes/240/Code/ispERP/frontend-customer/src/main.ts)).

- [x] **Onda J — Régua de Notificação Ativa Omnichannel & Aviso Proativo de Rompimento PON (`[CONCLUÍDO]`):**
  - **Contrato OpenAPI (`contracts/openapi/`):** Adição dos endpoints `POST /billing/dunning/notifications/run`, `GET /billing/dunning/notifications`, `POST /billing/dunning/notifications/invoice/{invoiceId}/send` e `GET /ftth/monitoring/incidents/active/contract/{contractId}`, além dos schemas `NotificationDunningStage`, `BillingNotificationDispatchResponse`, `BillingNotificationSweepResponse` e do campo `activeNetworkIncident` em `ClientPortalDashboardResponse`.
  - **Backend Java 25 + Flyway `V39` (`backend/`):** Criação da tabela `billing_notification_dispatches` com constraint única idempotente `(invoice_id, stage, channel)` ([`V39__add_billing_notification_dispatches.sql`](file:///Volumes/240/Code/ispERP/backend/src/main/resources/db/migration/V39__add_billing_notification_dispatches.sql)), motor [`BillingNotificationDunningService`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/BillingNotificationDunningService.java) integrado aos canais ativos de `/notifications/configs` para os estágios `D_MINUS_3` (Lembrete + PIX), `D_ZERO` (Vencimento Hoje + PIX), `D_PLUS_5_ANATEL_NOTICE` (Notificação Prévia Legal RGC Anatel Res. 632/765) e `PAYMENT_CONFIRMED` (disparo imediato em [`InvoiceService.markInvoiceAsPaid`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/InvoiceService.java)), além de correlação automática CTO/PON em [`FtthCorrelationEngine.findActiveIncidentForContract`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/FtthCorrelationEngine.java) e [`ClientPortalService`](file:///Volumes/240/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/ClientPortalService.java).
  - **Frontends (`frontend-flutter/` e `frontend-customer/`):** Painel e disparo manual/em lote da Régua Omnichannel na Aba 1 (`Cobrança & Régua`) de [`financial_dashboard_screen.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/financial/presentation/financial_dashboard_screen.dart), e **Banner Proativo de Rompimento / Manutenção Massiva PON** no Hub de Atendimento 360 ([`step_customer_360_overview.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/features/attendance/presentation/steps/step_customer_360_overview.dart)) e na Central do Assinante ([`main.ts`](file:///Volumes/240/Code/ispERP/frontend-customer/src/main.ts) / [`index.html`](file:///Volumes/240/Code/ispERP/frontend-customer/index.html)).

- [ ] **Onda K — Exportador Regulatório Anatel DICI / SICI (`[PLANEJADO]`):**
  - Geração mensal automatizada do relatório oficial da Anatel agrupando acessos ativos por código IBGE do município, tipo de pessoa (`PF` / `PJ`), faixa de velocidade (`downloadSpeed` / `uploadSpeed`) e tecnologia (`FTTH`).

- [ ] **Onda L — Viabilidade Técnica Comercial por CTO & Agenda Técnica por Turno/Capacidade (`[PLANEJADO]`):**
  - Consulta automática no Onboarding Comercial da CTO mais próxima com portas livres (`freePorts > 0`) e distância estimada em metros.
  - Agendamento de O.S. por janela de turno (`MANHÃ` / `TARDE`) respeitando o limite diário de instalações por técnico.

- [ ] **Onda M — Gerência Remota de Wi-Fi via TR-069 / CWMP (ACS) (`[PLANEJADO]`):**
  - Alteração remota de nome da rede Wi-Fi (`SSID`) e senha pelo próprio assinante na Central do Assinante e pelo atendente N1 no Hub 360, além de leitura de dispositivos conectados e reboot remoto da CPE.
