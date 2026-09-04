# v9 - Planejamento da Migração dos Domínios de Autenticação e Usuários (Auth & Users)

## 1. Contexto e Diagnóstico Atual

Os módulos de **Autenticação (`AuthController.java`)** e **Gestão de Usuários (`UserController.java`)** constituem o núcleo de segurança e controle de acesso baseado em papéis (RBAC - Role-Based Access Control) do ispERP.

### Diagnóstico de Arquitetura e Segurança Crítica:
1. **Vulnerabilidade Severa de Exposição de Hash de Senha:**
   * O `UserController` legado retornava diretamente a entidade JPA `@Entity User` nos métodos `getAllUsers()`, `getUserById()`, `createUser()` e `updateUser()`.
   * A entidade `User` contém o atributo `password` (hash BCrypt). O retorno da entidade inteira no payload JSON vazava os hashes de senha de todos os colaboradores do provedor nas listagens e detalhes de usuário!
   * **Ganho Mandatório de Segurança:** A adoção formal do DTO `UserResponse` no contrato OpenAPI **omite 100% o campo `password`**, garantindo que nenhum hash de credencial trafegue pela rede em consultas ou cadastros.
2. **Injeção de Dependências por `@Autowired`:**
   * O `UserController` utilizava injeção de campo com `@Autowired`. Migrar para injeção via construtor com Lombok `@RequiredArgsConstructor`.
3. **Formalização de RBAC no Contrato:**
   * O enum `UserRole` (`ADMIN`, `CFO`, `DIRECTOR`, `USER`, `ATTENDANT`, `FINANCIAL`, `ADMINISTRATIVE_ASSISTANT`, `SUPPORT_N2`, `SUPPORT_ANALYST`, `TECHNICIAN`, `CLIENT`) passa a ser tipado explicitamente no OpenAPI, prevenindo atribuição de papéis inexistentes.
4. **Contrato de Autenticação (`/auth/login`):**
   * Modelar formalmente `LoginRequest` e `LoginResponse` para garantir tipagem ponta a ponta no cliente HTTP gerado pelo Orval.

---

## 2. Escopo da Migração (API-First)

### A. Contratos OpenAPI
1. `contracts/openapi/domains/auth/`:
   * `auth.yaml`: `POST /auth/login`
   * `schemas.yaml`: `LoginRequest`, `LoginResponse`
2. `contracts/openapi/domains/users/`:
   * `users.yaml`: `GET /users`, `POST /users`, `GET /users/{id}`, `PUT /users/{id}`, `DELETE /users/{id}`
   * `schemas.yaml`: `UserRole`, `UserResponse`, `UserCreateRequest`, `UserUpdateRequest`
3. Registro em `contracts/openapi/openapi.yaml`:
   * Adicionar tags `Auth` e `Users`.
   * Adicionar paths correspondentes.

### B. Backend (Spring Boot 4.1 / Java 25)
1. Geração de stubs via `./scripts/generate-api.sh`:
   * Interfaces `AuthApi` e `UsersApi`.
   * DTOs em `br.dev.xb.isperp.api.dto.*`.
2. Criação do MapStruct `UserMapper.java`:
   * `toResponse(User user)`
   * `toResponseList(List<User> users)`
   * `toEntity(UserCreateRequest request)`
   * `updateEntityFromRequest(UserUpdateRequest request, @MappingTarget User user)`
3. Refatoração de `AuthController.java`:
   * Implementar `AuthApi`.
4. Refatoração de `UserController.java`:
   * Implementar `UsersApi`.
   * Substituir `@Autowired` por `@RequiredArgsConstructor`.
5. Testes Unitários MockMvc:
   * `AuthControllerTest.java`: login com sucesso e credenciais inválidas.
   * `UserControllerTest.java`: listagem, busca por ID, criação, atualização e exclusão, além de teste assertivo garantindo que o hash de senha não é serializado.

### C. Frontend (React 19 / Orval)
1. Refatoração de `authService.ts`:
   * Utilizar cliente gerado pelo Orval (`getAuth()`).
2. Refatoração de `userService.ts`:
   * Utilizar cliente gerado pelo Orval (`getUsers()`).
3. Validação de tipagem e build:
   * `npm run typecheck`
   * `npm run build`

### D. Validação Live no Docker Compose
* Recompilar containers e validar chamadas live autenticadas.
