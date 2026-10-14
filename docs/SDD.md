# SDD — System Design & Specification Document (`ispERP`)

**Versão:** 2.0 (Realinhamento Arquitetural & Spec-Driven Development)  
**Data de Atualização:** 14 de Outubro de 2026  
**Status:** Especificação Oficial Ativa (Fonte Única da Verdade de Engenharia)

---

## 1. Propósito e Escopo do SDD

Este **System Design & Specification Document (SDD)** consolida a arquitetura real, os contratos de comunicação, o modelo de dados, o estado de implementação de cada componente e o plano estruturado de evolução do **ispERP**.

Qualquer nova funcionalidade, refatoração ou correção deve respeitar estritamente:
1. **Workflow API-First (`contracts/openapi/`):** Toda rota HTTP e DTO nasce no contrato OpenAPI 3.0.3 antes de ser implementada no Backend ou consumida nos Frontends.
2. **Zero Dados Falsos e Desenvolvimento Sob Demanda:** Proibição absoluta de mocks estáticos em telas de produção ou funcionalidades não solicitadas ([`.agents/rules/zero-fake-data-demand-driven.md`](file:///Volumes/240/Code/ispERP/.agents/rules/zero-fake-data-demand-driven.md)).
3. **Sobriedade Terminológica:** Uso exclusivo de nomenclatura técnica, contábil e regulatória padrão da indústria de telecomunicações ([`.agents/rules/sobriedade-terminologica-e-zero-exageros.md`](file:///Volumes/240/Code/ispERP/.agents/rules/sobriedade-terminologica-e-zero-exageros.md)).
4. **Segurança e Testes Obrigatórios:** Identificadores UUIDv7, autenticação JWT com RBAC em nível de método (`@PreAuthorize`), proteção de segredos e cobertura de testes automatizados em todas as entregas.

---

## 2. Topologia do Monorepo (As 4 Stacks)

O repositório é organizado em uma arquitetura desacoplada de **4 camadas principais** mais a camada de infraestrutura containerizada:

```mermaid
flowchart TB
    subgraph Contracts["1. Contratos OpenAPI 3.0.3 (contracts/openapi/)"]
        YAML["34 Arquivos YAML em 22 Domínios"]
        Bundle["openapi.bundled.json (Redocly CLI)"]
        YAML --> Bundle
    end

    subgraph Backend["2. Backend Core (backend/)"]
        API_Interfaces["Interfaces Geradas (*Api.java & DTOs)"]
        Controllers["53 Controllers Spring Boot 4.1.1 (Java 25)"]
        Services["71 Services + Outbox Event Bus"]
        DB[("PostgreSQL 17\n(Flyway V1..V36 | UUIDv7)")]
        RADIUS["FreeRADIUS v3 (rlm_sql_postgresql)\nUDP 1812 / 1813 / 3799 CoA"]
        API_Interfaces --> Controllers --> Services --> DB
        RADIUS <--> DB
    end

    subgraph Frontends["3. Frontends Desacoplados"]
        Flutter["frontend-flutter/\nERP Operacional Multiperfil\n(Flutter 3.x + Riverpod + GoRouter + Dio)"]
        Customer["frontend-customer/\nCentral do Assinante\n(Vanilla TS + Vite 6 + HTML5/CSS3)"]
        Site["frontend-site/\nSite Institucional & Planos\n(Vanilla TS + Vite 6 + HTML5/CSS3)"]
    end

    Bundle -->|./gradlew openApiGenerate| API_Interfaces
    Flutter -->|REST JSON + Bearer JWT| Controllers
    Customer -->|REST JSON + CPF/PIN| Controllers
    Site -->|REST JSON Público| Controllers
```

### 2.1. Resumo Tecnológico e Licenciamento por Diretório

| Componente | Diretório | Stack Tecnológica Real | Licença | Papel no Ecossistema |
| :--- | :--- | :--- | :--- | :--- |
| **Contratos API** | [`contracts/openapi/`](file:///Volumes/240/Code/ispERP/contracts/openapi) | OpenAPI 3.0.3 + Redocly CLI | GNU AGPLv3 | Fonte única da verdade para rotas REST, schemas e validações |
| **Backend Core** | [`backend/`](file:///Volumes/240/Code/ispERP/backend) | Java 25, Spring Boot 4.1.1, PostgreSQL 17, Flyway (`V1`–`V36`), MapStruct 1.6.3, JSpecify | GNU AGPLv3 | Regras de negócio, faturamento, contabilidade, FTTH, RADIUS e integrações |
| **ERP Operacional** | [`frontend-flutter/`](file:///Volumes/240/Code/ispERP/frontend-flutter) | Flutter 3.x, Riverpod 2.6, GoRouter 14.8, Dio 5.8, FlutterSecureStorage | GNU AGPLv3 | Cliente único para colaboradores (Admin, Comercial, Suporte, Despacho, Técnico, Financeiro e Estoque) |
| **Central do Assinante** | [`frontend-customer/`](file:///Volumes/240/Code/ispERP/frontend-customer) | Vanilla TypeScript, Vite 6.1, HTML5/CSS3 (zero dependências de runtime) | MIT | Autoatendimento do assinante (login CPF+PIN, 2ª via Pix, Desbloqueio em Confiança 48h) |
| **Site Institucional** | [`frontend-site/`](file:///Volumes/240/Code/ispERP/frontend-site) | Vanilla TypeScript, Vite 6.1, HTML5/CSS3 (zero dependências de runtime) | MIT | Vitrine pública de planos ativos (`GET /plans/active`) e captação comercial |
| **Infra & RADIUS** | [`docker/`](file:///Volumes/240/Code/ispERP/docker) | Docker Compose, FreeRADIUS v3 (`rlm_sql_postgresql`), Nginx | GNU AGPLv3 | Orquestração de containers, autenticação PPPoE e proxy reverso |

---

## 3. Governança de Contratos OpenAPI & Codegen

### 3.1. Estrutura Modular (`contracts/openapi/`)
- **Arquivo Raiz:** [`contracts/openapi/openapi.yaml`](file:///Volumes/240/Code/ispERP/contracts/openapi/openapi.yaml)
- **Domínios (`contracts/openapi/domains/`):** `audit`, `auth`, `backup`, `bi`, `billing`, `companies`, `contracts`, `customers`, `financial`, `fiscal`, `ftth`, `geocoding`, `helpdesk`, `infra`, `inventory`, `network`, `plans`, `portal`, `radius`, `sales`, `settings`, `users`, `workorders`.
- **Regras de Contrato:**
  1. **Zero `@Entity` nas Respostas/Requisições:** Nenhum controller pode expor ou receber entidades JPA diretamente. Todos os 53 controllers implementam as interfaces geradas em `br.dev.xb.isperp.api.contract.*Api`.
  2. **Pipeline de Geração (`./scripts/generate-api.sh`):**
     - Executa `./scripts/bundle-contracts.sh` (Redocly `bundle` + `lint`) gerando `contracts/openapi/openapi.bundled.json`.
     - Executa `./gradlew openApiGenerate` no `backend/`, gerando as interfaces Spring `@Validated` e os DTOs Java em `backend/build/generated/openapi/`.
  3. **Consumo nos Frontends:**
     - `frontend-flutter/`, `frontend-customer/` e `frontend-site/` utilizam repositórios HTTP enxutos (`Dio` no Flutter; `fetch` nativo nos portais web) e modelos tipados próprios, mantendo zero acoplamento com geradores pesados (`built_value` ou Orval), porém rigorosamente aderentes aos schemas do `openapi.yaml`.

---

## 4. Arquitetura do Backend (`backend/`)

### 4.1. Padrões Estruturais
- **Context Path:** `/api` (configurado em `application.yml`).
- **Identificadores Primários:** 100% **UUIDv7** (RFC 9562) via `UuidCreatorUtils.generateUuidV7()` no Java e `DEFAULT uuidv7()` nativo no PostgreSQL 17.
- **Arquitetura Orientada a Eventos (Transactional Outbox):**
  - Eventos de domínio são gravados transacionalmente na tabela `outbox_events` (`V3`) e processados assincronamente com garantia *at-least-once* e controle de idempotência (`processed_events`, `V4`).
- **Mapeamento Objeto-Objeto:** MapStruct `1.6.3` (`backend/src/main/java/br/dev/xb/isperp/mapper/`).
- **Segurança (`SecurityConfig.java` + `@EnableMethodSecurity`):**
  - Autenticação stateless via JWT Bearer (`JJWT 0.13.0`) com verificação de usuário ativo (`active = true`).
  - Controle de acesso RBAC com 11 papéis (`UserRole`): `ADMIN`, `DIRECTOR`, `CFO`, `FINANCIAL`, `ATTENDANT`, `ADMINISTRATIVE_ASSISTANT`, `SUPPORT_N2`, `SUPPORT_ANALYST`, `TECHNICIAN`, `USER`, `CLIENT`.
  - Mascaramento obrigatório de credenciais sensíveis (`••••••••` + 4 últimos caracteres) em respostas `GET` de Gateways de Pagamento, Notificações, NAS e Dispositivos de Rede.
  - Validação HMAC-SHA256 *fail-closed* nos webhooks de pagamento e assinatura Pix.

### 4.2. Histórico Completo de Migrações Flyway (`V1` a `V36`)

| Migração | Descrição Técnica |
| :--- | :--- |
| `V1`–`V7` | Schema inicial (`companies`, `users`, `plans`, `customers`, `contracts`, `work_orders`, `invoices`), índices de performance, `outbox_events`, `processed_events`, `sales`, `network_devices`, `onu_provisionings`, `notification_logs`. |
| `V8`–`V12` | Portal do Assinante, roteirização de O.S. e BI, almoxarifado multi-depósito e custódia (`warehouses`, `serialized_assets`, `stock_transfers`, `tool_custody_agreements`, `custody_logs`), faturamento hierárquico e Helpdesk Anatel (`helpdesk_tickets`, `ticket_interactions`). |
| `V13`–`V17` | Fiscal NFCom Modelo 62 (`fiscal_companies`, `nfcom_records`), geolocalização nas O.S., base local GeoCEP (`cep_coordinates`), configurações dinâmicas de WhatsApp/SMTP (`notification_configs`) e multi-gateway de pagamento (`payment_gateway_configs`). |
| `V18`–`V23` | Storage S3/SeaweedFS (`storage_configs`), transição de regime tributário (`fiscal_regime_transitions`), assinatura eletrônica via Pix (`contract_templates`, `electronic_signature_sessions`), CGNAT e Marco Civil (`cgnat_mappings`, `marco_civil_reports`), IPAM (`ipam_asns`, `ipam_vrfs`, `ipam_subnets`, `ipam_ip_addresses`), Topologia FTTH (`ftth_pops`, `ftth_poles`, `ftth_cables`, `ftth_closures`, `ftth_splitters`, `ftth_fusions`, `ftth_ctos`, `ftth_cto_ports`). |
| `V24`–`V28` | Monitoramento PON e incidentes NOC (`olt_pon_ports`, `ftth_incidents`), triagem de materiais de instalação (`installation_material_demands`), políticas de ciclo de vida RADIUS (`radius_policy_configs`, `radius_lifecycle_logs`), retirada de comodato e cobrança jurídica (`legal_collection_records`), Backup & Disaster Recovery (`backup_policies`, `backup_destinations`, `backup_execution_logs`). |
| `V29`–`V31` | Gestão financeira avançada: Plano de Contas, Contas a Pagar, Custódia de Caixa por CPF, Taxas de O.S., Projetos de Rede FTTH, Alertas de Auditoria (`sentinel_audit_logs`), configurações visuais do site (`site_settings`) e logs de auditoria administrativa (`audit_logs`). |
| `V32` | Colunas de autenticação por PIN na Central do Assinante (`portal_pin_hash`, `portal_pin_must_change` em `customers`). |
| `V33` | Colunas de rastreabilidade quantitativa de estoque (`quantity`, `balance_after` em `custody_logs`). |
| `V34` | Coluna `vendor` (`MIKROTIK`, `HUAWEI`, `GENERIC`) na tabela `nas` para atributos RADIUS multi-vendor. |
| `V35` | Colunas `framed_route` (IPv4) e `framed_ipv6_route` (IPv6) em `onu_provisionings` para entrega de rotas estáticas B2B via RADIUS. |
| `V36` | Integração nativa com `rlm_sql_postgresql` do FreeRADIUS v3: criação das tabelas `radpostauth` e `nasreload`, e adição das colunas `framedipv6address`, `framedinterfaceid` e `class` em `radacct`. |

---

## 5. Matriz de Implementação do `frontend-flutter/` e Plano de Evolução

### 5.1. Matriz RBAC de Perfis no Flutter (`UserRole`)
Todos os 11 papéis do Backend devem ser mapeados em [`lib/core/models/user_role.dart`](file:///Volumes/240/Code/ispERP/frontend-flutter/lib/core/models/user_role.dart):
- **Diretoria & Administração (`ADMIN`, `DIRECTOR`):** Acesso integral a todas as rotas e configurações.
- **Gestão Financeira (`CFO`, `FINANCIAL`):** Acesso ao Cockpit Financeiro, Custódia de Caixa, Atendimento e BI.
- **Comercial & Atendimento (`ATTENDANT`, `ADMINISTRATIVE_ASSISTANT`):** Acesso ao Balcão Omnichannel (`AttendanceHubModal`), Vendas, Suporte N1 e Minha Custódia de Caixa.
- **Suporte Técnico & NOC (`SUPPORT_N2`, `SUPPORT_ANALYST`):** Acesso ao Helpdesk/Suporte, Torre de Despacho, Atendimento e Diagnóstico.
- **Campo & Almoxarifado (`TECHNICIAN`, `USER`):** Acesso às Ordens de Serviço de Campo, Execução Técnica, Almoxarifado/Custódia e Minha Custódia de Caixa.

### 5.2. Fases e Ondas de Desenvolvimento (Estritamente Sob Demanda)

#### Fase 1 (Concluída) — Alinhamento SDD, Contratos, Infra e Fechamento do Ciclo de Vida MVP
- **Entrega 1.1 (Documentação & Contratos OpenAPI — Concluída):**
  - Criação deste `docs/SDD.md` e atualização de `docs/` e `.agents/rules/`.
  - Migração dos 8 endpoints manuais (`UserController`, `InventoryController`, `AssetCustodyController`, `WorkOrderController`, `TechnicianFieldExecutionController`) para `contracts/openapi/`, eliminando retornos de `@Entity`.
- **Entrega 1.2 (Infraestrutura Docker & CI/CD — Concluída):**
  - Otimização de `.github/workflows/deploy-pages.yml` (`@v7`/`@v5` Node 24 + `paths-ignore` + 5 pilares de economia).
  - Conversão de `docker/Dockerfile.backend` para multi-stage build e remoção de `docker/wait-for-mysql.sh`.
  - Correção de conflito de portas e configuração Nginx (`docker/nginx-prod.conf`) em `docker-compose.prod.yml` e inclusão do serviço `customer` (`docker/Dockerfile.customer`).
- **Entrega 1.3 (Saneamento de Bugs, RBAC e Integração Real das 5 Telas do `frontend-flutter` — Concluída):**
  - Correção de `/radius/nas` em `nas_repository.dart`.
  - Mapeamento de todos os papéis RBAC do backend em `user_role.dart`.
  - Remoção de todos os dados hardcoded e integração real das 5 telas:
    1. `SalesDashboardScreen` ➔ `GET /plans/active` e `POST /sales`.
    2. `TechnicianDashboardScreen` ➔ `GET /work-orders` e `POST /technician/execution/{id}/complete`.
    3. `SupportDashboardScreen` ➔ `GET /work-orders` e busca integrada ao `AttendanceHubModal` (`GET /customers/search`).
    4. `FinancialDashboardScreen` ➔ Ação real de Desbloqueio em Confiança (`POST /radius/lifecycle/action`).
    5. `MyCashCustodyScreen` ➔ `GET /financial/custody/cash/all` e `GET /financial/custody/cash/deposit/pending`.

#### Ondas Subsequentes no `frontend-flutter` (Aguardando Solicitação Explícita por Onda)
- **Onda A — Governança, Configurações Unificadas, Planos, Contratos e Backup:**
  - Hub de Configurações (`/admin/settings`: Empresa, Site, SMTP, WhatsApp, Storage S3), CRUD de Planos (`/plans`), Templates de Contrato (`/contracts/templates`) e Backup & Disaster Recovery (`/financial/backup/*`).
- **Onda B — Engenharia de Rede, Topologia FTTH, RADIUS Sessions, IPAM, CGNAT e Marco Civil:**
  - Gestão de OLTs/PONs/CTOs/ONUs (`/ftth/*`, `/network-devices`, `/onus`), Monitoramento NOC (`/ftth/monitoring/*`), Sessões RADIUS Ativas e PoD (`/radius/sessions/*`, `/radius/disconnect`), IPAM (`/ipam/*`), CGNAT (`/cgnat/*`) e Marco Civil (`/marco-civil/*`).
- **Onda C — Gestão Financeira Avançada, DRE e Custódia Patrimonial:**
  - Plano de Contas (`/financial/chart-of-accounts`), Contas a Pagar (`/financial/payables`), Conferência de Custódia pelo Financeiro (`/financial/custody/cash/*`), Alçadas de Taxas de O.S. (`/financial/work-orders/*`), Relatório DRE (`/financial/reports/dre`), Desalavancagem 36M (`/financial/deleveraging/*`), Payback de Projetos FTTH (`/financial/network-projects/*`) e Alertas de Auditoria (`/financial/sentinel/*`).
- **Onda D — Módulo Fiscal & Conformidade Tributária:**
  - Painel NFCom Modelo 62 (`/fiscal/records`, `/fiscal/invoices/{id}/emit`), Configuração Fiscal e Certificado A1 (`/fiscal/company`), Exportação Convênio ICMS 115/03 (`/fiscal/convenio115/*`) e Transição de Regime (`/fiscal/regimes/transitions`).
