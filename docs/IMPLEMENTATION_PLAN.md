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

### 2.1. Central de Atendimento & Helpdesk ([frontend/src/pages/Helpdesk](file:///Users/ruy/Code/ispERP/frontend/src/pages/Helpdesk))
- [ ] **Listagem de Chamados:**
  - Exibição de tabela paginada com status (Aberto, Em Atendimento, Pendente Cliente, Resolvido, Cancelado).
  - Badge colorido de prioridade e SLA restante (tempo até expiração).
  - Exibição do **Protocolo Anatel formatado** (`YYYYMMDD-XXXXXX`).
- [ ] **Abertura e Interações do Chamado:**
  - Modal/Formulário para nova abertura com seleção de contrato, categoria do problema e descrição.
  - Timeline de interações (mensagens públicas para o cliente vs. notas internas da equipe técnica).
  - Botão de ação para gerar Ordem de Serviço de Reparo vinculada diretamente ao chamado.

### 2.2. Almoxarifado Multi-Depósito & Custódia ([frontend/src/pages/Inventory](file:///Users/ruy/Code/ispERP/frontend/src/pages/Inventory))
- [ ] **Gestão de Depósitos e Estoques:**
  - Visão geral de múltiplos estoques (Almoxarifado Principal vs. Veículos Técnicos).
  - Transferência de itens em lote com geração de guia de remessa interna.
- [ ] **Controle de Ativos Serializados:**
  - Consulta de ONTs e Roteadores por MAC / Número de Série / Status (Disponível, Em Trânsito, Instalado no Cliente, Avariado).
- [ ] **Termos de Custódia de Ferramentas:**
  - Emissão de Termo de Responsabilidade para ferramentas caras (Máquina de Fusão, OTDR, Power Meter).
  - Histórico de devolução e conferência de avarias com logs imutáveis.

### 2.3. Painel Financeiro & Rebalanceamento Pro-Rata ([frontend/src/pages/Financial](file:///Users/ruy/Code/ispERP/frontend/src/pages/Financial))
- [ ] **Gestão de Faturas & Faturamento Hierárquico:**
  - Visualização de faturas individuais e faturas consolidadas de matriz/filiais.
  - Modal de detalhes da fatura com QR Code dinâmico do Pix Xingubit Pay e linha digitável.
- [ ] **Simulador de Rebalanceamento:**
  - Modal para solicitação de upgrade/downgrade de plano com preview do cálculo proporcional pro-rata antes da confirmação.

### 2.4. Central do Assinante (Portal do Cliente) ([frontend/src/pages/Portal](file:///Users/ruy/Code/ispERP/frontend/src/pages/Portal))
- [ ] **Área Pública / Login por Magic Link:**
  - Autenticação sem senha via link seguro enviado por WhatsApp/E-mail.
- [ ] **Autoatendimento:**
  - Visualização de faturas em aberto com cópia rápida de código Pix.
  - Botão de **Desbloqueio em Confiança (Trust Unblock)** de 48 horas com feedback visual de elegibilidade.

### 2.5. Otimização de Rotas de Campo ([frontend/src/pages/WorkOrders](file:///Users/ruy/Code/ispERP/frontend/src/pages/WorkOrders))
- [ ] **Mapa de Despacho Técnico:**
  - Visualização geográfica das O.S. do dia por técnico.
  - Sugestão da ordem ótima de paradas para redução de quilometragem e combustível.

---

## 3. Critérios de Aceite e Verificação

1. **Compilação e Lint do Frontend:** `npm run build` e `npm test` no diretório `frontend` sem erros.
2. **Integração com Backend:** Todos os endpoints REST consumidos com tratamento adequado de erros e feedbacks visuais (toasts, loading states, validações de formulário).
3. **Responsividade:** Telas operacionais e portal do assinante com layout responsivo para desktop e dispositivos móveis.
4. **Segurança:** Respeito integral às permissões do usuário logado (RBAC) e proteção de endpoints autenticados.
