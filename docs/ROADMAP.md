# Roadmap de Desenvolvimento (ispERP - Milestones)

Este documento estabelece as etapas e marcos de desenvolvimento priorizados para a evolução do **ispERP**.

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

- [x] Tabela `outbox_events` e `processed_events` via Flyway.
- [x] Implementação do `DomainEventPublisher` com persistência na Outbox.
- [x] Worker/Dispatcher assíncrono para entrega resiliente de eventos (`OutboxDispatcher`).
- [x] Mecanismo de Idempotência (`IdempotencyService` / `ProcessedEvent`).
- [x] Testes unitários do publisher, dispatcher e idempotência (100% de aprovação).

---

## 🎯 Milestone 3: Vendas, Clientes, Contratos & Planos (Concluído)
> **Objetivo:** Implementar o fluxo de contratação e ciclo de vida do cliente.

- [x] Módulo de Planos (Download, Upload, Preços, SVA) e endpoints REST.
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

## 🎯 Milestone 5: Faturamento Recorrente, Cobrança & Multi-Gateway de Pagamentos
> **Objetivo:** Geração automatizada de cobranças com suporte a múltiplos gateways simultâneos e integração primária com Xingubit Pay (Pix COB, COBV e NFCom).

- [ ] **Entidades & Modelo de Dados:**
  - `Invoice` e `PaymentTransaction` com UUIDv7.
  - `PaymentGatewayConfig` (armazenando credenciais OAuth, chaves de API, webhook secrets por gateway/empresa).
  - Vínculo hierárquico de gateway: `Contract.gateway_config_id` -> `Plan.gateway_config_id` -> `Company.default_gateway_config_id`.
- [ ] **Interface de Gateway & Roteamento (`PaymentGateway`):**
  - Implementação do `PaymentGatewayRouter` que seleciona dinamicamente o gateway correto.
  - Adaptador Primário: `XingubitPaymentGatewayAdapter` (OAuth 2.0, Pix Imediato COB, Pix com Vencimento COBV e Carnês).
  - Estrutura para adaptadores secundários plugáveis (Asaas, Efí/Gerencianet).
- [ ] **Webhooks Unificados de Pagamento:**
  - Endpoint `/api/webhooks/payments/xingubit` para recebimento de notificações em tempo real.
  - Emissão do `PaymentConfirmedEvent` para acionamento de desbloqueio de rede e notificação de agradecimento.
- [ ] **Consumidor de Ativação:**
  - Consumidor que gera fatura a partir do `ContractActivatedEvent`.
  - Emissão do `InvoiceGeneratedEvent` para o módulo de notificações multicanal.

---

## 🎯 Milestone 6: Notificações Multicanal (WhatsApp & E-mail SMTP Dinâmico)
> **Objetivo:** Envio automático de cobranças e avisos pelo canal de preferência do assinante com provedores de WhatsApp plugáveis e servidor SMTP customizável por empresa.

- [ ] **Consumidor de Eventos:**
  - Consumidor assíncrono para `InvoiceGeneratedEvent`, `InvoiceOverdueEvent` e `PaymentConfirmedEvent`.
- [ ] **Módulo de E-mail (SMTP Customizável):**
  - Cadastro de credenciais SMTP na entidade `Company` (`smtp_host`, `smtp_port`, `smtp_username`, `smtp_password`, `smtp_use_tls`, `smtp_from_email`, `smtp_from_name`).
  - Motor de templates HTML responsivos (Thymeleaf) para avisos de fatura, cobrança PIX com botão de cópia e anexação de boleto PDF.
  - Teste de envio de e-mail integrado nas configurações da empresa.
- [ ] **Módulo de WhatsApp (Strategy / Adapter Pattern):**
  - Interface `WhatsAppProvider` e 3 Adaptadores:
    - `EvolutionApiWhatsAppProvider` (Open-source / Self-hosted via Docker).
    - `ZApiWhatsAppProvider` (SaaS popular em ISPs).
    - `TwilioWhatsAppProvider` / `MetaOfficialWhatsAppProvider` (API Oficial Cloud da Meta).
- [ ] Templates dinâmicos de mensagem de texto com link de pagamento e código PIX Copia-e-Cola.

---

## 🎯 Milestone 7: Provisionamento de Rede Desacoplado (Multi-Driver & Microsserviço)
> **Objetivo:** Integração com a infraestrutura de rede através de drivers plugáveis (SmartOLT, Microsserviço dedicado, MikroTik e Radius) acionados exclusivamente por eventos de domínio.

- [ ] **Interface & Roteador de Rede (`NetworkProvisioningDriver` & `NetworkDriverRouter`):**
  - Definição do contrato unificado (`provisionAccess`, `suspendAccess`, `restoreAccess`, `deprovisionAccess`).
  - Associação de driver por Ponto de Acesso / Concentrador / POP.
- [ ] **Adaptadores de Rede Plugáveis:**
  - `SmartOltDriver`: Integração com API REST do SmartOLT.
  - `DedicatedMicroserviceDriver`: Cliente gRPC / REST para microsserviço de rede externo especializado.
  - `MikroTikRouterOsDriver`: Conexão com concentradores RouterOS (PPPoE / Queues).
  - `RadiusCoAProvisioningDriver`: Pacotes de desconexão e autorização Radius.
  - `NoOpNetworkDriver`: Driver para homologação / ambiente local.
- [ ] **Consumidores de Eventos de Rede:**
  - Consumidor assíncrono para `ContractActivatedEvent` (libera sinal).
  - Consumidor assíncrono para `ContractBlockedEvent` (bloqueia por atraso).
  - Consumidor assíncrono para `PaymentConfirmedEvent` (desbloqueia instantaneamente).
  - Consumidor assíncrono para `ContractCanceledEvent` (desprovisiona).
