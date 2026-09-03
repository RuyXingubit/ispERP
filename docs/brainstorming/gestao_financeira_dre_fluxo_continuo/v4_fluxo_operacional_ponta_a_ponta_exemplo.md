# Gestão Financeira & Operacional v4: Fluxo Ponta a Ponta (Da Venda ao 2º Mês de Faturamento)

> **Status:** Rascunho Vivo / Em Discussão  
> **Data:** 2026-09-02  
> **Versão:** v4 (Evolução da v3)  
> **Objetivo:** Simulação prática, passo a passo, do ciclo de vida completo de um cliente no ispERP: da abordagem comercial à instalação em campo, custódia de dinheiro por CPF, prestação de contas, faturamento recorrente, emissão de NFCom e reflexo contínuo no DRE e no Simulador de Desalavancagem.

---

## 1. Personagens & Cenário do Exemplo Prático

- **Cliente:** Seu João da Silva (CPF cadastrado).
- **Endereço:** Rua das Palmeiras, 340 (Bairro planejado com rede FTTH ativa).
- **Plano Contratado:** Fibra 600 Mega (R$ 119,90/mês) + Taxa de Instalação de R$ 300,00.
- **Vencimento Escolhido:** Todo dia 10.
- **Atores Internos do Provedor:**
  - **Lucas:** Vendedor Comercial externo.
  - **Carlos:** Técnico de Campo com veículo utilitário e estoque móvel.
  - **Maria:** Atendente Financeira / Caixa do Escritório Central.
  - **Roberto:** Dono / CFO do Provedor.

---

## 2. A Linha do Tempo Passo a Passo

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Seu João (Cliente)
    actor Vendedor as Lucas (Vendedor)
    actor Tecnico as Carlos (Técnico Campo)
    actor Caixa as Maria (Financeiro/Caixa)
    participant ERP as ispERP (Core / EDA)
    participant HW as Infra Rede (OLT / RADIUS)
    participant Fis as SEFAZ / Fiscal NFCom
    actor Dono as Roberto (Dono / CFO)

    rect rgb(240, 248, 255)
    Note over Cliente,Vendedor: DIA 01: Venda & Assinatura Eletrônica BACEN
    Vendedor->>ERP: 1. Cadastra Venda (Plano 600M + Taxa R$ 300)
    ERP->>Cliente: 2. Envia Link de Assinatura via WhatsApp
    Cliente->>ERP: 3. Assina na tela e paga Pix R$ 1,00 (BACEN valida titularidade CPF)
    ERP->>ERP: 4. Emite 'CONTRACT_SIGNED' ➔ Agendamento e Reserva de Drop
    end

    rect rgb(245, 255, 245)
    Note over Cliente,Tecnico: DIA 03: Execução em Campo & Recebimento da Taxa em Espécie
    Tecnico->>HW: 5. Auto-Discovery da ONU na OLT (Provisiona 600M)
    HW->>ERP: 6. FreeRADIUS confirma sessão PPPoE ativa
    Cliente->>Tecnico: 7. Paga taxa de R$ 300,00 em cédulas físicas
    Tecnico->>ERP: 8. Registra "Recebido em Espécie" e emite Recibo Timbrado com QR Code
    Note over ERP: Fatura da taxa = PAGA.<br/>Custódia = Saldo Devedor de R$ 300 no CPF do Carlos.
    Tecnico->>ERP: 9. Conclui O.S. com evidências e sinal óptico (-19.2 dBm)
    ERP->>ERP: 10. Emite 'WORK_ORDER_COMPLETED' ➔ Contrato vira ATIVO
    end

    rect rgb(255, 250, 240)
    Note over Tecnico,Caixa: DIA 04: Prestação de Contas & Fechamento de Custódia
    Tecnico->>Caixa: 11. Carlos entrega R$ 300 em notas para Maria no caixa central
    Caixa->>ERP: 12. Maria confirma recebimento do valor na tela
    Note over ERP: Custódia sai do CPF do Carlos e entra no Caixa da Maria.
    Caixa->>Caixa: 13. Maria deposita lote no banco; CFO Roberto aprova conciliação
    end

    rect rgb(248, 240, 255)
    Note over Cliente,ERP: DIA 25/OUT a 10/NOV: Faturamento Mês 1 & NFCom
    ERP->>ERP: 14. Scheduler gera Fatura 1 (R$ 119,90) vencimento 10/11
    ERP->>Cliente: 15. Dispara WhatsApp com Pix Copia-e-Cola e PDF
    Cliente->>ERP: 16. No dia 10/11, João paga via Pix Copia-e-Cola
    ERP->>ERP: 17. Webhook confirma em 2s ➔ 'INVOICE_PAID'
    ERP->>Fis: 18. Emite automaticamente NFCom Modelo 62 na SEFAZ
    end

    rect rgb(240, 255, 255)
    Note over Cliente,Dono: DIA 10/DEZ: Faturamento Mês 2 & Cockpit DRE / Desalavancagem
    ERP->>Cliente: 19. Ciclo recorrente repete fatura Mês 2 (R$ 119,90) ➔ João paga
    ERP->>Dono: 20. DRE consolida EBITDA e Simulador projeta Saída do Vermelho
    end
```

---

## 3. Detalhamento Operacional de Cada Etapa

### Etapa 1: Venda & Formalização Jurídica com Pix R$ 1,00 (Dia 01)
1. **No Tablet do Vendedor (Lucas):**
   - Lucas cadastra a venda: Seleciona o Plano Fibra 600M (R$ 119,90) e marca Taxa de Instalação de R$ 300,00 com vencimento para o dia da visita técnica.
   - O backend valida o CPF e cria o contrato em status `PENDING_PAYMENT_SIGNATURE`.
2. **Na Mão do Cliente (Seu João):**
   - Seu João recebe uma notificação no WhatsApp com o link seguro.
   - Ele lê o contrato com cláusula de fidelidade de 12 meses e comodato de ONU.
   - Para assinar, ele clica em `[ Assinar com Validação Pix ]`.
   - O gateway gera um Pix Dinâmico de R$ 1,00. Seu João paga no aplicativo do banco dele.
   - O Webhook recebe a confirmação bancária do BACEN. O sistema confere se o CPF do titular da conta bancária pagadora é rigorosamente o mesmo CPF cadastrado no contrato.
   - **Status:** Contrato assinado juridicamente com hash SHA-256 e prova irrefutável anti-fraude. Status avança para `PENDING_INSTALLATION`.

---

### Etapa 2: Execução em Campo & Auto-Discovery de Rede (Dia 03)
1. **Despacho Logístico:**
   - O sistema calcula a rota GeoCEP e metragem de cabo drop até a CTO mais próxima (CTO-08, porta 04).
   - O.S. despachada automaticamente para o técnico Carlos, que já possui no veículo o kit de materiais reservado pelo almoxarifado.
2. **Chegada e Ativação Física:**
   - Carlos passa o drop de 180 metros, faz a fusão e liga a ONU no imóvel.
   - No celular de Carlos (portal PWA), ele clica em `[ Buscar ONUs na Porta PON ]`.
   - A OLT responde via SNMP/API com o serial unprovisioned. Carlos vincula a ONU ao contrato do Seu João com 1 clique.
   - A OLT aplica os perfis de tráfego de 600M de Download e 300M de Upload.
   - O FreeRADIUS autentica a sessão PPPoE (`joao.silva`) e confirma: **Sinal Óptico: -19.2 dBm | Status: NAVEGANDO**.

---

### Etapa 3: Recebimento da Taxa de Ativação & Custódia por CPF (Dia 03)
1. **O Pagamento pelo Cliente:**
   - Seu João prefere pagar a taxa de instalação de R$ 300,00 em dinheiro vivo (3 cédulas de R$ 100).
2. **A Ação do Técnico (Carlos):**
   - Carlos seleciona no app: `[ Receber em Dinheiro em Campo ]`.
   - O sistema gera na tela o Recibo Oficial Timbrado com número sequencial e QR Code de autenticidade pública. Carlos imprime na impressora térmica portátil Bluetooth e entrega a Seu João.
3. **O Reflexo Contábil no ERP:**
   - A fatura da taxa de instalação do cliente fica com status `QUITADA` (o cliente não deve nada à empresa).
   - **A Trava de Segurança:** O valor de **R$ 300,00 é lançado a DÉBITO na Conta de Custódia do CPF do Carlos Técnico**.
   - No painel da diretoria/tesouraria, o colaborador Carlos consta com saldo devedor pendente de prestação de contas no valor de R$ 300,00.
   - Carlos conclui a O.S. com foto da instalação e assinatura touch do cliente. O contrato é ativado (`ACTIVE`).

---

### Etapa 4: Prestação de Contas na Tesouraria (Dia 04)
1. **Passagem de Valores com Duplo Aceite:**
   - Ao retornar à base, Carlos entrega as 3 cédulas de R$ 100 para a atendente financeira Maria.
   - Maria abre a tela de `Custódia & Caixa` no ispERP.
   - O sistema exibe: `Transferência de Custódia: Carlos Técnico (CPF X) ➡️ Maria Caixa (CPF Y) - Valor: R$ 300,00`.
   - Maria confere o dinheiro físico e clica no botão `[ Confirmar Recebimento ]`.
2. **Resultado Contábil:**
   - O saldo devedor de Carlos é zerado instantaneamente (ele recebe comprovante no app).
   - O valor passa a estar sob custódia formal de Maria.
   - No fim do dia, Maria reúne os R$ 300 com os demais recebimentos, prepara o envelope de depósito bancário e faz o depósito na agência do provedor.
   - O CFO Roberto importa o extrato OFX no dia seguinte e faz a conciliação bancária:
     - Crédito na Conta Corrente Bancária da Empresa.
     - Lançamento contábil creditando a conta analítica `01.01.01 Taxa de Instalação`.
     - Custo do drop (180m) e da ONU (R$ 130) baixados do estoque e contabilizados em `05.04.01 Materiais de Instalação` (Ativo Imobilizado/CAPEX).

---

### Etapa 5: Faturamento Recorrente do Mês 1 & NFCom Automática (Dia 25/OUT a 10/NOV)
1. **Rotina Automática de Cobrança (25/Outubro):**
   - Às 02:00 da madrugada, o worker `BillingScheduler` varre contratos ativos.
   - Identifica o contrato do Seu João (vencimento dia 10).
   - Gera a Fatura de R$ 119,90 com vencimento em 10/11.
   - Cria a cobrança com Pix Dinâmico Copia-e-Cola e Boleto com código de barras.
   - O `NotificationDispatcher` envia o aviso por WhatsApp e E-mail: *"Olá Seu João, sua fatura de internet com vencimento em 10/11 já está disponível. Pague em 1 clique via Pix."*
2. **O Pagamento (10/Novembro):**
   - No dia do vencimento, às 09:15, Seu João copia o Pix no WhatsApp e paga no banco dele.
   - Em 1.8 segundos, o Webhook da instituição financeira bate no ispERP.
   - O backend valida a assinatura HMAC, compensa a fatura e dispara o evento `INVOICE_PAID`.
3. **Emissão Fiscal Desacoplada:**
   - O módulo fiscal consome o evento e envia a requisição para a SEFAZ:
     - Emissão da **NFCom Modelo 62** com chave de acesso de 44 dígitos.
     - Segregação tributária: R$ 89,90 em SCM (Internet) e R$ 30,00 em SVA.
     - XML e PDF arquivados no S3/SeaweedFS.
     - O link do DANFE fiscal é enviado por e-mail para o cliente.

---

### Etapa 6: O Mês 2 & A Mágica no Cockpit do Dono (Dezembro)
1. **Ciclo Recorrente do Mês 2:**
   - No dia 25/11, o sistema emite a segunda fatura de R$ 119,90 (vencimento 10/12).
   - O cliente paga novamente via Pix no vencimento. O ciclo de baixa e emissão de NFCom se repete de forma 100% autônoma.
2. **Como o Financeiro e o Dono Enxergam a Empresa:**
   - O dono (Roberto) entra no módulo financeiro do ispERP:
   - **No DRE em Tempo Real:**
     - A receita recorrente (MRR) do Seu João soma R$ 119,90 nas receitas do Grupo `01.04.01`.
     - O imposto da NFCom do Seu João (Simples/ICMS) aparece deduzido no Grupo `02`.
     - O custo proporcional de link e aluguel de poste do Seu João aparece no Grupo `03` e `04`.
     - O resultado líquido do Seu João contribui para o **EBITDA daquele mês**.
   - **No Simulador de Desalavancagem ("Saída do Vermelho"):**
     - O sistema registra que o contrato do Seu João já superou o período de payback da instalação (os R$ 300 da taxa cobriram o custo físico inicial do drop e da mão de obra).
     - A partir deste segundo mês, os R$ 119,90 mensais passam a gerar **fluxo de caixa livre**.
     - Esse valor é somado aos outros 40 clientes novos do mês e confrontado diretamente contra a 4ª parcela da máquina de fusão e do caminhão no Grupo `05`.
     - O gráfico de projeção de caixa atualiza a data de virada:
       > *"Com a entrada líquida destes novos contratos e a manutenção da inadimplência em 4.2%, a curva de caixa da empresa atinge equilíbrio positivo em **Março/2027**, momento em que se encerra a última parcela do financiamento de rede."*

---

## 4. Resumo da Harmonia entre os Agentes

| Ator | O Que Faz no Sistema | Como o Sistema Protege a Empresa |
| :--- | :--- | :--- |
| **Vendedor** | Cadastra a venda no catálogo de planos | Validação de CPF e formalização por Pix BACEN (R$ 1,00) que impede contratos falsos. |
| **Técnico** | Instala, afere sinal e recebe a taxa | Auto-discovery sem acesso a senhas da OLT. Dinheiro recebido vira dívida pessoal em seu CPF até a conferência. |
| **Caixa/Financeiro** | Confere cédulas e liquida custódia | Duplo aceite obrigatório na passagem de turnos; nenhum centavo fica órfão. |
| **Orquestrador EDA** | Emite faturas, despacha WhatsApp e emite NFCom | Automação 100% autônoma sem intervenção humana; baixa bancária em < 2 segundos. |
| **Dono / CFO** | Acompanha o DRE e o Simulador | Visão estratégica de EBITDA e previsão da data exata de saída do vermelho. |
