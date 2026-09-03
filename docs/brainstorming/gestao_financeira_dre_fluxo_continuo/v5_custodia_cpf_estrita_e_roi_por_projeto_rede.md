# Gestão Financeira v5: Custódia Material por CPF, Ciclo Estrito de Dinheiro & Payback por Projeto de Rede

> **Status:** Rascunho Vivo / Em Discussão  
> **Data:** 2026-09-02  
> **Versão:** v5 (Evolução da v4)  
> **Tema Central:** Correções de Rigor Operacional: Eliminação de "Estoque Móvel" (Apenas CPF responde por materiais e dinheiro), Rastreabilidade Bancária com Dupla Conferência de Depósito e Gestão de Payback / CAC por Projeto de Rede (Mapa de Guerra Comercial).

---

## 1. Princípio da Responsabilidade Pessoal: "Veículo Não Tem CPF"

Em auditoria e direito trabalhista/cível, **um veículo utilitário é apenas uma lata inanimada sem responsabilidade jurídica**. Se sumirem 5 ONTs ou 3 bobinas de drop da caçamba de uma caminhonete, o veículo não pode ser acionado judicialmente nem prestar contas.

### A Regra Inviolável no ispERP:
> **Não existe "estoque de viatura". Todo e qualquer equipamento ou insumo em campo está sob a guarda e Carga Patrimonial do CPF DO COLABORADOR.**

```mermaid
graph LR
    Almoxarifado[Almoxarifado Central] -->|Carga de Materiais com Assinatura Digital| CPFTecnico[CPF do Técnico Carlos\n(Fiel Depositário)]
    CPFTecnico -->|Troca de Viatura| CPFTecnico
    CPFTecnico -->|Repasse de Peças| CPFOutroTecnico[CPF do Técnico Marcos\n(Exige Duplo Aceite de Contagem)]
    CPFTecnico -->|Instalação no Cliente| ContratoCliente[Baixa por Número de Série na O.S.]
```

- **Troca de Carro:** Se o técnico Carlos deixar a viatura na oficina e pegar outra, a responsabilidade dos materiais continua 100% no CPF dele.
- **Transferência entre Técnicos:** Se Carlos repassar 3 ONTs para Marcos na rua, abre uma transferência no aplicativo. Marcos deve conferir os números de série e clicar em `[ Confirmar Recebimento de Carga ]`. Sem isso, o material continua na responsabilidade do Carlos.

---

## 2. Ciclo Estrito de Dinheiro Vivo: Nenhuma Suposição de Depósito

Estar nas mãos da atendente Maria **NÃO SIGNIFICA** que o dinheiro entrou na conta bancária da empresa. A criatividade humana para desvios não tem limites.

### A Jornada Rígida de Cada Centavo em Dinheiro Vivo:

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente (Paga em Espécie)
    actor Tecnico as Técnico (CPF Carlos)
    actor Caixa as Caixa Escritório (CPF Maria)
    participant ERP as Livro-Caixa ispERP
    actor Fiscal as Auditor / CFO (CPF Roberto)
    participant Banco as Extrato Bancário Real

    Cliente->>Tecnico: 1. Paga R$ 300 em notas
    Tecnico->>ERP: 2. Registra recebimento ➔ Recibo timbrado emitido
    Note over ERP: Dinheiro está na CUSTÓDIA DO CPF DO CARLOS.<br/>(Saldo Devedor Pessoal de Carlos: R$ 300).

    Tecnico->>Caixa: 3. Carlos entrega as cédulas físicas para Maria no balcão
    Caixa->>ERP: 4. Maria confere as notas e clica em [Confirmar Recebimento]
    Note over ERP: Custódia sai do Carlos e ENTRA NO CPF DA MARIA.<br/>(Saldo Devedor Pessoal de Maria: R$ 300).<br/>⚠️ O dinheiro AINDA NÃO ESTÁ NO BANCO!

    rect rgb(255, 245, 245)
    Note over Caixa,Banco: Etapa de Destinação da Custódia da Maria
    alt Opção A: Depósito em Conta Bancária
        Caixa->>Banco: 5a. Maria vai à agência e deposita o dinheiro
        Caixa->>ERP: 6a. Maria anexa o Comprovante de Depósito Digitalizado
        Note over ERP: Status = DEPÓSITO PENDENTE DE CONFERÊNCIA BANCÁRIA.<br/>A custódia AINDA é de Maria até a confirmação!
        Fiscal->>Banco: 7a. CFO Roberto abre o Internet Banking / Extrato OFX
        Fiscal->>ERP: 8a. Roberto confere que os R$ 300 caíram na conta e clica em [Conciliar e Baixar Custódia]
        Note over ERP: ✅ Custódia da Maria zerada.<br/>Dinheiro finalmente integralizado na conta bancária!
    else Opção B: Pagamento de Despesa Local Autorizada
        Caixa->>ERP: 5b. Maria registra pagamento de despesa em dinheiro (ex: abastecimento emergencial)
        Note over ERP: Exige OBRIGATORIAMENTE Pedido de Compra / Autorização Prévia de Gasto do Gestor + Cupom Fiscal anexado.
    end
    end
```

---

## 3. O CAC Real & A Gestão Financeira por PROJETO DE REDE

Você está coberto de razão: **R$ 300,00 de taxa de ativação não pagam o CAC real de um cliente de fibra**.
O CAC real de telecomunicações é muito mais pesado:
- ONT Wi-Fi: ~R$ 300,00.
- Cabo Drop (ex: 200m a R$ 0,50/m): R$ 100,00.
- Conectores ópticos de campo (par): R$ 20,00.
- PTO, cordão óptico e fixadores: R$ 15,00.
- Hora técnica do instalador + Encargos: ~R$ 60,00.
- Combustível e desgaste da viatura (viagem ida e volta): ~R$ 50,00 a R$ 120,00.
- **CAC Real Total da Instalação:** ~R$ 545,00 a R$ 650,00.
- **Déficit Imediato na Ativação:** O cliente pagou R$ 300,00 ➔ A empresa ainda está no **negativo em -R$ 245,00 a -R$ 350,00** logo no primeiro dia! Esse valor só começa a se pagar a partir da 3ª ou 4ª mensalidade.

---

## 4. O Mapa de Guerra Comercial: Projetos de Rede com Payback Georreferenciado

Em vez de apenas misturar tudo em uma panela só, o ispERP introduz a **Gestão Financeira por Projeto de Expansão de Rede (Centro de Custo Topológico)**.

```mermaid
flowchart TD
    subgraph ProjetoRede["Projeto de Rede: 'Expansão Bairro Novo'"]
        CustosIniciais["Custos de Implantação Alocados ao Projeto:\n• 15km de Cabos de Fibra (R$ 22.000)\n• 40 Caixas de Emenda e CTOs (R$ 14.000)\n• Empreiteira de Lançamento e Fusão (R$ 18.000)\n• Aumento de Postes na Concessionária (R$ 3.500/mês)\n• Viagens e Diárias da Equipe (R$ 4.200)\n• Panfletagem e Tráfego Local (R$ 2.500)\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\nInvestimento Total do Projeto: R$ 64.200"]
        
        CTOsVinculadas["CTOs do Projeto:\nCTO-01 a CTO-40 (640 portas disponíveis)"]
    end

    subgraph RetornoProjeto["Entradas Geradas Pelo Bairro Novo"]
        Ativacoes["Taxas de Instalação Recebidas no Bairro"]
        Mensalidades["Mensalidades Recorrentes dos Clientes deste Bairro"]
    end

    subgraph MotorPayback["Painel de Payback do Projeto"]
        StatusPayback["📊 Termômetro do Projeto:\n• Investido: R$ 64.200\n• Retornado: R$ 28.400 (44% pago)\n• Saldo a Recuperar: R$ 35.800\n• Clientes Ativos: 85 clientes\n• Tempo Estimado para Break-Even: Mais 5 meses"]
    end

    subgraph EstrategiaComercial["🎯 O Direcionador de Vendas (Mapa de Guerra)"]
        AlertaComercial["🚨 DIRECIONADOR DE CAMPANHA:\n'Atenção time de vendas: o Projeto Bairro Novo ainda não pagou o investimento inicial e tem 555 portas ociosas. Concentrem 80% das ações comerciais nesta região!'"]
    end

    CustosIniciais --> MotorPayback
    CTOsVinculadas --> RetornoProjeto
    RetornoProjeto --> MotorPayback
    MotorPayback --> EstrategiaComercial
```

### Como Isso Muda o Jogo para o Dono do Provedor:
1. **Fim dos Tiros no Escuro:** O empresário descobre com clareza quais projetos foram lucrativos e quais viraram "cemitérios de cabos" que nunca deram retorno.
2. **Campanhas de Vendas Cirúrgicas:** Em vez de fazer panfletagem genérica na cidade inteira, a gerência comercial abre o painel do ispERP e filtra os projetos com **menor taxa de retorno sobre o investimento**. A equipe de vendas é enviada exatamente para as ruas onde a fibra já está paga e as portas estão ociosas esperando cliente.
3. **Métrica de Saúde por Projeto:**
   $$\text{Payback Líquido do Projeto} = \text{Investimento Total de Implantação} - \sum (\text{Ativações} + \text{Mensalidades Líquidas dos Clientes Vinculados})$$

---

## 5. Próximos Passos
- Incorporar a entidade `NetworkProject` (Projeto de Rede) com relacionamento para `FtthClosure`, `FtthCto` e `ChartOfAccount` (Centro de Custos).
- Modelar o fluxo de **Depósito Bancário com Duplo Aceite do Financeiro**.
- Modelar o fluxo de **Pedidos de Compra / Autorização Prévia de Despesas em Espécie**.
