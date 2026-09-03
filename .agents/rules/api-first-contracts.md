# DIRETRIZ MANDATÓRIA: WORKFLOW API-FIRST E CONTRATOS DE API

Esta regra governa qualquer criação, alteração ou exclusão de endpoints, parâmetros, DTOs e contratos de comunicação entre Backend, Frontend e futuros clientes (Mobile / Microsserviços) no repositório **ispERP**.

---

## 1. Princípio da Fonte Única da Verdade (Contract-First)
- Toda nova feature, alteração de rota, inclusão de campo ou ajuste de payload **DEVE nascer primeiramente dentro da pasta `contracts/openapi/`**.
- É **PROIBIDO** criar ou alterar anotações de rota (`@GetMapping`, `@PostMapping`, `@PatchMapping`, etc.) ou criar classes DTOs manuais no Backend sem que o contrato OpenAPI correspondente tenha sido previamente atualizado.
- É **PROIBIDO** criar ou alterar interfaces TypeScript (`types/*.ts`) ou métodos de chamada HTTP (`services/*.ts`) manuais no Frontend para endpoints cobertos pelo contrato.

---

## 2. Ciclo de Execução Obrigatório
Sempre que uma tarefa envolver comunicação cliente-servidor, DEVE seguir estritamente o ciclo:
1. **CONTRATO (YAML):** Declarar/ajustar a rota, schemas de request/response e códigos HTTP no domínio correspondente em `contracts/openapi/domains/<dominio>/`.
2. **GERAÇÃO AUTOMÁTICA (Codegen):** Executar o script unificado obrigatório:
   ```bash
   ./scripts/generate-api.sh
   ```
   *Este comando compila o bundle (Redocly), gera stubs/interfaces Java no Backend (Gradle `openApiGenerate`) e clientes/tipos TypeScript no Frontend (Orval `npm run codegen`).*
3. **IMPLEMENTAÇÃO BACKEND:** Implementar as interfaces geradas nos Controllers e Services Spring Boot (ex: `WorkOrdersApi`, `CustomersApi`, etc.).
4. **IMPLEMENTAÇÃO FRONTEND / MOBILE:** Consumir os métodos e tipos gerados em `frontend/src/api/generated/` nos services e páginas React.

---

## 3. Orquestrador Unificado de Contratos (`./scripts/generate-api.sh`)
O assistente e os desenvolvedores NUNCA precisam rodar geradores de backend e frontend separadamente. O script central executa de forma determinística:
- **Etapa 1/3 (Bundle & Lint):** Redocly CLI — Valida a semântica OpenAPI e resolve `$ref` criando `contracts/openapi/openapi.bundled.json`.
- **Etapa 2/3 (Backend):** Gradle OpenAPI Generator — Gera interfaces `@Validated` e DTOs Java no Backend em `backend/build/generated/openapi/`.
- **Etapa 3/3 (Frontend):** Orval — Gera clientes HTTP Axios com interceptor JWT e tipos TypeScript em `frontend/src/api/generated/`.

---

## 4. Blindagem de Segurança e DTOs
- **NUNCA exponha ou receba diretamente entidades JPA (`@Entity`) em rotas de API.**
- Todo endpoint deve consumir e produzir DTOs estritos gerados a partir do schema do contrato (prevenção contra *Mass Assignment* e vazamento de dados internos).
- Erros devem seguir o padrão RFC 7807 (Problem Details), sem vazamento de stacktrace ou metadados de infraestrutura para o cliente.
- Validações de entrada (regex de CPF/CNPJ, tamanho de campos, valores mínimos e enums válidos) devem ser definidas no contrato e aplicadas automaticamente via Bean Validation (`@Valid`, `@NotNull`, `@Size`).
