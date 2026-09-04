# v8 - Planejamento e Execução da Migração do Domínio de Contratos (Contracts)

## 1. Contexto e Diagnóstico Atual

O módulo de **Contratos (`ContractController.java`)** é o vínculo contratual entre o cliente (`Customer`) e o serviço de banda larga/internet (`Plan`).

### Diagnóstico de Arquitetura e Segurança:
1. **Exposição de Entidades JPA (@Entity):**
   * O `ContractController` legado retornava a entidade JPA `Contract` diretamente nas rotas `GET /contracts`, `GET /contracts/{id}`, `GET /contracts/customer/{customerId}` e `GET /contracts/status/{status}`.
   * **Ganho de Segurança Mandatório:** A adoção de `ContractResponse` desacopla completamente o modelo relacional de persistência da interface pública, prevenindo vazamento de dados internos de infraestrutura ou mutabilidade inadvertida.
2. **Divergência de Enums e Status:**
   * No banco de dados (`Contract.java`): `DRAFT`, `PENDING_INSTALLATION`, `ACTIVE`, `SUSPENDED`, `CANCELED` (com 1 'L').
   * No OpenAPI (`schemas.yaml`): `PENDING_SIGNATURE`, `PENDING_INSTALLATION`, `ACTIVE`, `SUSPENDED`, `CANCELLED` (com 2 'L').
   * Solução: Harmonizar o enum OpenAPI para contemplar `DRAFT` e utilizar `@ValueMapping(source = "CANCELED", target = "CANCELLED")` e vice-versa no MapStruct `ContractMapper`, mantendo a consistência do banco de dados sem quebras.
3. **Inconsistência de Verbos e Parâmetros na Atualização de Status:**
   * No backend legado: `@PatchMapping("/{id}/status")` esperando `@RequestParam Contract.ContractStatus status`.
   * No frontend legado (`contractService.ts`): `api.put('/contracts/' + id + '/status', { status })`.
   * Solução: O contrato OpenAPI `patch /contracts/{id}/status` aceita tanto o query parameter quanto o corpo JSON `UpdateContractStatusRequest`, unificando a especificação e corrigindo o verbo no frontend.
4. **Campos Faltantes no Schema OpenAPI:**
   * A interface web (`ContractList.tsx`) exibe cidade, estado e CEP (`city`, `state`, `zipCode`). O schema inicial em `schemas.yaml` não continha esses campos, nem `ctoId`, `ctoPortNumber` e `pendingOnboardingCredit`.
   * Solução: Adicionar todos os atributos de endereço e porta ao `ContractResponse` e adicionar `ContractCreateRequest` e `ContractUpdateRequest`.

---

## 2. Plano de Execução

1. **Ajustes no Contrato OpenAPI (`contracts/openapi/domains/contracts/`):**
   * Expandir `schemas.yaml` com `DRAFT` no enum, novos campos em `ContractResponse` e schemas para criação/atualização.
   * Expandir `contracts.yaml` com `POST /contracts` (criação) e `PUT /contracts/{id}` (edição).
   * Validar com Redocly (`npx @redocly/cli lint`).
2. **Geração de Código Automatizada:**
   * Executar `./scripts/generate-api.sh` com `--rerun-tasks`.
3. **Backend (Spring Boot 4.1 / Java 25):**
   * Criar `ContractMapper.java` com MapStruct.
   * Implementar `ContractsApi` em `ContractController.java`.
   * Criar `ContractControllerTest.java` cobrindo 100% dos endpoints.
   * Rodar `./gradlew test` no backend.
4. **Frontend (React 19 / Orval):**
   * Refatorar `contractService.ts` consumindo o cliente Orval gerado.
   * Rodar `npm run typecheck` e `npm run build`.
5. **Docker Compose & Smoke Tests:**
   * Atualizar os containers locais e testar endpoints via HTTP.
