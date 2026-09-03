# ESPECIFICAÇÃO TÉCNICA E DE NEGÓCIO: COCKPITS OPERACIONAIS ESPECIALIZADOS
**Versão:** v2 (Incremental Imutável)  
**Tema:** `cockpits_e_perfis_operacionais`  
**Data:** 03/09/2026  
**Status:** Planejamento e Arquitetura Detalhada  

---

## 1. Visão Geral da Arquitetura de Cockpits

Para garantir máxima eficiência operacional e segurança, o ISP ERP adota a separação estrita de interfaces operacionais. Usuários com papéis de execução (Vendedor, Técnico, Atendente) não devem ser sobrecarregados com menus administrativos ou dados irrelevantes ao seu trabalho:

```
+-----------------------------------------------------------------------------------+
|                               ISP ERP AUTH GATEWAY                                |
+-----------------------------------------------------------------------------------+
       |                                |                             |
       v                                v                             v
[ROLE: SELLER]                 [ROLE: FIELD_TECH]           [ROLE: ATTENDANT]
       |                                |                             |
       v                                v                             v
COCKPIT DO VENDEDOR            COCKPIT DO TÉCNICO           COCKPIT DE ATENDIMENTO
/cockpit/seller                /portal/technician           /cockpit/attendant
- Minhas Vendas                - Minhas OS Atribuídas       - Busca Expressa (CPF/Nome/Tel)
- Comissões Confirmadas        - Navegação GPS              - Status ONU/Radius (Online/Off)
- Previsão de Pagto (Finan.)   - Upload de Fotos            - Gráfico de Tráfego / Sinal
- Meta do Mês                  - Conclusão com Assinatura   - Faturas & 2ª via Pix imediata
- Alerta de Ativação Técnica   - Dispara Alerta ao Vendedor - Abertura Ágil de OS/Chamado
```

---

## 2. Cockpit 1: Vendedor (`/cockpit/seller`)

### Objetivos:
- Visualizar performance individual e progresso rumo à meta mensal.
- Acompanhar comissões e previsão financeira de pagamento.
- **Ciclo Pós-Venda Integrado**: Notificação em tempo real quando o técnico finaliza a OS de instalação, permitindo contato imediato de boas-vindas e garantia de satisfação do cliente.

### Componentes de UI:
1. **Cards de KPIs**:
   - Vendas no Mês (Qtd e Valor Recorrente - MRR).
   - Comissões Confirmadas vs A Provisionar.
   - Data Prevista de Pagamento pelo Financeiro.
   - Termômetro da Meta (ex: 82% atingido).
2. **Esteira de Vendas**:
   - Vendas em Andamento (Aguardando Instalação).
   - Vendas Ativadas Recentemente (Ação: *Ligar Pós-Venda*).
   - Vendas Canceladas / Reprovadas (com motivo).
3. **Nova Venda Rápida**:
   - Acesso direto ao formulário ágil de venda.

---

## 3. Cockpit 2: Técnico de Campo (`/portal/technician`)

### Objetivos:
- Foco absoluto na rota do dia e nas ordens de serviço atribuídas.
- Eliminação de papel: fotos de ancoragem, sinal óptico e assinatura na tela.

### Componentes de UI:
1. **Agenda do Dia**:
   - Lista sequencial de OS (Instalação, Manutenção, Mudança de Endereço).
   - Botão "Abrir Rota" (Waze / Google Maps).
2. **Execução da OS**:
   - Iniciar Deslocamento -> Cheguei no Local -> Em Execução.
   - Validação de Sinal da ONU (potência dBm).
   - Upload de fotos (caixa CTO, fixação do cabo, conector, roteador do cliente).
3. **Finalização com Assinatura**:
   - Assinatura na tela do smartphone pelo cliente.
   - Conclusão da OS dispara evento assíncrono:
     - Ativação do contrato e conexão Radius.
     - Notificação no Cockpit do Vendedor para pós-venda.

---

## 4. Cockpit 3: Atendente / Suporte Ágil (`/cockpit/attendant`)

### Objetivos:
- Atendimento telefônico em menos de 10 segundos para encontrar o cliente.
- Visão 360° imediata do cliente sem navegar por várias abas do ERP.

### Componentes de UI:
1. **Omni-Search Bar**:
   - Digitação contínua: busca por CPF, CNPJ, Nome, Telefone, Endereço, Login PPPoE ou MAC.
2. **Card Central do Assinante**:
   - Nome, Documento, Telefones com link direto para WhatsApp.
   - Status da Conexão:
     - `ONLINE` há X horas (IP atribuído, Concentrador NAS, Sinal Óptico -19.4 dBm).
     - `OFFLINE` há Y horas/dias.
   - Mini-gráfico de consumo em tempo real ou últimas 24h.
3. **Ações Rápidas de 1 Clique**:
   - Enviar 2ª via Pix por WhatsApp / Copiar código Pix.
   - Conceder Desbloqueio em Confiança (se bloqueado).
   - Abrir Chamado / Ordem de Serviço de Reparo Técnico.
   - Reiniciar Conexão Radius (Disconnect Request / PoD).

---

## 5. Roteamento Inteligente pós-Login

No arquivo `frontend/src/routes/AppRoutes.tsx` e `Login.tsx`:
- Usuário logado com `ROLE_SELLER` -> direcionado automaticamente para `/cockpit/seller`.
- Usuário logado com `ROLE_TECHNICIAN` -> direcionado automaticamente para `/portal/technician`.
- Usuário logado com `ROLE_ATTENDANT` -> direcionado automaticamente para `/cockpit/attendant`.
- Usuário logado com `ROLE_ADMIN` -> direcionado para o `/dashboard` com acesso irrestrito e atalho para alternar entre quaisquer cockpits.
