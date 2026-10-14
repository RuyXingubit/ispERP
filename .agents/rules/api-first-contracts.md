# DIRETRIZ MANDATÓRIA: WORKFLOW API-FIRST E CONTRATOS DE API

Esta regra governa qualquer criação, alteração ou exclusão de endpoints, parâmetros, DTOs e contratos de comunicação entre o Backend (`backend/`), o ERP Operacional (`frontend-flutter/`), a Central do Assinante (`frontend-customer/`) e o Site Institucional (`frontend-site/`) no repositório **ispERP**.

---

## 1. Princípio da Fonte Única da Verdade (Contract-First)
- Toda nova feature, alteração de rota, inclusão de campo ou ajuste de payload **DEVE nascer primeiramente dentro da pasta `contracts/openapi/`**.
- É **PROIBIDO** criar ou alterar anotações de rota manuais (`@GetMapping`, `@PostMapping`, `@PatchMapping`, etc.) fora das interfaces geradas ou criar classes DTOs de contrato avulsas no Backend sem que o contrato OpenAPI correspondente tenha sido previamente atualizado.
- Os clientes frontends (`frontend-flutter/` via `Dio`, `frontend-customer/` e `frontend-site/` via `fetch` em Vanilla TypeScript) utilizam repositórios e modelos enxutos próprios que devem espelhar estritamente as rotas, métodos HTTP e schemas definidos no contrato OpenAPI.

---

## 2. Ciclo de Execução Obrigatório
Sempre que uma tarefa envolver comunicação cliente-servidor, DEVE seguir estritamente o ciclo:
1. **CONTRATO (YAML):** Declarar/ajustar a rota, schemas de request/response e códigos HTTP no domínio correspondente em `contracts/openapi/domains/<dominio>/` e referenciá-la em `contracts/openapi/openapi.yaml`.
2. **GERAÇÃO E VALIDAÇÃO AUTOMÁTICA (Codegen Backend):** Executar o script unificado obrigatório:
   ```bash
   ./scripts/generate-api.sh
   ```
   *Este comando valida e empacota o bundle com Redocly (`contracts/openapi/openapi.bundled.json`) e gera as interfaces `@Validated` e DTOs Java no Backend via Gradle (`openApiGenerate`).*
3. **IMPLEMENTAÇÃO BACKEND:** Implementar os métodos da interface gerada (`implements *Api`) nos Controllers e Services Spring Boot, utilizando MapStruct para conversão entre Entidades e DTOs.
4. **IMPLEMENTAÇÃO FRONTEND / MOBILE:** Consumir os endpoints homologados nos repositórios de `frontend-flutter/`, `frontend-customer/` ou `frontend-site/`, acompanhados dos respectivos testes unitários.

---

## 3. Orquestrador de Contratos (`./scripts/generate-api.sh`)
O script central executa de forma determinística:
- **Etapa 1/2 (Bundle & Lint):** Redocly CLI (`./scripts/bundle-contracts.sh`) — Valida a semântica OpenAPI 3.0.3 e resolve `$ref` criando `contracts/openapi/openapi.bundled.json`.
- **Etapa 2/2 (Backend Codegen):** Gradle OpenAPI Generator — Gera interfaces `@Validated` (`br.dev.xb.isperp.api.contract.*Api`) e DTOs Java (`br.dev.xb.isperp.api.dto.*`) em `backend/build/generated/openapi/`.

---

## 4. Segurança e Isolamento de DTOs
- **NUNCA exponha ou receba diretamente entidades JPA (`@Entity`) em rotas de API.**
- Todo endpoint deve consumir e produzir DTOs estritos gerados a partir do schema do contrato (prevenção contra *Mass Assignment* e vazamento de dados internos).
- Erros devem seguir o padrão RFC 7807 (Problem Details), sem vazamento de stacktrace ou metadados de infraestrutura para o cliente.
- Validações de entrada (regex de CPF/CNPJ, tamanho de campos, valores mínimos e enums válidos) devem ser definidas no contrato e aplicadas via Bean Validation (`@Valid`, `@NotNull`, `@Size`).
