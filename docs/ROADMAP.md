# Roadmap de Desenvolvimento (ispERP - Milestones)

Este documento estabelece as etapas e marcos de desenvolvimento priorizados para a evolução do **ispERP**, registrando o status de entrega e os próximos passos.

---

## 🎯 Milestone 1: Fundação Técnica, PostgreSQL 17+, UUIDv7 & Testes (Concluído)
> **Objetivo:** Estabelecer a infraestrutura moderna do banco de dados, migração de IDs para UUIDv7 e garantia de qualidade com suíte de testes automatizados (TDD).

- [x] **Docker & Configuração:**
  - Substituir container MySQL por PostgreSQL 17 (`postgres:17-alpine`) no `docker-compose.yml` e `docker-compose.prod.yml`.
  - Atualizar dependências no `build.gradle` (adicionar `org.postgresql:postgresql` e `com.github.f4b6a3:uuid-creator`, remover drivers MySQL).
  - Atualizar `application.yml` para driver e dialeto PostgreSQL.
- [x] **Migração de Schema (Flyway):**
  - Reescrever migrações `V1`, `V2`, `V3` em PostgreSQL com tipos nativos `UUID DEFAULT uuidv7()`.
- [x] **Refatoração de Entidades Atuais:**
  - `User`, `Company`, `SiteSettings`, `Customer` usando `UUID` v7.
- [x] **Suíte de Testes Automatizados (TDD):**
  - Configurar Testcontainers PostgreSQL para testes de repositório e migração.
  - Criar testes unitários para Services e Controllers com Mockito e JUnit 5 (100% de aprovação).

---

## 🎯 Milestone 2: Motor de Eventos (EDA) & Transactional Outbox (Concluído)
> **Objetivo:** Criar o mecanismo desacoplado e confiável para disparo e consumo de eventos de domínio.

- [x] Tabela `outbox_events` e `processed_events` via Flyway `V3`.
- [x] Implementação do `DomainEventPublisher` com persistência na Outbox.
- [x] Worker/Dispatcher assíncrono para entrega resiliente de eventos (`OutboxDispatcher`).
- [x] Mecanismo de Idempotência (`IdempotencyService` / `ProcessedEvent`).
- [x] Testes unitários do publisher, dispatcher e idempotência (100% de aprovação).

---

## 🎯 Milestone 3: Vendas, Clientes, Contratos & Planos (Concluído)
> **Objetivo:** Implementar o fluxo de contratação e ciclo de vida do cliente.

- [x] Módulo de Planos (Download, Upload, Preços, SVA) e endpoints REST via Flyway `V4`.
- [x] Módulo de Vendas (Endpoint `/sales` para submissão com validação de CPF e emissão de `SaleSubmittedEvent`).
- [x] Consumidor de Vendas: Cadastro/atualização de `Customer` e criação do `Contract` (Status `PENDING_INSTALLATION`) com disparo de `ContractCreatedEvent`.
- [x] Telas no Frontend (Vite 6 / React 19): Catálogo de Planos, Formulário de Venda Rápida e Gestão de Contratos.
- [x] Suíte de testes unitários automatizados cobrindo PlanService, SaleService, ContractService e SaleEventConsumer (100% Green).

---

## 🎯 Milestone 4: Ordens de Serviço (O.S.), Agendamento, Estoque & Campo (Concluído)
> **Objetivo:** Gestão de instalação técnica, verificação de insumos, geração de acessos do cliente e ativação automática pós-visita.

- [x] Migração Flyway `V5` com tabelas `work_orders`, `inventory_items` e preferências de agendamento na venda.
- [x] Célula de Acesso: Geração automática de login (`ruyfranca`) e senha inicial (`franca`) com hash BCrypt a partir do evento `CONTRACT_CREATED` (`ClientCredentialsConsumer`).
- [x] Célula de Almoxarifado: Verificação e reserva automática de insumos (ONT, Drop, Conectores SC/APC, PTO) via `InventoryStockConsumer`.
- [x] Painel de Despacho & Agendamento de O.S.: Gestão de datas, períodos e equipes técnicas via `WorkOrderService`.
- [x] Baixa Técnica de Instalação: Coleta de MAC da ONU, Número de Série e Sinal óptico (dBm), com emissão do evento `WORK_ORDER_COMPLETED` e ativação automática do contrato para `ACTIVE`.
- [x] Telas no Frontend (Vite 6 / React 19): Central de Ordens de Serviço com abas de status, modal de agendamento e modal de conclusão de campo.
- [x] Suíte de testes unitários automatizados cobrindo geração de credenciais, reserva de estoque, despacho e ciclo de vida da O.S. (100% Green).

---

## 🎯 Milestone 5: Faturamento Recorrente, Cobrança & Multi-Gateway de Pagamentos (Concluído)
> **Objetivo:** Geração automatizada de cobranças com suporte a múltiplos gateways simultâneos e integração primária com Xingubit Pay (Pix COB, COBV e NFCom).

- [x] **Entidades & Modelo de Dados:**
  - `Invoice` e `PaymentTransaction` com UUIDv7 via Flyway `V6`.
  - `PaymentGatewayConfig` (armazenando credenciais OAuth, chaves de API, webhook secrets por gateway/empresa).
- [x] **Arquitetura Strategy / Multi-Gateway:**
  - Interface universal `PaymentGateway` com `PaymentGatewayResolver`.
  - Implementação oficial do **Xingubit Pay** (`XingubitPayGateway`) conforme especificação `pay.xingubit.com.br/doc` com Pix Copia e Cola, QR Code dinâmico e validação HMAC.
- [x] **Motor de Cobrança e Faturamento Recorrente:**
  - `InvoiceService` para emissão, cancelamento e baixa de faturas.
  - Scheduler diário de faturamento recorrente (`BillingScheduler`).
  - Webhook controller seguro `/api/webhooks/payments/{gatewayType}` com compensação instantânea e evento `INVOICE_PAID`.
- [x] **Notificações SMTP / E-mail:**
  - `NotificationEventConsumer` para envio assíncrono de faturas, credenciais e confirmação de pagamentos.
- [x] **Telas no Frontend (Vite 6 / React 19):**
  - Painel de Gestão de Faturas com métricas financeiras, modal de Pix Copia e Cola e QR Code.
  - Gerenciador de Gateways de Pagamento (configuração de credenciais).
- [x] Suíte de testes unitários automatizados cobrindo gateway, roteador, webhook e faturas (100% Green).

---

## 🎯 Milestone 6: Provisionamento de Rede Desacoplado & Multi-Driver (Concluído)
> **Objetivo:** Integração com a infraestrutura de rede através de drivers plugáveis (SmartOLT, Microsserviço dedicado, MikroTik e Radius) acionados exclusivamente por eventos de domínio.

- [x] **Entidades & Modelo de Dados:**
  - `NetworkDevice` (OLTs, BRAS PPPoE, Concentradores) e `OnuProvisioning` com UUIDv7 via Flyway `V7`.
- [x] **Arquitetura Strategy / Multi-Driver:**
  - Interface universal `NetworkProvisioner` com `NetworkDriverResolver`.
  - Driver `SmartOltProvisioner` (integração SmartOLT API).
  - Driver `ExternalMicroserviceProvisioner` (preparado para microserviço de rede dedicado).
  - Driver `MockNetworkProvisioner` para testes e homologação.
  - Driver `RadiusCoAProvisioner` (pacotes CoA / Disconnect).
- [x] **Automação de Rede Orientada a Eventos (EDA):**
  - Consumidor `NetworkProvisioningConsumer`:
    - Ao receber `WORK_ORDER_COMPLETED` ➔ Provisiona automaticamente a ONU na OLT com perfis de download/upload do plano.
    - Ao receber `INVOICE_PAID` ➔ Desbloqueia o assinante instantaneamente em tempo real.
- [x] **Serviços & Diagnóstico de Sinal Óptico:**
  - `NetworkProvisioningService` com diagnóstico de atenuação/potência óptica (dBm) e comandos de bloqueio administrativo.
- [x] **Telas no Frontend (Vite 6 / React 19):**
  - Painel NOC de ONUs com visualização de RX Power (dBm) colorido por atenuação e botões de diagnóstico em tempo real.
  - Gerenciador de Equipamentos de Rede (OLTs e Concentradores).
- [x] Suíte de testes unitários automatizados cobrindo driver SmartOLT, roteador, provisionamento e desbloqueio por Pix (100% Green).

---

## 🎯 Milestone 7: Central do Assinante, Autodesbloqueio & WhatsApp Multiprovedor (Concluído no Backend)
> **Objetivo:** Portal do cliente com autoatendimento e mensageria WhatsApp conectada a múltiplos provedores (Evolution API, Z-API, Twilio).

- [x] **Modelo de Dados & Migração (`V8`):**
  - Tabelas `whatsapp_configs`, `whatsapp_templates`, `notification_logs`, `client_portal_access_tokens`, `trust_unblocks`.
- [x] **Mensageria WhatsApp (Strategy Pattern):**
  - `WhatsAppProviderResolver` com drivers para `EvolutionApiWhatsAppProvider`, `ZApiWhatsAppProvider`, `TwilioWhatsAppProvider` e `MockWhatsAppProvider`.
  - Envio automático de mensagens com chave Pix Copia-e-Cola e links de 2ª via.
- [x] **Serviços da Central do Assinante (`ClientPortalService` & `TrustUnblockPolicyService`):**
  - Autenticação por Magic Link / Token temporário.
  - Consulta de faturas em aberto, histórico de pagamentos e consumo.
  - Regra de **Desbloqueio em Confiança (Trust Unblock)** de 48h com validação de política de uso.
- [x] Testes unitários com 100% de aprovação (`ClientPortalServiceTest`, `TrustUnblockPolicyServiceTest`, `TwilioWhatsAppProviderTest`).

---

## 🎯 Milestone 8: Roteirização Inteligente de Campo & Métricas de BI (Concluído no Backend)
> **Objetivo:** Otimização logística de deslocamento dos técnicos e dashboard de Business Intelligence com métricas de ISP.

- [x] **Modelo de Dados & Migração (`V9`):**
  - Coordenadas geográficas (`latitude`, `longitude`) no endereço dos clientes e ordens de serviço.
  - Tabelas `service_routes` e `service_route_stops`.
- [x] **Algoritmo de Roteirização (`RouteOptimizationService`):**
  - Agrupamento de O.S. por proximidade geográfica (Haversine/Nearest Neighbor) para redução de tempo de deslocamento técnico.
- [x] **Dashboard de BI (`DashboardBiService`):**
  - Cálculo de MRR (Receita Recorrente Mensal), Churn Rate, Taxa de Inadimplência, ARPU e tempo médio de atendimento de O.S.
- [x] Testes unitários de BI e roteirização com 100% de aprovação.

---

## 🎯 Milestone 9: Almoxarifado Multi-Depósito, Ativos Serializados & Custódia de Técnicos (Concluído no Backend)
> **Objetivo:** Rastreabilidade integral de equipamentos em trânsito, estoque central vs. veículos e termos de responsabilidade técnica.

- [x] **Modelo de Dados & Migração (`V10`):**
  - Tabelas `warehouses`, `stock_transfers`, `stock_transfer_items`, `serialized_assets`, `tool_custody_agreements`, `custody_logs`.
- [x] **Serviços Especializados (`WarehouseService` & `AssetCustodyService`):**
  - Transferência de itens e ativos serializados (ONTs, Roteadores Wi-Fi 6, Bobinas de Drop) entre almoxarifado central e veículos técnicos.
  - Emissão e gestão de Termos de Custódia de Ferramentas (Máquinas de Fusão, OTDR, Power Meter, Clivador).
  - Logs imutáveis de entrega, devolução e conferência de avarias.
- [x] Testes unitários de estoque e custódia com 100% de aprovação (`AssetCustodyServiceTest`).

---

## 🎯 Milestone 10: Faturamento Hierárquico & Rebalanceamento Inteligente (Concluído no Backend)
> **Objetivo:** Cobrança de clientes corporativos com matriz/filiais e rebalanceamento pro-rata de faturas em caso de alteração de plano ou data de corte.

- [x] **Modelo de Dados & Migração (`V11`):**
  - Hierarquia de faturamento corporativo (`parent_contract_id`, `billing_mode`).
  - Histórico de rebalanceamentos e solicitações de upgrade (`plan_upgrade_requests`, `invoice_rebalance_logs`).
- [x] **Serviços de Billing Avançado (`HierarchicalBillingService` & `InvoiceRebalanceService`):**
  - Agrupamento de múltiplos pontos de acesso de uma empresa em uma única fatura consolidada.
  - Cálculo proporcional exato (dias utilizados no plano antigo vs. plano novo) com emissão de fatura complementar ou crédito no ciclo seguinte.
- [x] Testes unitários cobrindo faturamento hierárquico e rebalanceamento pro-rata (100% Green).

---

## 🎯 Milestone 11: Helpdesk com SLA, Protocolo Anatel & Motor Fiscal NFCom (Concluído no Backend)
> **Objetivo:** Central de chamados com protocolo regulatório Anatel e compliance fiscal para decisão de modelo de tributação em telecom.

- [x] **Modelo de Dados & Migração (`V12`):**
  - Tabelas `helpdesk_tickets`, `helpdesk_interactions`, `nfcom_decisions`.
- [x] **Gestão de Chamados (`HelpdeskService`):**
  - Gerador de protocolos no padrão oficial Anatel (`YYYYMMDD-XXXXXX`).
  - Matriz de prioridade (Crítica, Alta, Média, Baixa) com cálculo automático de SLA de atendimento e solução.
  - Vinculação com O.S. de reparo em campo quando necessário.
- [x] **Motor de Decisão Fiscal (`NfcomDecisionService`):**
  - Classificação de itens da fatura entre SCM (Serviço de Comunicação Multimídia - NFCom Mod. 62) e SVA (Serviço de Valor Adicionado - NFS-e).
- [x] Testes unitários de helpdesk, protocolo Anatel e decisão NFCom (100% Green).

---

## 🎯 Milestone 12: Modernização Java 25, Spring Boot 4.1.1 & Null-Safety JSpecify (Concluído)
> **Objetivo:** Elevar a robustez e segurança do código com o ecossistema Java mais recente e análise estática de nulos rigorosa.

- [x] Atualização da Toolchain para **Java 25** e Spring Boot **4.1.1**.
- [x] Governança de nulos via **JSpecify (`@NullMarked` em package-info.java)** e anotações `@Nullable` explícitas em 100% do backend.
- [x] Configuração centralizada do Jackson (`JacksonConfig`) com testes automatizados de serialização.
- [x] Atualização da imagem Docker (`eclipse-temurin:25-jre-alpine`).
- [x] Suíte completa de 103 testes unitários e de integração 100% aprovada.

---

## 🎯 Milestone 13: Consolidação do Frontend React 19 & Validação E2E (Concluído)
> **Objetivo:** Conectar e validar todas as telas do Frontend (Vite 6 / React 19 / Node 24) com as APIs avançadas disponíveis no backend e teste E2E automatizado.

- [x] **Integração das Telas Operacionais:**
  - [x] Telas de Almoxarifado Multi-Depósito, Ativos Serializados e Termos de Custódia de Técnicos (`InventoryManager.jsx`).
  - [x] Tela de Chamados / Helpdesk com filtro por prioridade, cópia de protocolo Anatel e SLA (`TicketList.jsx`).
  - [x] Painel da Central do Assinante com 2ª via Pix Xingubit Pay, extrato, troca de senha e Desbloqueio em Confiança de 48h (`ClientPortal.jsx`).
  - [x] Painel Financeiro com simulador de rebalanceamento contábil pro-rata (Dona Maria) e régua de cobrança/dunning (`InvoiceList.jsx`).
  - [x] Upgrade para **Node 24** no Dockerfile e `.nvmrc`.
- [x] **Validação E2E dos Fluxos Críticos:**
  - [x] Teste automatizado de ciclo operacional de ponta a ponta (`CompleteOperationalLifecycleE2EIntegrationTest.java`).
  - [x] Suíte completa de 104 testes unitários e de integração 100% aprovada (`./gradlew test`).
  - [x] Build do frontend via Vite 6 aprovado sem warnings (`npm run build`).

---

## 🎯 Milestone 14: Emissão Fiscal NFCom (Modelo 62), Multi-Gateway Fiscal & Convênio ICMS 115/03 (Concluído)
> **Objetivo:** Automação fiscal completa com arquitetura Multi-Gateway (Strategy Pattern), integração oficial com Xingubit Pay para emissão de NFCom e upload de certificado A1, e exportação do Convênio ICMS 115/03.

- [x] **Modelo de Dados & Migração (`V13`):**
  - Tabelas `fiscal_companies`, `fiscal_gateway_configs` e `nfcom_records`.
- [x] **Arquitetura Multi-Gateway Fiscal (Strategy Pattern):**
  - Interface `FiscalGateway`, `FiscalGatewayResolver` e enum `FiscalGatewayType`.
  - Driver de Produção `XingubitPayFiscalDriver` (com suporte a OAuth2, onboarding `/v1/empresas`, configuração `/v1/empresas/{cnpj}/config/nfcom`, upload de certificado A1 `.pfx` e emissão `/v1/invoices/nfcom`).
  - Driver `MockFiscalDriver` para testes e CI/CD.
- [x] **Convênio ICMS 115/03 (`ConvenioIcms115Service`):**
  - Geração dos 4 arquivos magnéticos oficiais (Mestre `M`, Item `I`, Destinatário `D` e Controle `C`) com hash MD5 cruzado e download em `.zip`.
- [x] **Frontend Fiscal (`FiscalDashboard.jsx`):**
  - Aba de Emissões NFCom com busca, status SEFAZ e download de DANFE PDF e XML.
  - Aba de Cadastro de Empresa, parametrização de série/ambiente e upload do Certificado A1.
  - Aba de Exportação do Convênio 115/03 por mês/ano.
- [x] **Testes Automatizados:**
  - 108 testes unitários e de integração 100% aprovados (`./gradlew test`).

---

## 🎯 Milestone 15: App Web Mobile de Campo, Mapas & Crowdsourcing GeoCEP (Concluído)
> **Objetivo:** Interface Web Mobile-First otimizada para smartphones (Chrome/Safari) para técnicos de campo realizarem instalações, coletas e assinaturas com mapas vetoriais nativos GeoCEP e crowdsourcing predial.

- [x] **Mapas Vetoriais Integrados GeoCEP (MapLibre GL):**
  - Componente `GeoCepMapView.jsx` conectado a `https://geocep.api.br/v1/maps/style.json` com renderização a 60fps, visualização da rota, posição GPS do técnico e residência do cliente.
- [x] **Crowdsourcing Predial GeoCEP (`POST /v1/contribute`):**
  - Integração no `GeoCepClient`, `GeoCepController` e no portal do técnico para envio da coordenada real em frente ao número do imóvel com precisão submétrica.
- [x] **Assinatura Digital Touch & Termos em Campo:**
  - Canvas touch interativo para assinatura na tela com o dedo e armazenamento em Base64 na tabela `work_orders` (Flyway `V15`).
- [x] **Interface Mobile do Técnico (`TechnicianPortal.jsx`):**
  - Cards verticais de O.S., ações em 1 toque (WhatsApp, Ligar), captura de GPS e baixa técnica com ativação imediata (`WORK_ORDER_COMPLETED`).
- [x] **Testes Automatizados:**
  - Suíte de testes unitários e de integração 100% aprovada (`./gradlew test`).

---

## 🎯 Milestone 16: OpenAPI Swagger UI, MapStruct Mandatório & TypeScript no Frontend (Concluído)
> **Objetivo:** Modernizar a integração Fullstack com documentação interativa viva, eliminação total do ModelMapper em prol do MapStruct (compilação rápida e segura) e adoção do TypeScript no React 19 para tipagem estrita e POO no cliente.

- [x] **OpenAPI & Swagger UI Interativo (`springdoc-openapi`):**
  - Documentação viva em `/swagger-ui.html` e contrato `/v3/api-docs`.
  - Suporte a Bearer Token JWT direto no Swagger UI para testes rápidos (`OpenApiConfig.java`).
  - Endpoints de documentação liberados no `SecurityConfig.java`.
- [x] **Migração Total para MapStruct & Remoção do ModelMapper:**
  - Configuração do MapStruct 1.6.3 + `lombok-mapstruct-binding` no Java 25.
  - Implementação das interfaces `@Mapper(componentModel = "spring")` (`PlanMapper`, `ContractMapper`, `WorkOrderMapper`).
  - Remoção definitiva da biblioteca `ModelMapper`.
  - Regra arquitetural (**ADR 016**) tornando o MapStruct padrão obrigatório para novos desenvolvimentos.
- [x] **Frontend TypeScript (React 19 + Vite 6 + TypeScript):**
  - Configuração do `tsconfig.json` e `tsconfig.node.json` no frontend.
  - Tipagem estrita dos DTOs e entidades do domínio ispERP em `frontend/src/types/` (`customer.ts`, `contract.ts`, `invoice.ts`, `workorder.ts`, `fiscal.ts`, `helpdesk.ts`, `geocep.ts`).
  - Adição do script `npm run typecheck` para validação estática de tipos.
- [x] **Validação & Testes:**
  - Suíte de testes unitários e de integração do backend 100% aprovada (`MapperTest.java`, `./gradlew test`).
  - Build do Vite e checagem de tipos aprovados sem erros (`npm run typecheck` && `npm run build`).

---

## 🎯 Milestone 17: Excelência REST - RFC 7807, JPA Specifications, Cache ETag & Storage Desacoplado (Concluído)
> **Objetivo:** Elevar o backend ispERP ao mais alto nível de engenharia REST com tratamento padronizado de erros RFC 7807, consultas dinâmicas com Criteria Specifications, respostas ultrarrápidas com ETag (304 Not Modified), persistência de arquivos plugável e seeder de homologação seguro.

- [x] **RFC 7807 (Problem Details for HTTP APIs):**
  - Manipulador global [`GlobalExceptionHandler.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/exception/GlobalExceptionHandler.java) tratando validações (400), recursos não encontrados (404), regras de negócio e erros internos (500).
  - Enriquecimento com mensagens amigáveis (`userMessage`) e lista de campos inválidos (`objects`) para o frontend React/TypeScript.
  - Testes unitários dedicados em [`GlobalExceptionHandlerTest.java`](file:///Users/ruy/Code/ispERP/backend/src/test/java/br/dev/xb/isperp/exception/GlobalExceptionHandlerTest.java).
- [x] **Consultas Dinâmicas com JPA Specifications:**
  - `JpaSpecificationExecutor` habilitado em `InvoiceRepository` e `WorkOrderRepository`.
  - Predicados dinâmicos em [`InvoiceSpecs.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/repository/spec/InvoiceSpecs.java) e [`WorkOrderSpecs.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/repository/spec/WorkOrderSpecs.java).
- [x] **Cache HTTP com ETag (RFC 7234):**
  - Bean [`ShallowEtagHeaderFilter`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/config/WebConfig.java) registrado para cálculo de MD5 e resposta `304 Not Modified` em requisições de leitura.
- [x] **Seeder Seguro de Homologação (`afterMigrate.sql`):**
  - Script [`afterMigrate.sql`](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/devdata/afterMigrate.sql) isolado em `db/devdata` e ativado apenas no profile de desenvolvimento (`application-dev.yml`), garantindo que produção suba 100% limpa.
- [x] **Armazenamento Desacoplado de Arquivos (`FileStorageService`):**
  - Interface [`FileStorageService.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/storage/FileStorageService.java), implementação local [`LocalFileStorageService.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/storage/LocalFileStorageService.java) e endpoint [`FileStorageController.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/controller/FileStorageController.java).
  - Testes unitários em [`FileStorageServiceTest.java`](file:///Users/ruy/Code/ispERP/backend/src/test/java/br/dev/xb/isperp/storage/FileStorageServiceTest.java).
- [x] **Validação & Testes:**
  - Suíte completa de 109+ testes unitários e de integração 100% aprovada (`./gradlew test`).

---

## 🎯 Milestone 18: Armazenamento S3 Universal com SeaweedFS Local & Cloud (AWS S3 / Cloudflare R2) (Concluído)
> **Objetivo:** Implementar o suporte universal a armazenamento compatível com a API S3, fornecendo o SeaweedFS pré-configurado no Docker Compose para desenvolvimento e ISPs locais, além de painel na UI para parametrização dinâmica de provedores em nuvem (AWS S3, Cloudflare R2, MinIO ou outro storage S3).

- [x] **SDK Universal S3 & Driver de Armazenamento:**
  - Adição do SDK oficial AWS Java v2 (`software.amazon.awssdk:s3`).
  - Implementação do driver [`S3FileStorageService.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/storage/S3FileStorageService.java) com auto-criação de buckets, upload seguro com UUIDv7 e streaming de download.
- [x] **Modelo de Dados & Migração Flyway (`V16`):**
  - Tabela `storage_configs` ([`V16__Create_storage_configs_table.sql`](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/migration/V16__Create_storage_configs_table.sql)), entidade [`StorageConfig.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/entity/StorageConfig.java) e repositório.
- [x] **Serviços & Endpoints REST de Gestão & Teste S3:**
  - [`StorageConfigService.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/StorageConfigService.java) com resolução dinâmica de credenciais e teste de conectividade em tempo real com cálculo de latência (`testConnection`).
  - [`StorageConfigController.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/controller/StorageConfigController.java) (`GET /storage/config`, `PUT /storage/config`, `POST /storage/config/test`).
- [x] **Infraestrutura Docker Compose com SeaweedFS:**
  - Container `seaweedfs` (`chrislusf/seaweedfs:latest`) adicionado no [`docker-compose.yml`](file:///Users/ruy/Code/ispERP/docker-compose.yml) e [`docker-compose.prod.yml`](file:///Users/ruy/Code/ispERP/docker-compose.prod.yml) com healthcheck e volume persistente.
- [x] **Frontend React / TypeScript:**
  - Tipagem estrita em [`storage.ts`](file:///Users/ruy/Code/ispERP/frontend/src/types/storage.ts).
  - Tela completa de parametrização [`StorageConfig.jsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Settings/StorageConfig.jsx) com presets inteligentes (SeaweedFS, AWS S3, Cloudflare R2, Custom S3, Local Disk), toggle de secret key, feedback de teste de conexão com badge de latência e botão de salvar.
  - Rota protegida `/settings/storage` e item no menu lateral do [`Sidebar.js`](file:///Users/ruy/Code/ispERP/frontend/src/components/Sidebar/Sidebar.js).
- [x] **Testes Automatizados:**
  - Suíte de testes unitários ([`S3FileStorageServiceTest.java`](file:///Users/ruy/Code/ispERP/backend/src/test/java/br/dev/xb/isperp/storage/S3FileStorageServiceTest.java), [`StorageConfigServiceTest.java`](file:///Users/ruy/Code/ispERP/backend/src/test/java/br/dev/xb/isperp/service/StorageConfigServiceTest.java), [`StorageConfigControllerTest.java`](file:///Users/ruy/Code/ispERP/backend/src/test/java/br/dev/xb/isperp/controller/StorageConfigControllerTest.java)) e build do frontend 100% aprovados.

---

## 🎯 Milestone 19: Assistente Fiscal & Transição de Regime Tributário (Imediata & Agendada com Vigência) (Concluído)
> **Objetivo:** Oferecer flexibilidade para provedores no onboarding fiscal (confirmação não-bloqueante "Vou confirmar com meu contador"), motor de transição de regime tributário (Simples Nacional, Lucro Presumido, Lucro Real) com vigência programada e scheduler diário, mantendo imutabilidade fiscal histórica.

- [x] **Confirmação Fiscal Não-Bloqueante:**
  - Migração Flyway [`V17__add_fiscal_confirmation_to_companies.sql`](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/migration/V17__add_fiscal_confirmation_to_companies.sql) com campos `fiscal_confirmed` e `fiscal_confirmed_at`.
  - Assistente fiscal na UI com botões *"Vou confirmar com meu contador"* (mantém valores sugeridos como pendentes) e *"Confirmar e Salvar Dados Fiscais"*, com badge visual de alerta e status.
- [x] **Motor de Transição de Regime Fiscal (`V18`):**
  - Tabela `fiscal_regime_transitions` com `DEFAULT uuidv7()` ([`V18__create_fiscal_regime_transitions.sql`](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/migration/V18__create_fiscal_regime_transitions.sql)).
  - Entidade [`FiscalRegimeTransition.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/entity/FiscalRegimeTransition.java), DTOs e Mapper MapStruct.
  - Serviço [`FiscalRegimeTransitionService.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/FiscalRegimeTransitionService.java):
    - Se a data de vigência for `<= hoje`, o status é definido como `APPLIED` e aplicado imediatamente na `FiscalCompany`.
    - Se a data for futura (ex: `01/01/2026`), é registrado como `SCHEDULED`.
    - Possibilidade de cancelamento de transições pendentes.
  - Scheduler diário [`FiscalRegimeScheduler.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/scheduler/FiscalRegimeScheduler.java) executado à meia-noite e no startup.
  - Controlador REST [`FiscalRegimeTransitionController.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/controller/FiscalRegimeTransitionController.java).
- [x] **Interface do Usuário (Frontend):**
  - Aba de Transição de Regime Fiscal em [`FiscalDashboard.tsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Financial/FiscalDashboard.tsx) com formulário de agendamento/aplicação imediata e tabela de histórico com cancelamento de agendamentos.
- [x] **Testes Automatizados:**
  - Testes unitários para serviço e controller com 100% de cobertura e aprovação (`./gradlew test`).

---

## 🎯 Milestone 20: Migração Total do Frontend para TypeScript (100% .ts / .tsx) (Concluído)
> **Objetivo:** Converter 100% da base de código do frontend React para TypeScript estrito, garantindo type-safety de ponta a ponta, redução de bugs em tempo de execução e total sincronia com os DTOs do backend.

- [x] **Conversão Completa de Páginas, Componentes, Serviços e Contextos:**
  - 100% dos serviços em `src/services/` migrados para `.ts`.
  - 100% das páginas em `src/pages/` e componentes em `src/components/` migrados para `.tsx`.
  - Contextos (`AuthContext.tsx`) e utilitários (`cpfValidator.ts`) migrados para `.tsx` / `.ts`.
  - Ponto de entrada migrado para `src/App.tsx` e `src/index.tsx` com atualização no `index.html`.
  - Remoção de 100% dos arquivos legados `.js` e `.jsx` em `src/`.
- [x] **Validação & Compilação:**
  - `npm run typecheck` executado com 0 erros.
  - `npm run build` do Vite compilado para produção com sucesso.

---

## 🎯 Milestone 21: IPAM Core (Subsistema Modular, VLSM/Split & Visualização) (Concluído)
> **Objetivo:** Estabelecer o subsistema corporativo de IPAM (IP Address Management) para inventário e documentação de recursos de numeração (ASNs, VRFs, Subnets IPv4/IPv6 e IPs alocados), com motor matemático de alta performance (`com.github.seancfoley:ipaddress`), divisão de sub-redes (Split VLSM), detecção de sobreposição e visualização no frontend.

- [x] **Motor Matemático & Dependências:**
  - Adição de `com.github.seancfoley:ipaddress:5.5.1` ao `build.gradle`.
  - Implementação do [`IpCalculator.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/ipam/IpCalculator.java) suportando IPv4, IPv6, split de sub-redes, primeiro/último host, broadcast, netmask, wildcard mask e overlap detection.
- [x] **Banco de Dados (Flyway `V19`):**
  - Migração [`V19__create_ipam_schema.sql`](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/migration/V19__create_ipam_schema.sql) com tabelas `ipam_asns`, `ipam_vrfs`, `ipam_subnets` e `ipam_ip_addresses` utilizando `UUID DEFAULT uuidv7()`.
- [x] **Backend Java 25:**
  - Entidades JPA (`IpamAsn`, `IpamVrf`, `IpamSubnet`, `IpamIpAddress`).
  - DTOs e Mapper MapStruct [`IpamMapper.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/mapper/IpamMapper.java).
  - Serviços de negócio [`IpamService.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/service/IpamService.java) com métricas de ocupação e busca do próximo IP disponível.
  - Controlador REST [`IpamController.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/controller/IpamController.java).
  - Testes unitários cobrindo cálculos matemáticos, divisão de subnets e endpoints REST (100% Green).
- [x] **Frontend React 19 / TypeScript:**
  - Tipagem estrita em [`src/types/ipam.ts`](file:///Users/ruy/Code/ispERP/frontend/src/types/ipam.ts).
  - Serviço [`src/services/ipamService.ts`](file:///Users/ruy/Code/ispERP/frontend/src/services/ipamService.ts).
  - Painel [`src/pages/Network/IpamManager.tsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Network/IpamManager.tsx) com Árvore de Subnets, Barras de Ocupação, Calculadora/Simulador VLSM, Gestão de ASNs/VRFs e Modal de Split.
  - Registro de rotas e link de navegação no menu Rede.

---

## 🎯 Milestone 22: FreeRADIUS Multi-Vendor, CGNAT Forense & Central do Marco Civil Anti-Fraude (Concluído)
> **Objetivo:** Disponibilizar o container FreeRADIUS no Docker com suporte a autenticação simples (apenas login/senha/velocidade) e avançada (IPAM/Rotas), parsers de CGNAT multi-fabricante e Central de Investigação do Marco Civil com laudos periciais, Hash SHA-256 e QR Code público de autenticidade.

- [x] **Container FreeRADIUS no Docker Compose (`rlm_sql_postgresql`):**
  - Portas UDP expostas: `1812:1812/udp` (Auth), `1813:1813/udp` (Acct), `3799:3799/udp` (CoA / PoD) em `docker-compose.yml` e `docker-compose.prod.yml`.
- [x] **Banco de Dados (Flyway `V20`):**
  - Tabelas `nas`, `radcheck`, `radreply`, `radgroupcheck`, `radgroupreply`, `radusergroup`, `radacct`, `cgnat_mappings` e `marco_civil_reports` com `UUID DEFAULT uuidv7()`.
- [x] **Injeção Dinâmica Multi-Vendor & PoD:**
  - Provisionamento automatizado para MikroTik (`Mikrotik-Rate-Limit`, `Mikrotik-Address-List`), Huawei (`Huawei-Input-Average-Rate`), Juniper (`ERX-Ingress-Policy-Name`), Cisco/RFC (`WISPr-Bandwidth-Max-Down`, `Framed-IP-Address`, `Delegated-IPv6-Prefix`).
  - Desconexão de sessões ativas via comando PoD (Packet of Disconnect - RFC 3576).
- [x] **Parsers CGNAT Multi-Fabricante:**
  - Suporte a MikroTik RouterOS (`/ip firewall nat`), Huawei VRP (`nat address-group`), A10 Networks (`cgnv6`), Cisco IOS-XE (`port-block`) e planilhas CSV/XLS.
- [x] **Central do Marco Civil da Internet (Lei 12.965/2014 & Decreto 8.771/2016):**
  - Cruzamento forense reverso (`IP Público + Porta + Data/Hora` ➔ `CGNAT` ➔ `IP Privado` ➔ `radacct` ➔ `Assinante e Endereço de Instalação`).
  - Emissão de Laudo Pericial Oficial com Hash Criptográfico **SHA-256** e **QR Code**.
  - Página e endpoint públicos de validação anti-fraude (`/public/validar-laudo/:token`).
- [x] **Frontend React 19 / TypeScript:**
  - Telas [`RadiusManager.tsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Network/RadiusManager.tsx), [`CgnatManager.tsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Network/CgnatManager.tsx), [`MarcoCivilSearch.tsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Network/MarcoCivilSearch.tsx) e [`ReportValidation.tsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Public/ReportValidation.tsx).
- [x] **Testes Automatizados:**
  - Suíte de testes unitários para Parsers, Provisioning, Accounting, Disconnect e Marco Civil 100% aprovada (`./gradlew test`).

---

## 🎯 Milestone 23: Sincronização Automática do Ciclo de Vida RADIUS, Auto-Corte & Desbloqueio Instantâneo via PIX com PoD (Concluído)
> **Objetivo:** Estabelecer a orquestração completa entre Contratos, Faturas, Webhook de Pagamento PIX/Boleto, Desbloqueio em Confiança e o servidor FreeRADIUS com envio dinâmico de pacotes PoD (Packet of Disconnect - RFC 3576).

- [x] **Banco de Dados (Flyway `V21`):**
  - Tabelas `radius_policy_configs` e `radius_lifecycle_logs` com `UUID DEFAULT uuidv7()`.
- [x] **Orquestrador de Ciclo de Vida (`RadiusLifecycleService.java`):**
  - Sincronização automática de credenciais PPPoE e atributos de velocidade ao ativar contratos ou provisionar ONUs.
  - Bloqueio com injeção de atributos captive portal (`pg_bloqueados` no MikroTik / domínio captive na Huawei) e disparo imediato de PoD.
  - Desbloqueio com restauração da velocidade do plano e disparo imediato de PoD.
  - Validação de elegibilidade financeira (ausência de outras faturas vencidas além da tolerância).
- [x] **Scheduler de Auto-Corte por Inadimplência (`RadiusLifecycleScheduler.java`):**
  - Rotina agendada diária que analisa faturas vencidas além dos dias de tolerância (ex: 5 dias) e respeita solicitações de Desbloqueio em Confiança ativas.
- [x] **Event Listener Reativo em Tempo Real (`RadiusLifecycleEventListener.java`):**
  - Consumo assíncrono e idempotente de `INVOICE_PAID` (desbloqueio em < 1s via PIX), `CONTRACT_ACCESS_RESTORE_REQUESTED` (Desbloqueio em Confiança) e `ONU_PROVISIONED`.
- [x] **Controlador REST (`RadiusLifecycleController.java`):**
  - Endpoints `/api/radius/lifecycle/summary`, `/policy`, `/logs`, `/action` (bloqueio/desbloqueio manual com PoD) e `/run-autoblock`.
- [x] **Frontend React 19 / TypeScript:**
  - Aba **"Ciclo de Vida & Auto-Corte"** em [`RadiusManager.tsx`](file:///Users/ruy/Code/ispERP/frontend/src/pages/Network/RadiusManager.tsx) com painel KPI, formulário de políticas, botão de execução manual e tabela de auditoria com status PoD.
- [x] **Testes Automatizados:**
  - Suíte de 197 testes unitários e de integração 100% aprovados (`./gradlew test`).



