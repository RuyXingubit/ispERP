# Roadmap de Desenvolvimento (ispERP - Milestones)

Este documento estabelece as etapas e marcos de desenvolvimento priorizados para a evolução do **ispERP**.

---

## 🎯 Milestone 1: Fundação Técnica, PostgreSQL 17+, UUIDv7 & Testes
> **Objetivo:** Estabelecer a infraestrutura moderna do banco de dados, migração de IDs para UUIDv7 e garantia de qualidade com suíte de testes automatizados (TDD).

- [ ] **Docker & Configuração:**
  - Substituir container MySQL por PostgreSQL 17 (`postgres:17-alpine`) no `docker-compose.yml` e `docker-compose.prod.yml`.
  - Atualizar dependências no `build.gradle` (adicionar `org.postgresql:postgresql` e `com.github.f4b6a3:uuid-creator`, remover drivers MySQL).
  - Atualizar `application.yml` para driver e dialeto PostgreSQL.
- [ ] **Migração de Schema (Flyway):**
  - Reescrever migrações `V1`, `V2`, `V3` em PostgreSQL com tipos nativos `UUID DEFAULT uuidv7()`.
- [ ] **Refatoração de Entidades Atuais:**
  - `User`, `Company`, `SiteSettings`, `Customer` usando `UUID` v7.
- [ ] **Suíte de Testes Automatizados (TDD):**
  - Configurar Testcontainers PostgreSQL para testes de repositório e migração.
  - Criar testes unitários para Services e Controllers com Mockito e JUnit 5.

---

## 🎯 Milestone 2: Motor de Eventos (EDA) & Transactional Outbox
> **Objetivo:** Criar o mecanismo desacoplado e confiável para disparo e consumo de eventos de domínio.

- [ ] Tabela `outbox_events` e `processed_events` via Flyway.
- [ ] Implementação do `DomainEventPublisher` com persistência na Outbox.
- [ ] Worker/Dispatcher assíncrono para entrega resiliente de eventos.
- [ ] Mecanismo de Idempotência e Dead Letter Queue (DLQ).
- [ ] Testes de concorrência e consistência transacional.

---

## 🎯 Milestone 3: Vendas, Clientes, Contratos & Planos
> **Objetivo:** Implementar o fluxo de contratação e ciclo de vida do cliente.

- [ ] Módulo de Planos (Download, Upload, Preços, SVA).
- [ ] Módulo de Vendas (Endpoint para submissão de vendas com emissão de `SaleSubmittedEvent`).
- [ ] Consumidor de Vendas: Cadastro/atualização de `Customer` e criação do `Contract` (Status `PENDING_INSTALLATION`).
- [ ] Telas no Frontend: Formulário de Venda Rápida e Gestão de Contratos.

---

## 🎯 Milestone 4: Ordens de Serviço (O.S.) & Campo
> **Objetivo:** Gestão de instalação técnica e ativação automática pós-visita.

- [ ] Entidade `WorkOrder` com ciclo de vida (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`).
- [ ] Consumidor que gera a O.S. de Instalação a partir do `ContractCreatedEvent`.
- [ ] Tela/Interface para o técnico informar conclusão, MAC da ONU, número de série e sinal dBm.
- [ ] Emissão do `WorkOrderCompletedEvent` e transição do contrato para `ACTIVE`.

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

## 🎯 Milestone 7: Provisionamento de Rede (MikroTik / Radius)
> **Objetivo:** Integração direta com equipamentos de rede para liberação/corte automático.

- [ ] Conector de API MikroTik RouterOS (RouterOS Java API / REST).
- [ ] Consumidor de `ContractActivatedEvent` para criação de PPPoE Secrets e Queues de velocidade.
- [ ] Rotina de bloqueio automático por inadimplência (`ContractBlockedEvent`).
