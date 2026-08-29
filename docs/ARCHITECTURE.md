# Arquitetura e Decisões Técnicas (ADR & Architecture Blueprint)

## 1. Visão Geral da Arquitetura

O **ispERP** adota a arquitetura de **Monólito Modular Orientado a Eventos (Event-Driven Modular Monolith)**. Esse padrão combina a facilidade de deploy e desenvolvimento de um monólito com o baixo acoplamento, alta coesão e assincronia de microsserviços.

```mermaid
graph TD
    UI[Frontend React / MUI] -->|REST API + JWT| API[Spring Boot REST Controllers]
    
    subgraph Core Monolith [ispERP Core Backend]
        API --> Sales[Sales Module]
        API --> Customers[Customer & Contract Module]
        API --> Operations[Work Order / Field Operations]
        API --> Network[Network Provisioning / MikroTik]
        API --> Billing[Billing & Invoicing Module]
        API --> Notify[Notification Dispatcher]
        
        Sales -.->|Events| Outbox[Transactional Outbox / Event Broker]
        Operations -.->|Events| Outbox
        Billing -.->|Events| Outbox
        
        Outbox -->|Async Dispatch| Customers
        Outbox -->|Async Dispatch| Operations
        Outbox -->|Async Dispatch| Network
        Outbox -->|Async Dispatch| Billing
        Outbox -->|Async Dispatch| Notify
    end
    
    subgraph Data Layer [PostgreSQL 17+]
        DB[(PostgreSQL + uuidv7)]
        OutboxTable[(outbox_events)]
    end
    
    Customers --> DB
    Operations --> DB
    Billing --> DB
    Outbox --> OutboxTable
    
    Network -->|API / SSH| MikroTik[MikroTik / OLTs / Radius]
    Billing -->|Webhook / REST| Gateways[PIX / Gateway de Pagamento]
    
    subgraph Notification Adapters [Estratégia Multicanal WhatsApp & E-mail]
        Notify --> Evolution[Evolution API - Open Source]
        Notify --> ZApi[Z-API - SaaS ISP]
        Notify --> Twilio[Twilio / Meta Official Cloud API]
        Notify --> SMTP[Custom SMTP - E-mail com Templates HTML]
    end
```

---

## 2. Decisões Arquiteturais Fundamentais (ADRs)

### ADR 001: Adoção do PostgreSQL 17+ (com suporte nativo a UUIDv7)
- **Contexto:** Suporte nativo à função `uuidv7()` diretamente no SQL sem a necessidade de extensões externas no banco, além de suporte a `JSONB` de alta performance e concorrência avançada (MVCC).
- **Decisão:** Utilizar PostgreSQL 17 (imagem `postgres:17-alpine`).
- **Consequências:**
  - O banco suporta nativamente `id UUID PRIMARY KEY DEFAULT uuidv7()`.
  - Consultas e migrações SQL podem gerar UUIDs v7 nativamente sem dependências C extras.

---

### ADR 002: Identificadores UUIDv7 no Backend e no Banco
- **Contexto:** Na arquitetura orientada a eventos (EDA), precisamos conhecer o identificador da entidade em memória antes da persistência para publicar eventos de domínio com IDs correlacionados (ex: `SaleSubmittedEvent` já precisa carregar o `saleId`).
- **Decisão:** 
  - **No Banco (PostgreSQL 17+):** Colunas `UUID DEFAULT uuidv7()`.
  - **No Backend Java (Java 21):** Biblioteca `com.github.f4b6a3:uuid-creator` (RFC 9562, ultra-leve, zero dependências adicionais).
- **Vantagens:**
  - Ordenação cronológica (índices B-Tree com eficiência idêntica a inteiros sequenciais).
  - Segurança antienumeração (proteção contra ataques de enumeração IDOR).

---

### ADR 003: Arquitetura Orientada a Eventos (EDA) & Transactional Outbox
- **Contexto:** Operações de provedores dependem de serviços externos com latências e taxas de falha variadas (geração de cobrança bancária, provisionamento no MikroTik, envio de WhatsApp). Executar tudo em uma única transação síncrona gera timeout, locks no banco e falhas em cascata.
- **Decisão:** Desacoplar os processos de negócio em **Domain Events** com padrão **Transactional Outbox**.
- **Mecanismo:**
  1. O comando (ex: Registrar Venda) persiste os dados da entidade e insere o evento na tabela `outbox_events` dentro da **mesma transação ACID**.
  2. Um dispatcher assíncrono (Spring `@TransactionalEventListener(phase = AFTER_COMMIT)`) processa o evento e o encaminha aos consumidores correspondentes.
  3. Consumidores são **idempotentes** (gravam o `event_id` na tabela `processed_events` para evitar duplicações).

---

### ADR 004: Notificações Multicanal Plugáveis (WhatsApp Adapters & E-mail SMTP Dinâmico)
- **Contexto:** Provedores de internet precisam se comunicar com o assinante por múltiplos canais. O canal de WhatsApp varia de acordo com o porte do ISP (Evolution API open-source, Z-API ou Twilio/Meta Oficial), enquanto o canal de E-mail exige envio confiável via servidor **SMTP customizado por empresa** com templates HTML responsivos para faturas e lembretes.
- **Decisão:** Criar interfaces desacopladas (`WhatsAppProvider` e `EmailNotificationProvider`) com suporte a:
  1. **WhatsApp (Strategy Pattern):**
     - `EvolutionApiWhatsAppProvider` (Open Source, self-hosted via Docker).
     - `ZApiWhatsAppProvider` (SaaS especializado em ISPs).
     - `TwilioWhatsAppProvider` / `MetaOfficialWhatsAppProvider` (API Oficial Cloud da Meta).
  2. **E-mail (Custom SMTP Provider):**
     - Configuração dinâmica por empresa (`smtp_host`, `smtp_port`, `smtp_username`, `smtp_password`, `smtp_use_tls`, `smtp_from_email`, `smtp_from_name`).
     - Renderização de templates HTML modernos para faturas, avisos de vencimento, comprovantes de pagamento e carnês anexados em PDF.

---

### ADR 005: Segurança, RBAC e Conformidade LGPD
- **Princípio da Segurança em Primeiro Lugar:**
  - **Autenticação:** JWT Stateless com rotação de segredo.
  - **Autorização (RBAC):** Anotações `@PreAuthorize` granulares em nível de serviço/controlador.
  - **Proteção de Dados Sensíveis:** Validação e mascaramento de CPF/CNPJ nos logs. Armazenamento seguro de chaves de API.
  - **Auditoria:** Registro imutável de logs de alteração cadastral e liberação manual de sinal na tabela `audit_logs`.

---

### ADR 006: Estratégia de Testes Automatizados (TDD & Pirâmide de Testes)
- **Regra:** Nenhuma nova funcionalidade ou refatoração é considerada concluída sem testes unitários cobrindo o caminho feliz, cenários de borda e validações de segurança.
- **Ferramentas:**
  - **Unitários:** JUnit 5 + Mockito + AssertJ.
  - **Integração:** `@DataJpaTest` com **Testcontainers PostgreSQL** (testando migrações reais do Flyway e queries nativas).
  - **Frontend:** React Testing Library + Jest.
