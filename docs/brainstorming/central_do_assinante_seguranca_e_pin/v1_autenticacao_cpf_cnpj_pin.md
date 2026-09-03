# Central do Assinante: Autenticação Segura via CPF/CNPJ & PIN de 4 Dígitos
**Versão**: v1
**Data**: 2026-09-03
**Status**: Planejamento e Especificação Técnica

---

## 1. Princípios de Negócio e Segurança
1. **Separação Rígida de Domínios**:
   - O ERP Administrativo é voltado exclusivamente para os operadores do provedor (Administradores, Financeiro, NOC, Suporte, Técnicos de Campo).
   - A Central do Assinante (`/portal/client`) é uma aplicação pública para o cliente final.
   - O link "Central do Assinante" DEVE ser removido da Sidebar administrativa do ERP para não induzir a confusão de papéis.
2. **Equivalência Estrita Dev vs Prod**:
   - Zero código de apresentação, "mock de demonstração" ou fallbacks inseguros que entreguem dados sem credenciais.
   - A única diferença entre Dev e Prod é a carga do banco de dados (seeder de testes local).
3. **Fluxo de Autenticação do Assinante**:
   - Entrada direta via Home do Portal ou URL `/portal/client`.
   - Etapa 1: Digitação de CPF (Pessoa Física) ou CNPJ (Pessoa Jurídica).
   - Validação no Backend: localiza o cliente ativo.
   - Etapa 2 (PIN de 4 dígitos):
     - Se o cliente já possui um PIN cadastrado: exige a digitação do PIN.
     - Se o cliente ainda não possui PIN: permite o primeiro acesso ou exige o cadastro de um PIN numérico de 4 dígitos para proteger suas faturas e histórico de chamados.
     - Permite que um atendente no ERP defina um PIN temporário ou resete o PIN se o cliente solicitar.
4. **Eliminação do Fallback Inseguro**:
   - O método `resolveCustomerId` em `ClientPortalController.java` NUNCA mais fará `customerRepository.findAll().stream().findFirst()`.
   - Se a requisição não tiver identificação legítima autenticada (token ou sessão de cliente, ou operador administrativo autenticado no ERP), o backend responderá `401 Unauthorized`.
