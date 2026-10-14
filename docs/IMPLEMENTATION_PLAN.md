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
