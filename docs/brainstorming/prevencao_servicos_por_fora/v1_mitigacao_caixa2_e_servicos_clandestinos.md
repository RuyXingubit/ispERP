# Brainstorming v1: Mitigação do "Golpe do Serviço por Fora" (Caixa 2 Clandestino em Campo e Balcão)

## 1. O Problema Real
O técnico ou atendente negocia diretamente com o cliente um serviço (ex: Mudança de Endereço, Ponto Adicional, Troca de Cômodo, Reparo Físico):
- Cobra R$ 50,00 a R$ 150,00 diretamente do cliente (em dinheiro ou Pix pessoal).
- Nada é lançado no ERP.
- A empresa arca com o custo de transporte, homem-hora, drop de fibra, conectores e desgaste de frota.
- O colaborador embolsa 100% do valor do serviço.

---

## 2. Pilares de Prevenção e Blindagem Sistêmica

### Pilar 1: Bloqueio Lógico de Rede (OLT & RADIUS / Option 82)
- **A Verdade Física da Rede:** Uma mudança de endereço implica fisicamente trocar de caixa de emenda, splitter, CTO ou porta PON da OLT.
- **Detecção de Deslocamento de ONU:**
  - A OLT monitora em qual porta PON e CTO cada serial de ONU está autorizado.
  - Se uma ONU cadastrada na CTO-04 (Bairro Centro) repentinamente tenta autenticar na PON-02 / CTO-18 (Bairro Novo), a OLT gera um evento de `ONU_ROAMING_ANOMALY`.
  - Sem passar pelo ispERP para formalizar a Mudança de Endereço, a OLT **não autoriza a VLAN de serviço** nem o perfil de banda no endereço novo.
  - O técnico de campo não possui a senha enable/root da OLT (quem provisiona é o backend do ispERP via SmartOLT / API / SNMP). Logo, ele é obrigado a registrar a O.S. no sistema para que a internet volte a funcionar no novo endereço.

### Pilar 2: Rastreabilidade Forense de Consumo de Materiais (Custódia por CPF)
- Na Milestone 28, eliminamos o conceito de "estoque do veículo".
- O técnico possui na sua carga pessoal (CPF):
  - 1 bobina de drop compacta (ex: 500m).
  - 50 conectores de campo.
  - 10 ONTs.
- Para realizar uma mudança de endereço, o técnico gasta em média de 50 a 100 metros de cabo drop e 2 conectores novos.
- Se o técnico não abre a O.S., ele não tem como justificar a queima desse material. Na conferência semanal do almoxarifado, o sistema aponta:
  $$\text{Drop Consumido sem O.S.} = \text{Carga Inicial} - \text{Estoque Físico Restante} - \sum \text{Baixas em O.S. Oficiais}$$
- Se houver divergência, o valor do material é debitado na folha ou cobrado na prestação de contas do CPF do colaborador.

### Pilar 3: Transparência Radical com o Assinante (Aviso Anti-Fraude & Recibo Digital)
- **A Tática do Varejo ("Exija sua Nota"):** O provedor instrui o cliente no contrato e no App:
  > *"Nenhum técnico ou atendente está autorizado a receber valores em chave Pix pessoal. Todo pagamento legítimo gera recibo imediato no seu App e SMS/WhatsApp com link oficial de quitação da [Nome da Empresa]. Pagamentos por fora não possuem garantia técnica e podem resultar no bloqueio da sua conexão."*
- **Check-in Georreferenciado:** Quando o técnico chega na residência do cliente, o aplicativo do técnico exige o check-in por GPS. Se o técnico executa serviço sem check-in, a telemetria do veículo (ou app de campo) acusa parada prolongada sem O.S. associada.

### Pilar 4: Automação da Taxa na Ordem de Serviço de Mudança de Endereço
- Quando a mudança de endereço é solicitada (pelo cliente no app ou pelo atendente), o sistema gera automaticamente a O.S. com a **Taxa de Transferência** atrelada:
  - Ou fatura gerada com vencimento futuro.
  - Ou cobrança Pix gerada na hora no aplicativo do cliente em nome do CNPJ da empresa.
  - Se for recebido em dinheiro pelo técnico, entra na custódia do CPF dele (conforme implementamos na Milestone 28).

---

## 3. Matriz de Decisão & Recomendações Técnicas

| Mecanismo | Nível de Esforço | Eficácia | Como Funciona |
| :--- | :--- | :--- | :--- |
| **Trava de OLT/PON** | Médio | **99%** | A ONU não navega na porta nova sem que uma O.S. autorize a migração de CTO no ispERP. |
| **Balanço Semanal de Materiais** | Baixo | **90%** | Comparar metragem de drop e conectores usados vs baixados em O.S. vinculadas ao CPF. |
| **Alerta WhatsApp de Mudança de Topologia** | Baixo | **85%** | Ao detectar troca de porta PON/CTO, enviar mensagem ao cliente confirmando a solicitação do serviço. |
