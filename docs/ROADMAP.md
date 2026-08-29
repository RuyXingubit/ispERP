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

## 🚀 Milestone 13: Consolidação do Frontend React 19 & Validação E2E (Em Andamento / Próximo Foco)
> **Objetivo:** Conectar e validar todas as telas do Frontend (Vite 6 / React 19) com as APIs avançadas já disponíveis no backend.

- [ ] **Integração das Telas Operacionais:**
  - [ ] Telas de Almoxarifado Multi-Depósito e Termos de Custódia de Técnicos.
  - [ ] Tela de Chamados / Helpdesk com exibição de protocolo Anatel e SLA.
  - [ ] Painel do Portal do Assinante (2ª via Pix, extrato e botão de desbloqueio em confiança).
  - [ ] Painel de Faturamento com visualização de rebalanceamento e faturas agrupadas.
  - [ ] Mapa de Roteirização de Técnicos de Campo.
- [ ] **Validação E2E dos Fluxos Críticos:**
  - [ ] Fluxo Venda ➔ O.S. ➔ Ativação SmartOLT ➔ Cobrança Pix ➔ Webhook ➔ Desbloqueio.
  - [ ] Fluxo Abertura de Chamado ➔ Despacho de O.S. de Reparo ➔ Baixa e encerramento de SLA.

---

## 🔮 Milestone 14: Emissão Fiscal NFCom (Modelo 62) & Convênio ICMS 115/03 (Planejado)
> **Objetivo:** Automação fiscal completa com geração, assinatura digital (A1) e transmissão de lotes NFCom e arquivos do convênio 115/03.

- [ ] Geração de XML no layout oficial da NFCom (Modelo 62).
- [ ] Módulo de assinatura digital com certificado digital ICP-Brasil (A1 em PKCS#12).
- [ ] Transmissão para a SEFAZ via Web Services com controle de recibos e autorização.
- [ ] Geração de DANFE NFCom em PDF.
- [ ] Exportação de arquivos magnéticos do Convênio ICMS 115/03 (Mestre, Item, Destinatário).

---

## 🔮 Milestone 15: App PWA / Mobile de Campo para Técnicos (Planejado)
> **Objetivo:** Interface offline-first para técnicos de campo realizarem instalações, coletas e assinaturas de termos.

- [ ] PWA offline-first com sincronização periódica.
- [ ] Coleta de coordenadas GPS em tempo real durante o atendimento.
- [ ] Assinatura digital do cliente e do técnico na tela para o Termo de Instalação e Termo de Custódia.
- [ ] Leitor de código de barras/QR Code para captura rápida de MAC de ONU e número de série.
