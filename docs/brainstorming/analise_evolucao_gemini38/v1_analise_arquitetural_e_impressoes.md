# ispERP: Análise Arquitetural, Diagnóstico & Roadmap de Evolução (Gemini 3.8 Flash)

> **Autor:** Gemini 3.8 Flash (Pair Programming com o Mantenedor)  
> **Data:** 2026-09-02  
> **Tema:** Avaliação Global do Software, O Que Manter, O Que Melhorar e Impressões Gerais  
> **Status:** Brainstorming & Alinhamento Estratégico  

---

## 1. Impressões Gerais sobre o Projeto

O **ispERP** destaca-se como um projeto de engenharia de software de raríssima maturidade no segmento de Telecomunicações (ISPs). A imensa maioria dos sistemas do mercado nesta área carrega débitos técnicos históricos severos (monólitos PHP legados, ausência de testes, acoplamento síncrono que derruba a aplicação durante oscilações de roteadores e bancos sem controle de concorrência).

Construído ao longo de **27 Milestones** e consolidado com **26 migrações Flyway**, o ispERP conseguiu unir a simplicidade operacional de um monólito à robustez e assincronia de microsserviços.

### Principais Destaques Observados:
1. **Stack de Vanguarda Confiável:** A escolha de **Java 25 + Spring Boot 4.1.1** com Virtual Threads, aliada a **PostgreSQL 17** e **React 19 + Vite 6 + TypeScript**, coloca a base tecnológica anos à frente de qualquer concorrente open-source.
2. **Identificadores UUIDv7 em 100% das Tabelas:** A decisão de adotar `UUIDv7` nativo no PostgreSQL 17 e no backend com a biblioteca RFC 9562 (`uuid-creator`) resolveu dois problemas simultaneamente:
   - Ordenação cronológica contígua em índices B-Tree (performance idêntica a inteiros autoincrementais).
   - Eliminação de vulnerabilidades de enumeração de dados (IDOR).
3. **Desacoplamento por Eventos (Transactional Outbox):** A operação de um provedor depende criticamente de serviços lentos ou instáveis (APIs bancárias, SEFAZ, OLTs, FreeRADIUS, mensageria WhatsApp). Ao adotar o padrão Transactional Outbox com garantia de entrega *at-least-once* e consumo idempotente (*exactly-once* via tabela `processed_events`), o sistema nunca trava transações bancárias ou de cadastro enquanto aguarda respostas de hardware.
4. **Respeito aos Padrões de Design (Strategy Everywhere):** Todos os subsistemas com integração externa utilizam o padrão Strategy com Resolvers dinâmicos:
   - Gateways de Pagamento (Xingubit Pay, Asaas, Efí).
   - Gateways Fiscais (NFCom Modelo 62 com Xingubit Pay, Convênio 115/03).
   - Mensageria WhatsApp (Evolution API, Z-API, Twilio).
   - Storage Desacoplado (Local Disk, S3 Universal, SeaweedFS).
   - Drivers de Rede (SmartOLT, Microserviço dedicado, MikroTik, FreeRADIUS PoD).
5. **Telecom & Jurídico Nativos:** Módulos de FreeRADIUS, CGNAT Forense, Central do Marco Civil da Internet (Lei 12.965/2014) com laudo pericial SHA-256 e validação por QR Code, documentação de rede passiva FTTH com normas ABNT NBR 14106 e TIA/EIA-598, e Assinatura Eletrônica Avançada com validação de titularidade bancária via Pix BACEN (R$ 1,00 anti-fraude).
6. **Cultura de Testes Rigorosa (TDD):** A existência de mais de 214 testes unitários e de integração (com Testcontainers rodando PostgreSQL real) garante que qualquer evolução futura ocorra sem risco de regressão.

---

## 2. O Que Eu MANTENHO (Pilares Fundamentais Inegociáveis)

Estes pilares formam a identidade arquitetural do ispERP e **não devem ser alterados**:

| Pilar | Decisão Mantida | Motivação & Benefício |
| :--- | :--- | :--- |
| **Arquitetura** | **Modular Monolith com EDA & Transactional Outbox** | Evita a sobrecarga de gerenciar dezenas de microsserviços em servidores de ISPs pequenos, mantendo fronteiras lógicas perfeitamente desacopladas. |
| **Persistência** | **PostgreSQL 17 com UUIDv7 Nativo** | `id UUID PRIMARY KEY DEFAULT uuidv7()` no banco e `uuid-creator` no Java. Mantém alta taxa de inserção, segurança antienumeração e correlação de eventos em memória. |
| **Interoperabilidade** | **Strategy Pattern em Drivers Externos** | Nenhum driver externo (banco, SEFAZ, OLT, storage) pode acoplar o núcleo do domínio. |
| **Linguagem & Tipagem** | **Java 25 + JSpecify no Backend / TypeScript 100% no Frontend** | Governança estrita de nulos (`@NullMarked`), compilação type-safe de ponta a ponta e zero tolerância a `NullPointerException` e tipos `any`. |
| **Mapeamento DTO** | **MapStruct Mandatório (Zero Reflection)** | Compilação em tempo de build, rápida, tipada e segura (substituindo em definitivo soluções lentas baseadas em reflection como ModelMapper). |
| **Segurança Jurídica** | **Assinatura Pix BACEN & Marco Civil SHA-256** | Validação estrita do titular da conta bancária contra o CPF cadastrado, blindando o provedor contra golpes e gerando provas periciais imutáveis. |

---

## 3. O Que Podemos MELHORAR (Oportunidades de Evolução e Salto de Maturidade)

Com base no estado atual do projeto e nas discussões recentes de brainstorming, os seguintes pontos representam saltos estratégicos de qualidade, performance e operação:

### A. Observabilidade & Rastreabilidade de Eventos (OpenTelemetry Tracing)
- **Cenário Atual:** O sistema possui logs estruturados, mas quando uma venda (`POST /sales`) dispara o evento no Outbox e ele é consumido assincronamente por 4 serviços diferentes em background, o contexto HTTP original é perdido nos logs.
- **Melhoria Proposta:** Injetar cabeçalhos W3C Trace Context (`traceparent`) no JSON de metadados do `outbox_events`. Quando o `OutboxDispatcher` processar o evento, abrir um *Child Span* com o mesmo `trace_id`.
- **Impacto:** O operador do ISP ou a equipe de suporte poderá colar um ID de venda no Grafana/Tempo e ver em uma linha do tempo única: Venda na Web ➡️ Reserva no Estoque ➡️ Criação de Contrato ➡️ O.S. Despachada ➡️ Provisionamento na OLT ➡️ Envio do WhatsApp.

### B. Módulo de Backup Nativo & Disaster Recovery (Zstd + S3 Multipart Streaming)
- **Cenário Atual:** A infraestrutura conta com volumes Docker locais, mas ainda sem um subsistema nativo no backend para agendamento, compressão e upload criptografado multi-destino.
- **Melhoria Proposta (conforme já estruturado no brainstorming):**
  - Implementar o pipeline de streaming em memória: `pg_dump` via TCP ➡️ Buffer 8MB ➡️ Compressão ZStandard (zstd) de 5x a 8x ➡️ Criptografia AES-256 ➡️ S3 Multipart Upload em **1 único arquivo consolidado** (ex: `backup_2026-09-02.sql.zst.enc`).
  - Painel com UX Security-First: escolha entre *Modo Resgate ispERP* e *Modo Zero-Knowledge*, geração de PDF com Chave Mestra e botão de restauração 1-clique.

### C. Rate Limiting & Backpressure em Drivers de Rede (Hardware Protection)
- **Cenário Atual:** As chamadas para OLTs e concentradores MikroTik são executadas conforme os eventos chegam.
- **Melhoria Proposta:** Adicionar um semáforo / limitador de concorrência por equipamento (ex: máximo de 3 comandos simultâneos na mesma OLT) utilizando Java 25 Virtual Threads e filas não-bloqueantes.
- **Impacto:** Protege roteadores e OLTs com CPUs modestas de sofrerem *kernel panic* ou degradação do tráfego dos clientes durante tempestades de alarmes ou varreduras de telemetria.

### D. Modo PWA Offline-First para o Aplicativo dos Técnicos de Campo
- **Cenário Atual:** O portal do técnico (`TechnicianPortal.jsx` / `TechnicianPortal.tsx`) é responsivo e fluido, mas requer conectividade constante à internet para registrar as baixas.
- **Melhoria Proposta:** Adicionar Service Worker e sincronização local via IndexedDB para permitir que o técnico colete a assinatura na tela, capture as coordenadas e registre a baixa mesmo se estiver em área de sombra de sinal celular, sincronizando automaticamente assim que a conexão for reestabelecida.

### E. Pipeline de CI/CD no GitHub Actions (Otimização Extrema de Custos)
- **Cenário Atual:** O diretório `.github/workflows/` ainda não foi formalizado no repositório.
- **Melhoria Proposta:** Criar os workflows seguindo rigorosamente a diretriz dos 5 pilares do projeto:
  1. Concurrency com `cancel-in-progress: true`.
  2. `paths-ignore` para arquivos `.md`, documentação e rascunhos.
  3. Timeouts enxutos (10 min para testes, 15 min para Docker).
  4. Cache nativo de Gradle e npm.
  5. Jobs paralelos com Fail-Fast (testes de backend e frontend antes de disparar o build das imagens Docker).

### F. Rede Neutra & Wholesale B2B (Módulo de Expansão)
- **Cenário Atual:** Estruturado conceitualmente no documento `docs/brainstorming/rede_neutra/v1_visao_geral_e_topologia.md`.
- **Melhoria Proposta:** Modelar a camada de Multi-Tenancy B2B para que provedores parceiros possam consultar viabilidade em tempo real, reservar portas de CTO e receber eventos de O.S. via Webhooks assinados com HMAC-SHA256.

---

## 4. Conclusão & Recomendações de Próximos Passos

O **ispERP** já se encontra em nível de produto de produção (*production-ready*). As bases técnicas são exemplares e não há necessidade de refatorações destrutivas ou mudanças de paradigma. 

A evolução recomendada para as próximas etapas deve seguir a ordem de prioridade operacional:
1. **Consolidar a implementação do Módulo de Backup & Disaster Recovery** (para garantir proteção absoluta aos dados de provedores que já colocarem o sistema em teste ou produção).
2. **Implementar a automação de CI/CD com GitHub Actions** (para proteger a suíte de 214+ testes a cada commit sem custos desnecessários).
3. **Adicionar Observabilidade OpenTelemetry nos eventos da Outbox**.
4. **Evoluir o suporte a Redes Neutras e PWA de campo**.
