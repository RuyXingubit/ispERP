# Plano de Implementação Imediata - Milestone 13: Consolidação do Frontend React 19 & Validação E2E

Este documento detalha o plano de execução para a **Milestone 13** do **ispERP**, servindo de guia passo a passo para conectar, validar e testar as telas e fluxos no frontend com as APIs já ativas no backend.

---

## 1. Visão Geral da Fase Atual

Com a conclusão e validação do backend (Java 25, Spring Boot 4.1.1, PostgreSQL 17 com Flyway `V1` a `V12` e 100% de testes unitários verdes), o foco atual é a **integração das telas no Frontend (React 19 / Vite)** e **validação dos fluxos operacionais ponta a ponta**.

### Módulos do Backend Prontos para Conexão no Frontend:
1. **Almoxarifado & Custódia de Ativos (`V10`):** `WarehouseController`, `AssetCustodyController`.
2. **Helpdesk & Protocolos Anatel (`V12`):** `HelpdeskController`, `NfcomDecisionController`.
3. **Faturamento Hierárquico & Rebalanceamento (`V11`):** `HierarchicalBillingController`, `InvoiceRebalanceController`.
4. **Roteirização de O.S. & BI (`V9`):** `RouteOptimizationController`, `DashboardBiController`.
5. **Central do Assinante (`V8`):** `ClientPortalController`.

---

## 2. Tarefas e Telas a Serem Integradas / Validadas

### 2.1. Central de Atendimento & Helpdesk ([frontend/src/pages/Helpdesk](../frontend/src/pages/Helpdesk))
- [x] **Listagem de Chamados:**
  - Exibição de tabela paginada com status (Aberto, Em Atendimento, Pendente Cliente, Resolvido, Cancelado).
  - Badge colorido de prioridade e SLA restante (tempo até expiração).
  - Exibição do **Protocolo Anatel formatado** (`YYYYMMDD-XXXXXX`) com botão de cópia rápida.
- [x] **Abertura e Interações do Chamado:**
  - Modal/Formulário para nova abertura com seleção de contrato, categoria do problema e descrição.
  - Timeline de interações (mensagens públicas para o cliente vs. notas internas da equipe técnica).
  - Botão de ação para gerar Ordem de Serviço de Reparo vinculada diretamente ao chamado.

### 2.2. Almoxarifado Multi-Depósito & Custódia ([frontend/src/pages/Inventory](../frontend/src/pages/Inventory))
- [x] **Gestão de Depósitos e Estoques:**
  - Visão geral de múltiplos estoques (Almoxarifado Principal vs. Veículos Técnicos).
  - Transferência de itens em lote com geração de guia de remessa interna e duplo handshake.
- [x] **Controle de Ativos Serializados:**
  - Consulta de ONTs e Roteadores por MAC / Número de Série / Status (Disponível, Em Trânsito, Instalado no Cliente, Avariado).
- [x] **Termos de Custódia de Ferramentas:**
  - Emissão de Termo de Responsabilidade com valor de Nota Promissória Executiva para ferramentas caras (Máquina de Fusão, OTDR, Power Meter).
  - Histórico de devolução e conferência de avarias com logs imutáveis e triagem de logística reversa.

### 2.3. Painel Financeiro & Rebalanceamento Pro-Rata ([frontend/src/pages/Financial](../frontend/src/pages/Financial))
- [x] **Gestão de Faturas & Faturamento Hierárquico:**
  - Visualização de faturas individuais e faturas com status em tempo real.
  - Modal de detalhes da fatura com QR Code dinâmico do Pix Xingubit Pay e cópia de Pix Copia e Cola.
- [x] **Simulador de Rebalanceamento:**
  - Simulador e execução de compensação cruzada (pagamento fora de ordem / Dona Maria) quitando fatura anterior e reabrindo futura sem penalidades.
  - Régua de dunning automatizada e desbloqueio em confiança concedido por atendente.

### 2.4. Central do Assinante (Portal do Cliente) ([frontend/src/pages/Portal](../frontend/src/pages/Portal))
- [x] **Área de Autoatendimento & Simulação:**
  - Barra de alternância rápida de clientes para suporte/atendentes.
  - Diagnóstico de sinal óptico (dBm), velocidade contratada e botão de upgrade de plano.
- [x] **Autoatendimento Financeiro & Suporte:**
  - Visualização de faturas em aberto e pagas com link para download de NFCom (Modelo 62).
  - Botão de **Desbloqueio em Confiança (Trust Unblock)** de 48 horas com reativação instantânea do sinal.
  - Abertura de chamados com protocolo regulatório ANATEL direto pelo assinante.

### 2.5. Otimização de Rotas de Campo & BI ([frontend/src/pages/WorkOrders](../frontend/src/pages/WorkOrders))
- [x] **Dashboard de BI & Despacho Técnico:**
  - Métricas consolidadas de ISP (MRR, Churn, Inadimplência, ARPU).
  - Roteirização de O.S. com proximidade geográfica (Haversine/GeoCEP).

### 2.6. Emissão Fiscal NFCom & Convênio ICMS 115/03 ([frontend/src/pages/Fiscal](../frontend/src/pages/Fiscal))
- [x] **Painel de Controle Fiscal (`FiscalDashboard.jsx`):**
  - Listagem de notas fiscais NFCom com status de autorização SEFAZ, chave de acesso e download de XML e DANFE PDF.
  - Formulário de parametrização da empresa emissora com upload seguro de Certificado A1 (`.pfx`).
  - Geração e exportação dos 4 arquivos magnéticos do Convênio ICMS 115/03 em lote compactado (`.zip`).

### 2.7. Portal Mobile do Técnico & GeoCEP ([frontend/src/pages/Technician](../frontend/src/pages/Technician))
- [x] **App de Campo Mobile-First (`TechnicianPortal.jsx`):**
  - Mapa vetorial acelerado por WebGL via MapLibre GL (`GeoCepMapView.jsx`) integrado à API GeoCEP.
  - Crowdsourcing de coordenadas prediais (`POST /v1/contribute`) no momento do atendimento.
  - Coleta de assinatura digital na tela (touch screen) e conclusão de O.S. com ativação imediata de rede.

### 2.8. OpenAPI Swagger UI, MapStruct Mandatório & TypeScript no Frontend
- [x] **OpenAPI & Swagger UI:**
  - Habilitado em `/swagger-ui.html` com suporte a autenticação Bearer JWT e metadados oficiais.
- [x] **MapStruct Mandatório:**
  - Eliminação do `ModelMapper` e introdução do MapStruct 1.6.3 com compilação determinística no Java 25.
  - Regra arquitetural (**ADR 016**) obrigatória para novas entidades e DTOs.
- [x] **TypeScript no Frontend:**
  - Suporte a TypeScript (React 19 + Vite 6 + TypeScript) com `tsconfig.json` e tipos do domínio ispERP em `src/types/`.

---

## 3. Critérios de Aceite e Verificação

1. [x] **Compilação e Lint do Frontend:** `npm run build` e `npm run typecheck` executados com sucesso no Node 24 / Vite 6 sem erros de tipagem.
2. [x] **Integração com Backend:** Todos os endpoints REST consumidos com tratamento adequado de erros e feedbacks visuais (toasts, loading states, validações de formulário).
3. [x] **Responsividade:** Telas operacionais, portal do técnico e portal do assinante com layout responsivo para desktop e dispositivos móveis.
4. [x] **Segurança:** Respeito integral às permissões do usuário logado (RBAC) e proteção de endpoints autenticados com tokens JWT e UUIDv7.
5. [x] **Teste E2E do Ciclo Operacional:** `CompleteOperationalLifecycleE2EIntegrationTest.java` com 100% de aprovação no PostgreSQL 17.
6. [x] **Suíte Geral de Testes:** Mais de 105 testes automatizados aprovados no backend (`./gradlew test`).

