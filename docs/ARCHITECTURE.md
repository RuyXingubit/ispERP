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
    
    subgraph Network Drivers [Provisionamento de Rede Desacoplado & Multi-Driver]
        Network --> NetRouter[NetworkDriverRouter]
        NetRouter --> SmartOLT[SmartOLT API Driver]
        NetRouter --> Microservice[Microsserviço de Rede Dedicado - gRPC/REST]
        NetRouter --> MikroTik[MikroTik RouterOS API]
        NetRouter --> Radius[FreeRADIUS / CoA Disconnect]
    end
    
    subgraph Payment Gateways [Multi-Gateway Plugável com Roteamento Hierárquico]
        Billing --> Router[PaymentGatewayRouter]
        Router --> XingubitPay[Xingubit Pay - Pix COB/COBV & NFCom]
        Router --> Asaas[Asaas / Gateways Secundários]
        Router --> Efi[Efí / Gerencianet]
    end
    
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
  - **No Backend Java (Java 25):** Biblioteca `com.github.f4b6a3:uuid-creator` (RFC 9562, ultra-leve, zero dependências adicionais).
- **Vantagens:**
  - Ordenação cronológica (índices B-Tree com eficiência idêntica a inteiros sequenciais).
  - Segurança antienumeração (proteção contra ataques de enumeração IDOR).

---

### ADR 003: Arquitetura Orientada a Eventos (EDA) & Transactional Outbox
- **Contexto:** Operações de provedores dependem de serviços externos com latências e taxas de falha variadas (geração de cobrança bancária, provisionamento no MikroTik, envio de WhatsApp). Executar tudo em uma única transação síncrona gera timeout, locks no banco e falhas em cascata.
- **Decisão:** Desacoplar os processos de negócio em **Domain Events** com padrão **Transactional Outbox**.
- **Mecanismo Detalhado do Transactional Outbox:**

```mermaid
sequenceDiagram
    autonumber
    actor Service as Serviço de Negócio (ex: VendaService)
    participant DB as PostgreSQL (Transação ACID)
    participant Outbox as outbox_events
    participant Publisher as DomainEventPublisher
    participant Dispatcher as OutboxDispatcher (Worker)
    participant Consumer as Consumidor / Listener
    participant Idempotency as processed_events

    Service->>DB: 1. Inicia @Transactional
    Service->>DB: 2. Persiste Entidade de Domínio
    Service->>Publisher: 3. publish(domainEvent)
    Publisher->>Outbox: 4. INSERT INTO outbox_events (STATUS='PENDING')
    Service->>DB: 5. COMMIT da Transação
    
    loop A cada X ms (Dispatcher Assíncrono)
        Dispatcher->>Outbox: 6. SELECT WHERE status = 'PENDING'
        Dispatcher->>Consumer: 7. Despacha Evento para Listeners Spring
        Consumer->>Idempotency: 8. Checa / Registra processed_events (event_id, consumer)
        alt Não foi processado ainda
            Consumer->>Consumer: 9. Executa Regra de Negócio (ex: Criar Contrato)
            Consumer->>Idempotency: 10. Grava sucesso
            Dispatcher->>Outbox: 11. UPDATE status = 'PUBLISHED'
        else Já processado
            Dispatcher->>Outbox: 12. UPDATE status = 'PUBLISHED' (Ignora reexecução)
        end
    end
```

- **Garantias:**
  1. **Consistência Atômica:** O evento é salvo na tabela `outbox_events` na mesma transação JDBC da alteração de negócio.
  2. **Entrega Confiável (*At-least-once Delivery*):** Em caso de falha transitória ou queda do servidor, o `OutboxDispatcher` reprocessa com backoff exponencial.
  3. **Consumo Idempotente (*Exactly-once Processing*):** A tabela `processed_events` (`PRIMARY KEY (event_id, consumer_name)`) protege contra processamentos duplicados.

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

---

### ADR 007: Arquitetura de Multi-Gateways de Pagamento com Roteamento Hierárquico
- **Contexto:** Provedores de internet frequentemente operam com múltiplos gateways de pagamento para contingência, divisão de taxas ou acordos corporativos específicos. É necessário poder trocar de gateway sem quebrar faturas emitidas no passado, além de suportar gateways diferentes por plano ou cliente simultaneamente.
- **Decisão:** Implementar o padrão **Strategy + Hierarchical Router Pattern** via interface `PaymentGateway`:
  1. **Hierarquia de Roteamento Dinâmico:**
     - **Nível 1 (Contrato/Cliente):** Se o contrato possuir `gateway_config_id` específico (ex: cliente corporativo negociado), usa este gateway.
     - **Nível 2 (Plano):** Se o plano possuir `gateway_config_id` configurado (ex: plano de alta velocidade em promoção via Pix), usa este gateway.
     - **Nível 3 (Padrão da Empresa):** Se nenhum dos níveis acima estiver definido, usa o gateway padrão ativo da `Company`.
  2. **Primeiro Gateway Suportado: Xingubit Pay (`https://pay.xingubit.com.br/doc`):**
     - Autenticação OAuth 2.0 (`/v1/oauth/token` com `client_id` e `client_secret`).
     - Emissão de Pix Imediato (COB) e Pix com Vencimento (COBV com juros e multa diária).
     - Geração de Carnês Pix parcelados.
     - Webhooks em tempo real (`POST /api/webhooks/payments/xingubit`) para confirmação instantânea de pagamento (`PaymentConfirmedEvent`).
     - Integração com emissão fiscal unificada (NFCom).
  3. **Imutabilidade e Rastreabilidade da Fatura:**
     - Cada registro de `Invoice` armazena o `gateway_type`, `gateway_tx_id` e `gateway_payload` original. Se a empresa alterar o gateway padrão para cobranças futuras, faturas passadas continuam funcionando e recebendo webhooks normalmente.

---

### ADR 008: Provisionamento de Rede Desacoplado & Multi-Driver (SmartOLT, Microsserviço, MikroTik, Radius)
- **Contexto:** Provedores de internet possuem topologias de rede heterogêneas. Pequenos ISPs utilizam concentradores MikroTik locais; médios e grandes provedores utilizam SmartOLT para gerenciar OLTs (Huawei, ZTE, Fiberhome); outros operam com servidores FreeRADIUS dedicados ou microsserviços de rede isolados para isolamento de carga e segurança de borda.
- **Decisão:** Modelar a camada de rede 100% desacoplada orientada a eventos através da interface `NetworkProvisioner` e do roteador `NetworkDriverResolver`:
  1. **Tipos de Drivers Suportados (Plugáveis):**
     - `SmartOltProvisioner`: Integração com a API REST do SmartOLT para autorização de ONUs e alteração de profiles.
     - `ExternalMicroserviceProvisioner`: Comunicação assíncrona (gRPC ou REST) com um microsserviço externo especializado em automação de rede.
     - `RadiusCoAProvisioner`: Envio de pacotes CoA (Change of Authorization / Disconnect) para servidores Radius.
     - `MockNetworkProvisioner`: Driver passivo para homologação e testes unitários/integrados.
  2. **Gatilhos por Eventos de Domínio:**
     - `WorkOrderCompletedEvent` ➡️ `driver.provisionOnu(onu)`
     - `InvoicePaidEvent` ➡️ `driver.unblockSubscriber(subscriber)`
     - `ContractBlockedEvent` (Inadimplência) ➡️ `driver.suspendSubscriber(subscriber)`
  3. **Associação Flexível:** Cada Ponto de Acesso / POP / Concentrador define qual `network_driver_type` e `network_driver_config_id` deve ser utilizado, permitindo ao ERP operar com múltiplos provedores de rede em paralelo.

---

### ADR 009: Almoxarifado Multi-Depósito, Ativos Serializados e Termos de Custódia de Técnicos
- **Contexto:** ISPs enfrentam frequentes perdas de patrimônio, descontrole de insumos entre o depósito central e veículos técnicos, e extravio de ferramentas de alto valor (máquinas de fusão, OTDRs, clivadores).
- **Decisão:** Implementar modelo de dados (`V10`) com controle multi-depósito (`warehouses`), transferências rastreadas (`stock_transfers`), rastreamento unitário por número de série/MAC (`serialized_assets`) e termos formais de custódia com log de auditoria imutável (`tool_custody_agreements`, `custody_logs`).
- **Consequências:** 
  - Todo ativo possui ciclo de vida rastreado: `AVAILABLE`, `IN_TRANSFER`, `IN_CUSTODY`, `INSTALLED`, `DAMAGED`, `RETIRED`.
  - Técnicos assumem formalmente a responsabilidade por ferramentas caras mediante termo de custódia assinado.

---

### ADR 010: Faturamento Hierárquico Multi-Empresa e Rebalanceamento Pro-Rata
- **Contexto:** Clientes corporativos com matriz e múltiplas filiais exigem faturamento centralizado (uma única fatura consolidada cobrindo múltiplos contratos). Além disso, trocas de plano no meio do ciclo de faturamento geram inconsistências contábeis se o cálculo pro-rata não for automatizado.
- **Decisão:** Implementar entidades e serviços de faturamento hierárquico (`V11`, `HierarchicalBillingService`) e rebalanceamento inteligente (`InvoiceRebalanceService`).
- **Consequências:**
  - Suporte a faturas consolidadas ou segregadas por filial com rateio proporcional.
  - Cálculo exato de dias utilizados no plano anterior vs. novo plano, gerando faturas complementares ou abatimentos no ciclo subsequente.

---

### ADR 011: Central de Atendimento (Helpdesk) com Protocolos Regulatórios Anatel e Classificação Fiscal NFCom
- **Contexto:** Provedores de internet no Brasil são regulados pela Anatel (exigência de numeração única de protocolo de atendimento) e pela SEFAZ (obrigatoriedade da NFCom Modelo 62 com segregação precisa entre serviços de telecomunicação SCM e serviços de valor agregado SVA).
- **Decisão:** Implementar módulo de Helpdesk (`V12`, `HelpdeskService`) com gerador de protocolos Anatel (`AnatelProtocolGenerator`), matriz de cálculo de SLA e motor de decisão fiscal (`NfcomDecisionService`).
- **Consequências:**
  - Todo chamado gera protocolo único no formato `YYYYMMDD-XXXXXX` rastreável em todas as interações.
  - Cada item cobrado é classificado automaticamente para tributação correta (ICMS sobre SCM / ISS sobre SVA).

---

### ADR 012: Governança Estrita de Tipagem e Null-Safety com JSpecify no Java 25
- **Contexto:** Falhas por `NullPointerException` (NPE) em tempo de execução são críticas para sistemas de missão crítica como ERPs de telecomunicações. Anotações legadas (@NonNullApi do Spring) causavam conflitos com Lombok e IDEs modernas.
- **Decisão:** Adotar a especificação padrão da indústria **JSpecify** (`org.jspecify.annotations:jspecify`), anotando todos os pacotes com `@NullMarked` em `package-info.java` e sinalizando explicitamente campos e retornos opcionais com `@Nullable`.
- **Consequências:**
  - Análise estática em tempo de compilação sem falsos-positivos com Lombok.
  - Código 100% autodocumentado quanto a contratos de nulidade.

---

### ADR 013: Arquitetura Multi-Gateway Fiscal (Strategy Pattern), NFCom Modelo 62 e Convênio ICMS 115/03
- **Contexto:** ISPs no Brasil precisam emitir a Nota Fiscal Fatura de Serviços de Comunicação Eletrônica (NFCom Modelo 62) junto à SEFAZ autorizadora ou gerar arquivos magnéticos legados do Convênio ICMS 115/03. A solução não pode ficar acoplada a um único emissor ou depender de soluções desktop lentas.
- **Decisão:** Implementar a camada fiscal desacoplada com Strategy Pattern via interface `FiscalGateway`, resolução dinâmica via `FiscalGatewayResolver` e suporte nativo aos drivers `XingubitPayFiscalDriver` (driver de nuvem oficial com OAuth2 e upload de Certificado A1 `.pfx`) e `MockFiscalDriver` (para testes/CI), além de um gerador nativo dos arquivos magnéticos do Convênio 115/03 (`ConvenioIcms115Service`) com validação de hashes MD5 cruzados.
- **Consequências:**
  - Emissão síncrona/assíncrona de NFCom com armazenamento de chave de acesso SEFAZ, XML e link para DANFE em PDF.
  - Parametrização individual por empresa (ambiente de homologação/produção, série fiscal e certificado A1).
  - Exportação em lote de arquivos do Convênio 115/03 (Mestre, Item, Destinatário e Controle) em `.zip` para conformidade estadual.

---

### ADR 014: App Mobile do Técnico com Mapas Vetoriais GeoCEP (MapLibre GL), Crowdsourcing Predial e Assinatura Touch
- **Contexto:** Técnicos em campo operam através de dispositivos móveis em condições de rede variáveis e necessitam de navegação precisa até o endereço do cliente, conferência de porta de atendimento, coleta de coordenadas reais e assinatura do cliente sem formulários em papel.
- **Decisão:** Criar interface Web Mobile-First (`TechnicianPortal.jsx`) com mapas vetoriais acelerados por WebGL via MapLibre GL (`GeoCepMapView.jsx`) consumindo estilos e tiles da API GeoCEP (`geocep.api.br`). Implementar fluxo de crowdsourcing predial (`POST /v1/contribute` via `GeoCepClient`) e canvas de assinatura digital touch armazenada em Base64 na O.S.
- **Consequências:**
  - Carregamento de mapas a 60fps diretamente no navegador mobile sem custos com APIs proprietárias.
  - Alimentação contínua da base GeoCEP com coordenadas submétricas coletadas pelo GPS do técnico no momento da instalação.
  - Eliminação de papel com comprovantes de instalação assinados digitalmente e ativação de rede em tempo real no término da O.S.

---

### ADR 015: Padronização de E-mails Transacionais com Apache FreeMarker e Fechamento Contábil Mensal Automatizado
- **Contexto:** Provedores necessitam de comunicação transacional visualmente consistente (boletos, avisos de cobrança, códigos 2FA, Magic Links) e envio mensal recorrente de faturas e relatórios fiscais para as assessorias contábeis sem intervenção humana manual.
- **Decisão:** Adotar a engine de templates Apache FreeMarker (`.ftl`) gerenciada pelo `EmailNotificationService` e implementar job de despacho contábil automatizado (`MonthlyAccountingDispatchService`) com geração de arquivos compactados `.zip` anexados diretamente ao e-mail da contabilidade cadastrada.
- **Consequências:**
  - Separação clara entre a lógica de negócio do backend e o design/responsividade dos e-mails em HTML.
  - Envio automático no primeiro dia útil do mês contendo todas as NFCom emitidas e relatórios financeiros do mês anterior para a contabilidade do provedor.

---

### ADR 016: Adoção Mandatória do MapStruct para Mapeamento DTO ↔ Entidade e Descontinuação do ModelMapper
- **Contexto:** O uso de bibliotecas baseadas em reflexão em tempo de execução (`ModelMapper`) introduz lentidão e risco de inconsistências silenciosas quando propriedades são renomeadas. O Java 25 exige compilação estrita e determinística.
- **Decisão:** Adotar o **MapStruct 1.6.3** como padrão **mandatório e exclusivo** para todas as conversões entre Entidades JPA e DTOs, descontinuando o ModelMapper.
- **Regra Arquitetural Obrigatória:**
  > **SEMPRE** que uma nova entidade, DTO ou serviço for criado, o mapeamento **DEVE** ser implementado através de uma interface `@Mapper(componentModel = "spring")` gerenciada pelo Spring DI e processada pelo `mapstruct-processor` na compilação.
- **Consequências:**
  - Conversões de alto desempenho geradas em código Java puro durante o `./gradlew compileJava`.
  - Falha imediata de compilação caso algum campo obrigatório não seja mapeado corretamente.
  - Total compatibilidade com GraalVM e AOT.

---

### ADR 017: Governança de Tipagem com TypeScript no Frontend e Contratos OpenAPI
- **Contexto:** O crescimento da complexidade do ERP exige que o frontend seja orientado a objetos e fortemente tipado, aproveitando a capacidade computacional dos desktops corporativos modernos e evitando que o backend seja sobrecarregado com validações triviais que podem falhar antes na UI.
- **Decisão:** Migrar a camada de frontend para **TypeScript (React 19 + Vite 6 + TypeScript)** e expor a documentação dos serviços via **Springdoc OpenAPI (`/swagger-ui.html` e `/v3/api-docs`)**.
- **Consequências:**
  - Tipagem estrita de DTOs, contratos de API e modelos de domínio no frontend.
  - Eliminação de erros de digitação e propriedades inexistentes em tempo de desenvolvimento.
  - Sincronização automatizada dos contratos entre Java e React.

---

### ADR 018: Padronização Global de Erros de API com RFC 7807 (Problem Details)
- **Contexto:** Respostas de erro sem padrão estruturado geram inconsistências para o frontend e dificultam a identificação imediata de campos inválidos em formulários complexos.
- **Decisão:** Implementar a especificação padrão da IETF **RFC 7807 (Problem Details for HTTP APIs)** através do `GlobalExceptionHandler` estendendo `ResponseEntityExceptionHandler`.
- **Consequências:**
  - Todas as respostas de erro (400, 404, 409, 500) retornam schema unificado com `type`, `title`, `status`, `detail`, `userMessage`, `timestamp` e o array `objects` contendo os campos violados.
  - O frontend React/TypeScript exibe feedbacks visuais precisos abaixo dos inputs correspondentes.

---

### ADR 019: Consultas Dinâmicas com JPA Specifications, Cache HTTP ETag e Storage Desacoplado
- **Contexto:** Listagens do ERP (faturas, ordens de serviço, clientes) exigem filtros multidimensionais que sobrecarregam repositórios clássicos. Além disso, leituras estáticas e uploads de fotos de campo necessitam de otimização de banda e persistência plugável.
- **Decisão:** Adotar **JPA Specifications (`CriteriaBuilder`)** para filtros compostos, **`ShallowEtagHeaderFilter`** para cache HTTP `304 Not Modified`, e interface **`FileStorageService`** para persistência de fotos e documentos.
- **Consequências:**
  - Filtros dinâmicos limpos e reutilizáveis sem duplicação de métodos no repositório.
  - Respostas instantâneas em < 5ms para consultas repetidas com validação de ETag.
  - Armazenamento desacoplado pronto para transição entre disco local e cloud storage (S3/MinIO).

---

### ADR 020: Armazenamento Universal S3 com SeaweedFS Local e Drivers Cloud (AWS S3 / Cloudflare R2)
- **Contexto:** Com a mudança de licenciamento do MinIO para AGPLv3 e a descontinuação da distribuição comunitária simples, os provedores necessitam de uma solução S3 on-premise moderna, leve e sem atritos de licença (Apache 2.0), além da flexibilidade de usar nuvens públicas (AWS S3, Cloudflare R2, Wasabi) sem alterar o código da aplicação.
- **Decisão:** Adotar a biblioteca oficial **AWS SDK v2 (`software.amazon.awssdk:s3`)** com o driver `S3FileStorageService` conectado por padrão ao **SeaweedFS (`chrislusf/seaweedfs`)** no Docker Compose, suportando parametrização dinâmica via UI e banco de dados (`StorageConfig`).
- **Consequências:**
  - Ambientes de desenvolvimento e provedores on-premise sobem o SeaweedFS em < 1s consumindo ~30MB de RAM.
  - Provedores que optarem por AWS S3 ou Cloudflare R2 apenas configuram as credenciais pela UI administrativa (`/settings/storage`), com teste de latência em tempo real.
  - O bucket é criado automaticamente se não existir e os identificadores seguem o padrão UUIDv7.

---

### ADR 021: Transições de Regime Tributário com Vigência Programada e Imutabilidade Fiscal
- **Contexto:** Provedores de internet frequentemente alteram sua opção fiscal (Simples Nacional, Lucro Presumido, Lucro Real) na virada do ano fiscal ou durante o crescimento do negócio. Alterações diretas no cadastro da empresa sem versionamento quebram a coerência de notas fiscais passadas (NFCom Modelo 62) e não permitem agendar a vigência com antecedência para início automático em 1º de Janeiro.
- **Decisão:** Criar a tabela `fiscal_regime_transitions` com identificadores UUIDv7 nativos e o serviço `FiscalRegimeTransitionService` com status `SCHEDULED`, `APPLIED` e `CANCELLED`.
- **Regras Arquiteturais:**
  1. Se a `effectiveDate` for menor ou igual à data corrente, o regime é aplicado imediatamente (`APPLIED`) atualizando a `FiscalCompany`.
  2. Se a `effectiveDate` for futura, a transição é agendada (`SCHEDULED`) e mantida em espera.
  3. O scheduler diário `FiscalRegimeScheduler` roda à meia-noite e no startup do sistema verificando e aplicando transições agendadas cuja vigência tenha chegado.
  4. As notas fiscais emitidas no passado (`nfcom_records`) mantêm seus valores de alíquota, base de cálculo e XML assinados de forma estritamente imutável.

---

### ADR 022: Migração Total e Padronização Mandatória de TypeScript no Frontend
- **Contexto:** A manutenção mista de arquivos `.jsx`/`.js` e `.tsx`/`.ts` trazia duplicidade de arquivos, falta de autocompletion em componentes legados e risco de divergência com os DTOs do backend Java 25.
- **Decisão:** Realizar a migração completa (100%) da base de código do frontend para TypeScript (`.ts` / `.tsx`), removendo todos os arquivos `.js` e `.jsx` residuais de `src/`.
- **Regra Mandatória:**
  > Novos componentes, serviços, hooks e páginas do frontend ispERP **DEVEM** ser desenvolvidos exclusivamente em `.ts` ou `.tsx`, integrando os tipos de domínio em `src/types/` e com validação contínua através do script `npm run typecheck`.
- **Consequências:**
  - 100% de type-safety no frontend React 19.
  - Zero erros de compilação no Vite e TypeScript compiler.
  - Documentação viva no próprio código de cada tela e serviço.

---

### ADR 023: Subsistema IPAM Hierárquico e Aritmética de Rede (IPv4 / IPv6)
- **Contexto:** Provedores de internet necessitam documentar seus blocos de numeração (ASNs, VRFs, subnets públicas e privadas, CGNAT e prefixos IPv6), permitindo cálculos precisos de gateway/broadcast/hosts, divisão de blocos (Split VLSM) e alocação opcional para contratos ou equipamentos de rede.
- **Decisão:** Criar um subsistema corporativo de IPAM desacoplado e modular, utilizando a biblioteca padrão da indústria `com.github.seancfoley:ipaddress:5.5.1` encapsulada no serviço [`IpCalculator.java`](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/ipam/IpCalculator.java).
- **Diretrizes de Implementação:**
  1. **Schema PostgreSQL com UUIDv7:** Tabelas `ipam_asns`, `ipam_vrfs`, `ipam_subnets` e `ipam_ip_addresses` via Flyway `V19`.
  2. **Independência Operacional:** O IPAM é opcional para o funcionamento básico de autenticação RADIUS, mas disponível como ferramenta de primeira classe para ISPs organizados.
  3. **Segurança Matemática:** Todo cálculo de split, contenção, detecção de overlap e máscara de sub-rede é executado via motor bitwise sem risco de estouro de memória ou corner-cases em IPv6.
- **Consequências:**
  - Capacidade de dividir blocos (/24 em /28, /32 em /40) com pré-visualização instantânea e persistência em lote.
  - Suporte completo a RFC 3021 (/31) e RFC 5952 (IPv6 canônico).
  - Base pronta para consumo nativo pelo FreeRADIUS (Milestone 22).

---

### ADR 024: FreeRADIUS Multi-Vendor, CGNAT Forense & Central do Marco Civil Anti-Fraude
- **Contexto:** Provedores de internet operam com infraestruturas heterogêneas de BNGs (MikroTik RouterOS, Huawei NE40/ME60, Juniper MX, Accel-PPP, Cisco ASR) e realizam CGNAT para economia de endereços IPv4 públicos. Paralelamente, a Lei nº 12.965/2014 (Marco Civil da Internet) exige a guarda e pronta disponibilização sob ordem judicial de registros de conexão (IP, porta lógica de origem, data/hora e identificação inequívoca do titular), com necessidade de proteção contra falsificação de laudos periciais entregues a autoridades policiais ou judiciais.
- **Decisão:** Integrar nativamente o FreeRADIUS com PostgreSQL 17+, fornecer parsers automatizados de regras CGNAT multi-fabricante e uma Central de Investigação Forense com emissão de laudos oficiais assinados criptograficamente.
- **Diretrizes de Implementação:**
  1. **Schema FreeRADIUS + CGNAT com UUIDv7:** Tabelas `nas`, `radcheck`, `radreply`, `radacct`, `cgnat_mappings` e `marco_civil_reports` criadas na migração Flyway `V20`.
  2. **Injeção Dinâmica Multi-Vendor:** Suporte a perfis de rate-limit nativos por fabricante (MikroTik, Huawei, Juniper, Cisco/RFC) e pool de bloqueio automático por inadimplência.
  3. **Desconexão PoD (Packet of Disconnect - RFC 3576):** Envio de pacotes CoA/PoD UDP na porta 3799 para derrubar conexões de clientes em tempo real.
  4. **Cruzamento Forense Reverso:** Algoritmo determinístico que correlaciona `IP Público + Porta + Timestamp` ➔ `Mapeamento CGNAT` ➔ `IP Privado` ➔ `radacct` ➔ `Assinante e Endereço de Instalação`.
  5. **Garantia de Autenticidade Anti-Fraude:** Emissão de laudos periciais com Hash Criptográfico SHA-256 e QR Code com link público `/public/validar-laudo/:token`, permitindo à autoridade policial/judicial atestar que o laudo impresso confere com os registros invioláveis do provedor.
- **Consequências:**
  - Plena conformidade legal com o Marco Civil e LGPD.
  - Agilidade imediata no atendimento a ofícios e inquéritos policiais.
  - Blindagem do ISP contra retaliação e fraudes documentais.

---

### ADR 025: Sincronização Reativa do Ciclo de Vida RADIUS, Auto-Corte & Desbloqueio Instantâneo com PoD
- **Contexto:** A gestão de bloqueio e liberação de clientes inadimplentes em ISPs precisa ser 100% automatizada para evitar perda de receita e sobrecarga no atendimento. Quando um assinante paga via PIX ou solicita Desbloqueio em Confiança, o acesso à internet deve ser restabelecido em menos de 1 segundo, sem necessidade de reboot do roteador pelo cliente ou intervenção humana do suporte.
- **Decisão:** Desenvolver um motor reativo e orientado a eventos (`RadiusLifecycleEventListener`, `RadiusLifecycleService` e `RadiusLifecycleScheduler`) integrado ao FreeRADIUS, emitindo pacotes PoD (Packet of Disconnect - RFC 3576) na porta UDP 3799 para derrubada e reconexão imediata da sessão PPPoE nos concentradores BNG.
- **Diretrizes de Implementação:**
  1. **Schema de Políticas & Auditoria com UUIDv7:** Tabelas `radius_policy_configs` e `radius_lifecycle_logs` criadas na migração Flyway `V21`.
  2. **Auto-Corte Inteligente:** Scheduler diário que avalia faturas vencidas além da carência configurada (ex: 5 dias), ignorando automaticamente clientes com *Desbloqueio em Confiança* ativo.
  3. **Desbloqueio em Tempo Real (< 1s):** Consumo assíncrono do evento `INVOICE_PAID` (Webhook PIX / Baixa de Boleto) com validação de elegibilidade financeira global e emissão instantânea de pacote PoD para restaurar a velocidade total contratada.
  4. **Auditoria Transparente:** Registro imutável de todas as ações de corte, desbloqueio e resposta dos concentradores BNG na tabela `radius_lifecycle_logs`.
---

### ADR 026: Documentação de Rede FTTH, Gestão de Fibras ABNT/TIA-598, Diagramas Unifilares e Viabilidade de Vendas
- **Contexto:** A operação de ISPs requer documentação precisa de ativos passivos de rede óptica (cabos ópticos, caixas de emenda/CEO, divisores/splitters, caixas de atendimento/CTO e postes). Para manutenções de campo e fusões sem erro humano, o sistema deve respeitar os códigos de cores oficiais das fibras ópticas conforme as normas nacionais (ABNT NBR 14106) e internacionais (TIA/EIA-598). Além disso, o time de vendas e suporte necessita de consulta instantânea de viabilidade técnica por geolocalização e cálculo do orçamento de potência óptica (Power Budget).
- **Decisão:** Desenvolver um módulo completo de topologia FTTH no backend Java 25 / PostgreSQL 17+ com motor de cores dinâmico, rastreamento óptico com atenuação acumulada teórica e interface gráfica interativa em React 19 / TypeScript com diagrama unifilar vetorial.
- **Diretrizes de Implementação:**
  1. **Schema FTTH com UUIDv7:** Tabelas `ftth_pops`, `ftth_poles`, `ftth_cables`, `ftth_closures`, `ftth_splitters`, `ftth_ctos`, `ftth_cto_ports` e `ftth_fusions` criadas na migração Flyway `V23`.
  2. **Motor de Cores Multi-Norma (`FtthColorService.java`):**
     - **ABNT NBR 14106 / Telebrás:** 1: Verde, 2: Amarelo, 3: Branco, 4: Azul, 5: Vermelho, 6: Violeta, 7: Marrom, 8: Rosa, 9: Preto, 10: Cinza, 11: Laranja, 12: Aqua.
     - **TIA/EIA-598:** 1: Azul, 2: Laranja, 3: Verde, 4: Marrom, 5: Cinza, 6: Branco, 7: Vermelho, 8: Preto, 9: Amarelo, 10: Violeta, 11: Rosa, 12: Aqua.
     - Suporte a agrupamento em tubos *loose* para cabos de 6, 12, 24, 36, 72 e 144 fibras.
  3. **Diagrama Unifilar Interativo em 2 Cliques (`FtthFusionDiagram.tsx`):**
     - Visualização das fibras de entrada com código de cores e identificação de tubos.
     - Montagem e exclusão de fusões fibra-a-fibra e divisores ópticos balanceados (PLC 1:2 até 1:64) ou desbalanceados (FBT 95/05 até 50/50).
  4. **Painel Frontal de CTOs & Rastreamento Óptico (`FtthCtoDetail.tsx` & `FtthLightPathService.java`):**
     - Visão realista de 8 ou 16 adaptadores SC-APC com identificação de ocupação por cliente/ONU.
     - Rastreamento reverso do caminho da luz calculando a atenuação teórica (Power Budget) e potência óptica Rx estimada na ONT.
---

### ADR 027: Monitoramento Ativo de OLTs, Alarmística de LOS e Correlação de Rompimento de Fibra FTTH
- **Contexto:** Em operações de ISPs com milhares de assinantes ópticos, falhas físicas na rede externa (rompimento de cabos por caminhões, obras ou vandalismo) ou quedas de energia elétrica em bairros precisam ser diagnosticadas instantaneamente. O envio de consultas SNMP frequentes e individuais para dezenas de milhares de ONUs pode sobrecarregar a CPU das controladoras das OLTs e travar o plano de gerência da rede.
- **Decisão:** Desenvolver um Centro de Operações de Rede (NOC) com arquitetura escalonada anti-tempestade em 3 camadas e um motor de correlação analítica topológica que cruza telemetria de OLTs com a rede passiva FTTH.
- **Diretrizes de Implementação:**
  1. **Schema de Telemetria & Incidentes com UUIDv7:** Tabelas `olt_pon_ports`, `ftth_incidents` e `onu_telemetry_records` criadas na migração Flyway `V24`.
  2. **Arquitetura Escalonada Anti-Tempestade:**
     - *Camada 1 (Push Reativo):* Recepção instantânea de SNMP Traps / Syslog de alarmes de *LOS* e *Dying Gasp* com custo zero de CPU em regime normal.
     - *Camada 2 (Summary Polling Leve a cada 2 min):* Consulta leve de contadores agregados por porta PON (< 0.1% CPU na OLT).
     - *Camada 3 (Leitura em Lotes com Rate-Limiting):* Coleta detalhada de dBm em blocos com Java 25 Virtual Threads e semáforo limitador de concorrência por OLT.
  3. **Motor Analítico de Correlação Topológica (`FtthCorrelationEngine.java`):**
     - Diferenciação precisa de *Dying Gasp* (queda de energia no bairro) vs *LOS* (rompimento de fibra).
     - Classificação do raio de impacto: Rompimento Troncal (PON inteira) vs Rompimento de Distribuição (CTO isolada).
     - Cálculo da coordenada geográfica central estimada do ponto de corte.
     - Abertura e despacho de Ordem de Serviço emergencial para a equipe de campo.
- **Consequências:**
  - Redução do MTTR (Mean Time to Repair) de incidentes graves de rompimento de fibra de horas para minutos.
  - Eliminação de falsos despachos de técnicos de fusão durante apagões elétricos da concessionária de energia.
  - Estabilidade e segurança total das controladoras de OLTs mesmo em redes com mais de 100.000 clientes.

