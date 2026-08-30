# PRD - Documento de Requisitos de Produto (ispERP)

## 1. Visão Geral e Propósito
O **ispERP** é uma plataforma moderna, aberta e altamente escalável para gestão integral de **Provedores de Serviços de Internet (ISPs)**. O sistema automatiza desde a prospecção/venda, cadastro de clientes, geração de contratos, emissão de ordens de serviço (O.S.), provisionamento em equipamentos de rede (MikroTik, OLTs, Radius), até o faturamento recorrente (PIX/Boleto), comunicação multicanal e compliance fiscal.

---

## 2. Objetivos de Negócio
- **Automação Ponta a Ponta ("Zero-Touch Onboarding"):** Uma venda aprovada dispara automaticamente o cadastro, contrato, O.S., provisionamento de rede e primeira fatura via eventos.
- **Redução da Inadimplência:** Automação de cobranças preventivas (WhatsApp/E-mail) e aplicação de regras graduais de corte/redução de velocidade via integração de rede.
- **Segurança e Privacidade (LGPD):** Identificadores UUIDv7, controle de acesso baseado em papéis (RBAC) e auditoria de ações críticas.
- **Alta Resiliência e Escalabilidade:** Arquitetura orientada a eventos (EDA) desacoplada, garantindo que falhas em serviços externos (gateways, OLTs) não travem o fluxo operacional.

---

## 3. Personas & Perfis de Usuário (RBAC)
1. **Administrador / Diretor:** Acesso total a relatórios, configurações fiscais, financeiras e parametrização do sistema.
2. **Vendedor / Comercial:** Acesso ao módulo de vendas, cadastro rápido de leads, simulação de planos e emissão de propostas.
3. **Técnico de Campo (O.S.):** Visualização de ordens de serviço atribuídas, check-in no local, validação de sinal de fibra (dBm), vinculação de MAC da ONU e conclusão de instalação.
4. **Operador de Suporte / NOC:** Monitoramento de conexões ativas, histórico de sinal, reset de portas e diagnóstico de quedas.
5. **Analista Financeiro:** Gestão de conciliação bancária, relatórios de inadimplência, estornos e notas fiscais.
6. **Cliente Final (Portal do Assinante):** Consulta de faturas, emissão de 2ª via PIX/Boleto, auto-desbloqueio por confiança e abertura de chamados.

---

## 4. Módulos Funcionais e Regras de Negócio

### 4.1. Módulo de Vendas & Onboarding Rápido
- **Entrada de Dados:** Nome/Razão Social, CPF/CNPJ, Telefone/WhatsApp, E-mail, Endereço de Instalação com geolocalização e Plano selecionado.
- **Gatilho de Evento:** Ao submeter a venda com sucesso, o sistema emite o evento `SaleSubmittedEvent`.
- **Canais de Envio:** Definição das preferências de comunicação do cliente (WhatsApp, E-mail, SMS).

### 4.2. Módulo de Clientes & Contratos
- Um mesmo cliente (`Customer`) pode ter múltiplos contratos (`Contract`) associados a diferentes endereços/pontos de acesso.
- **Estados do Contrato:**
  - `DRAFT`: Proposta em elaboração.
  - `PENDING_INSTALLATION`: Aguardando conclusão da O.S. de instalação.
  - `ACTIVE`: Instalação concluída com sucesso, sinal liberado e faturamento ativo.
  - `BLOCKED_OVERDUE`: Suspenso temporariamente por inadimplência.
  - `SUSPENDED_VOLUNTARY`: Suspensão temporária a pedido do cliente.
  - `CANCELED`: Contrato rescindido.

### 4.3. Módulo de Planos & Serviços
- Cadastro de velocidades de Download/Upload (ex: 500 Mbps / 250 Mbps).
- Atributos de QoS / Queues (MikroTik profile, Radius Group, VLAN ID).
- Serviços de Valor Adicionado (SVA) embutidos (Paramount+, Deezer, antivírus, etc.).
- Preço base, desconto de pontualidade e regras de fidelidade contratual (12 meses).

### 4.4. Módulo de Ordens de Serviço (O.S.) & Campo
- **Tipos de O.S.:**
  - `INSTALLATION`: Instalação nova vinculada a uma venda.
  - `REPAIR`: Manutenção técnica / reparo de fibra / troca de roteador.
  - `TRANSFER`: Mudança de endereço do ponto de acesso.
  - `RETRIEVAL`: Recolhimento de equipamentos após cancelamento.
- **Fluxo de Instalação:**
  - O técnico vincula o Serial/MAC da ONU/Roteador.
  - Ao marcar como `SUCCESS`, dispara o evento `WorkOrderCompletedEvent`.

### 4.5. Módulo de Rede & Provisionamento Desacoplado (Multi-Driver)
- **Arquitetura 100% Desacoplada e Orientada a Eventos:** O ERP não depende de uma única tecnologia de rede e pode delegar a execução para drivers plugáveis ou microsserviços dedicados.
- **Drivers Suportados (Plugáveis & Configuráveis por POP/Ponto de Acesso):**
  1. **SmartOLT API:** Para provedores com infraestrutura de fibra FTTH gerida pelo SmartOLT (Huawei, ZTE, Fiberhome, VSOL).
  2. **Microsserviço de Rede Dedicado (gRPC / REST):** Serviço externo isolado para provedores de grande porte com regras customizadas de NOC.
  3. **MikroTik RouterOS API:** Conexão direta com concentradores RouterOS para criação de PPPoE Secrets, IPs estáticos e Simple Queues de velocidade.
  4. **FreeRADIUS / CoA:** Integração com servidores Radius para autenticação e desconexão forçada (CoA / Packet of Disconnect).
  5. **NoOp / Manual:** Para ambientes de teste ou homologação sem impacto na rede física.
- **Gatilhos Automáticos por Eventos de Domínio:**
  - `ContractActivatedEvent` ➡️ Provisionamento automático de acesso e liberação de banda.
  - `ContractBlockedEvent` (Inadimplência) ➡️ Redirecionamento para pool de aviso ou bloqueio de pacotes.
  - `PaymentConfirmedEvent` ➡️ Desbloqueio e restauração instantânea do sinal (< 10 segundos).
  - `ContractCanceledEvent` ➡️ Desprovisionamento e desvinculação de ONU.

### 4.6. Módulo Financeiro, Cobrança & Multi-Gateway de Pagamentos
- Geração de cobranças automáticas vinculadas ao dia de vencimento escolhido (ex: dias 5, 10, 15, 20, 25).
- **Arquitetura Multi-Gateway Plugável (Strategy & Router Pattern):**
  - O sistema permite plugar múltiplos gateways simultaneamente e definir regras flexíveis de roteamento.
  - **Roteamento Hierárquico:** Contrato específico ➡️ Plano de assinatura ➡️ Gateway padrão da Empresa (`Company`).
  - **Troca Transparente de Gateway:** Alterar o gateway de um plano ou da empresa não impacta faturas passadas (cada fatura é imutável e vinculada ao seu gateway de origem).
- **Gateway Primário: Xingubit Pay (`https://pay.xingubit.com.br/doc`):**
  - **Pix Imediato (COB):** Para pagamentos instantâneos e ativação imediata.
  - **Pix com Vencimento (COBV):** Para faturas mensais com cálculo diário de juros, multas por atraso e validade pós-vencimento.
  - **Carnês Pix:** Emissão parcelada de 2x a 24x com QR Codes individuais.
  - **Webhooks de Confirmação em Tempo Real (`POST /api/webhooks/payments/xingubit`):** Baixa instantânea e emissão do `PaymentConfirmedEvent` (desbloqueio automático de rede em < 10 segundos).
  - **Integração Fiscal Unificada:** Emissão automática de NFCom (telecom) vinculada à cobrança.
- **Desbloqueio em Confiança:** Permite ao cliente reativar a conexão por 48 horas enquanto o pagamento é processado.

### 4.7. Módulo de Notificações Multicanal
- Envio automático de mensagens com link da fatura / código PIX Copia e Cola / PDF do boleto.
- **Provedores de WhatsApp Plugáveis:**
  1. **Evolution API:** Solução open-source self-hosted para provedores que desejam infraestrutura própria sem custo de licença.
  2. **Z-API:** Plataforma SaaS popular no ecossistema de ISPs brasileiros.
  3. **API Oficial Meta (Twilio / Meta Cloud API):** Canal corporativo com selo de verificação e conformidade oficial.
- **Canal de E-mail (Servidor SMTP Dinâmico por Empresa):**
  - Parametrização customizável por ISP (Host, Porta, Usuário, Senha, TLS/SSL, E-mail de remetente e Nome de exibição).
  - Templates HTML responsivos (com código PIX Copia e Cola, botão para pagamento e anexo de boleto em PDF).
- Canais adicionais: SMS / Webhooks de integração.
- Gatilhos automáticos:
  - Fatura gerada (5 dias antes do vencimento).
  - Lembrete no dia do vencimento.
  - Aviso de atraso (3 dias após vencimento).
  - Notificação de suspensão iminente (10 dias após vencimento).
  - Confirmação de pagamento e agradecimento.

### 4.8. Módulo Fiscal & Compliance Telecom (NFCom Modelo 62 & Convênio 115/03)
- **Multi-Gateway Fiscal Plugável:** Arquitetura desacoplada via Strategy Pattern com suporte a `XingubitPayFiscalDriver` e `MockFiscalDriver`.
- **Parametrização por Empresa:**
  - Configuração de ambiente (Homologação / Produção) e série fiscal.
  - Upload seguro e armazenamento criptografado do Certificado Digital A1 (`.pfx` / `.p12`).
- **Emissão Automática & Painel de Controle (`FiscalDashboard`):**
  - Emissão síncrona/assíncrona da NFCom Modelo 62 com consulta de chave de acesso, status SEFAZ, download do XML e DANFE em PDF.
  - Segregação de tributação automática (SCM / ICMS vs. SVA / ISS).
- **Convênio ICMS 115/03:**
  - Geração dos 4 arquivos magnéticos oficiais (Mestre, Item, Destinatário e Controle) com hashes MD5 cruzados e exportação compactada em `.zip`.
- **Despacho Contábil Mensal Automatizado:**
  - Envio agendado por e-mail com anexo `.zip` contendo todas as notas fiscais e relatórios do mês anterior para a contabilidade do ISP.

### 4.9. Módulo de Almoxarifado, Controle de Ativos & Custódia de Ferramentas
- **Multi-Depósito:** Gestão segregada entre Almoxarifado Central e Estoques Móveis (Veículos dos Técnicos).
- **Rastreabilidade de Ativos Serializados:** Controle individual de ONTs, Roteadores e Switches por Número de Série / MAC / Status (`AVAILABLE`, `IN_TRANSFER`, `IN_CUSTODY`, `INSTALLED`, `DAMAGED`, `RETIRED`).
- **Termos de Custódia de Ferramentas de Alto Valor:**
  - Emissão de Termos de Responsabilidade com força executiva para ferramentas caras (Máquinas de Fusão, OTDR, Power Meter).
  - Controle de devolução, conferência de avarias e histórico imutável de custódia.

### 4.10. Módulo de Helpdesk & Atendimento com Protocolo ANATEL
- **Geração de Protocolo Regulatório:** Numeração sequencial no formato oficial da Anatel (`YYYYMMDD-XXXXXX`).
- **Gestão de SLA:** Matriz de criticidade (Crítica, Alta, Média, Baixa) com cálculo automático de prazos de primeiro atendimento e resolução.
- **Interações & Histórico:** Registro de mensagens públicas para o cliente e notas internas entre suporte e equipe técnica.
- **Integração com O.S.:** Disparo de O.S. de Reparo vinculada ao chamado quando necessário atendimento em campo.

### 4.11. Portal do Técnico Mobile-First & GeoCEP
- **Interface Otimizada para Celular:** Visualização de ordens de serviço em cards verticais, botões rápidos de contato (WhatsApp, Telefone) e rota de deslocamento.
- **Mapas Vetoriais Integrados (MapLibre GL):** Renderização de mapas da API GeoCEP (`geocep.api.br`) a 60fps com localização em tempo real do técnico e ponto de instalação.
- **Crowdsourcing Predial (`POST /v1/contribute`):** Coleta da coordenada GPS submétrica em frente ao número predial do cliente para alimentar a base GeoCEP.
- **Assinatura Digital Touch:** Coleta da assinatura do cliente na tela do celular e ativação instantânea da conexão com baixa da O.S.

### 4.12. Central do Assinante (Portal do Cliente)
- **Autoatendimento Web:** Consulta e emissão de 2ª via de faturas com Pix Copia e Cola e QR Code dinâmico.
- **Desbloqueio em Confiança:** Reativação automática do sinal por 48 horas em caso de suspensão por atraso.
- **Abertura de Chamados:** Solicitação de suporte com protocolo Anatel instantâneo.
- **Download de Documentos:** Acesso rápido ao contrato assinado e DANFE da NFCom.

---

## 5. Requisitos Não Funcionais (NFRs)
- **Segurança:** Autenticação via JWT Stateless, senhas com BCrypt (custo 12), proteção contra IDOR através de identificadores **UUIDv7**.
- **Consistência e Confiabilidade:** Padrão **Transactional Outbox** para garantir que nenhum evento seja perdido se uma conexão externa oscilar.
- **Auditoria:** Registro imutável de logs de alteração cadastral, liberação manual de sinal, transferências de estoque e estornos financeiros.
- **Null-Safety:** Governança estrita de nulos via **JSpecify** (`@NullMarked`) no Java 25.
- **Cobertura de Testes:** Suíte automatizada com mais de 100 testes unitários, integrados e teste de ciclo de vida operacional E2E completo.
- **Banco de Dados:** PostgreSQL 17+ com Flyway para versionamento de schema.

