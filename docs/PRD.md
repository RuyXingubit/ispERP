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

### 4.5. Módulo de Rede & Provisionamento (MikroTik / Radius / OLT)
- Provisionamento automatizado após a conclusão da O.S.
- Criação de credenciais PPPoE/IPoE no servidor Radius / MikroTik.
- Bloqueio automático: redirecionamento para página de aviso de corte ou corte total de pacotes caso a fatura atinja a tolerância de dias de atraso.

### 4.6. Módulo Financeiro & Cobrança (Billing)
- Geração de cobranças automáticas vinculadas ao dia de vencimento escolhido (ex: dias 5, 10, 15, 20, 25).
- Suporte a múltiplos gateways de pagamento:
  - **PIX Dinâmico com Webhook:** Baixa instantânea e reativação automática da conexão em menos de 10 segundos.
  - **Boleto Bancário:** Emissão registrada com leitura de retorno/webhook.
  - **Cartão de Crédito Recorrente.**
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

### 4.8. Módulo Fiscal (Telecom NFCom)
- Emissão de Nota Fiscal de Serviços de Comunicação (NFCom - Modelo 62).
- Exportação de arquivos magnéticos do Convênio ICMS 115/03 (Modelos 21 e 22).

---

## 5. Requisitos Não Funcionais (NFRs)
- **Segurança:** Autenticação via JWT Stateless, senhas com BCrypt (custo 12), proteção contra IDOR através de identificadores **UUIDv7**.
- **Consistência e Confiabilidade:** Padrão **Transactional Outbox** para garantir que nenhum evento seja perdido se uma conexão externa oscilar.
- **Auditoria:** Registro imutável de logs de alteração cadastral, liberação manual de sinal e estornos financeiros.
- **Cobertura de Testes:** Meta de cobertura mínima de 80% em regras de domínio e fluxos de eventos com TDD (JUnit 5 + Mockito + Testcontainers).
- **Banco de Dados:** PostgreSQL 17+ com Flyway para versionamento de schema.
