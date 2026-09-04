# Diretriz Obrigatória: Zero Dados Falsos e Desenvolvimento Estritamente Sob Demanda

## Propósito
Garantir que todo o desenvolvimento de software seja 100% fiel às instruções reais do usuário, proibindo a introdução de dados simulados/falsos ("encher linguiça com isopor"), telas ou lógicas não solicitadas, e prevenindo retrabalho de remoção de código indesejado.

---

## Regras Mandatórias de Execução (WORKSPACE)

### 1. Proibição Total de Dados Fictícios e Placeholders ("Sem Encher Linguiça")
- **NUNCA invente dados mocados**, listas falsas, cards de faturamento inventados, métricas simuladas ou relatórios fictícios para preencher telas ou APIs.
- Caso uma funcionalidade ou tela ainda não tenha dados ou endpoints solicitados, ela **DEVE permanecer em estado limpo, neutro e enxuto** (estado vazio real ou indicador de prontidão de integração).
- Proibido criar "modos de demonstração" sem servidor, mocks sintéticos ou dados decorativos sem pedido prévio do usuário.

### 2. Fluxo Estrito: Pedir -> Executar o Mínimo Necessário -> Testar -> Aprovar -> Avançar
- **NUNCA antecipe funcionalidades não pedidas**: Não crie fluxos extras, botões secundários ou automações que o usuário não solicitou.
- O ciclo de desenvolvimento deve seguir rigorosamente:
  1. **Pedido**: O usuário instrui exatamente o que precisa ser feito.
  2. **Implementação Enxuta**: O assistente desenvolve estritamente o escopo solicitado.
  3. **Verificação**: Execução de testes unitários e checagem de erros.
  4. **Aprovação**: O assistente apresenta a entrega para o usuário testar e aprovar.
  5. **Próximo Passo**: Somente após a validação do usuário, avança-se para o item seguinte.
- O usuário nunca deve perder tempo pedindo para retirar coisas que não solicitou.

### 3. Tolerância Zero a Overflows e Quebras de Layout
- Em desenvolvimento frontend (Flutter, Web, Mobile), todas as UIs devem ser construídas defensivamente com constraints adequadas (`Expanded`, `Flexible`, `SingleChildScrollView`, `Wrap`, `TextOverflow.ellipsis`).
- Erros de `RenderFlex overflowed by N pixels` são falhas de entrega e devem ser prevenidos no código.
