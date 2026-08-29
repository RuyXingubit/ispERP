# Plano de Implementação - Milestone 1: PostgreSQL 17+, UUIDv7 & Base de Testes

Este documento detalha o plano de execução imediata da **Milestone 1** do **ispERP**, servindo de guia passo a passo para a retomada do desenvolvimento.

---

## 1. Visão Geral da Milestone 1

A Milestone 1 foca em estabelecer uma base técnica sólida, moderna e segura para suportar a arquitetura orientada a eventos (EDA):
1. Migração do banco de dados relacional de **MySQL** para **PostgreSQL 17+** (usando imagem `postgres:17-alpine`).
2. Padronização de todas as entidades e tabelas para **UUIDv7** (RFC 9562), com geração nativa no banco (`DEFAULT uuidv7()`) e na JVM (`com.github.f4b6a3:uuid-creator`).
3. Reestruturação das migrações do **Flyway**.
4. Criação da suíte de **testes automatizados (TDD)** no backend com **JUnit 5**, **Mockito** e **Testcontainers PostgreSQL**.

---

## 2. Tarefas e Arquivos a Serem Modificados

### 2.1. Configurações de Dependências e Containers
- [ ] **[backend/build.gradle](file:///Users/ruy/Code/ispERP/backend/build.gradle):**
  - Remover driver MySQL e dependências Flyway MySQL.
  - Adicionar driver PostgreSQL: `org.postgresql:postgresql:42.7.2`.
  - Adicionar Flyway PostgreSQL: `org.flywaydb:flyway-database-postgresql`.
  - Adicionar biblioteca UUIDv7: `com.github.f4b6a3:uuid-creator:6.0.0`.
  - Adicionar Testcontainers PostgreSQL: `org.testcontainers:postgresql`.
- [ ] **[docker-compose.yml](file:///Users/ruy/Code/ispERP/docker-compose.yml) & [docker-compose.prod.yml](file:///Users/ruy/Code/ispERP/docker-compose.prod.yml):**
  - Trocar container `mysql:8.0` por `postgres:17-alpine`.
  - Ajustar variáveis (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, porta `5432`).
  - Atualizar backend para conectar na porta `5432`.
- [ ] **[backend/src/main/resources/application.yml](file:///Users/ruy/Code/ispERP/backend/src/main/resources/application.yml):**
  - Atualizar `driver-class-name: org.postgresql.Driver`.
  - Atualizar dialeto Hibernate para `org.hibernate.dialect.PostgreSQLDialect`.
  - Atualizar porta padrão para `5432`.

### 2.2. Migrações Flyway (PostgreSQL 17 + UUIDv7)
- [ ] **[backend/src/main/resources/db/migration/V1__Create_initial_tables.sql](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/migration/V1__Create_initial_tables.sql):**
  - Criar tabelas `users`, `companies`, `site_settings` usando `id UUID PRIMARY KEY DEFAULT uuidv7()`.
- [ ] **[backend/src/main/resources/db/migration/V2__Create_customers_table.sql](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/migration/V2__Create_customers_table.sql):**
  - Criar tabela `customers` com `id UUID PRIMARY KEY DEFAULT uuidv7()`.
- [ ] **[backend/src/main/resources/db/migration/V3__Create_outbox_events_table.sql](file:///Users/ruy/Code/ispERP/backend/src/main/resources/db/migration/V3__Create_outbox_events_table.sql):**
  - Criar tabela `outbox_events` (`id UUID PRIMARY KEY`, `event_type VARCHAR`, `payload JSONB`, `status VARCHAR`, `created_at TIMESTAMP`) e `processed_events` (`event_id UUID PRIMARY KEY`, `processed_at TIMESTAMP`).

### 2.3. Entidades JPA, Repositórios e Utilitários
- [x] **[backend/src/main/java/br/dev/xb/isperp/util/UuidCreatorUtils.java](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/util/UuidCreatorUtils.java):**
  - Implementar método para gerar UUIDv7 com base em tempo via biblioteca `uuid-creator`.
- [x] **Refatoração de Entidades:**
  - Atualizar [Customer.java](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/entity/Customer.java), [User.java](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/entity/User.java), [Company.java](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/entity/Company.java) e [SiteSettings.java](file:///Users/ruy/Code/ispERP/backend/src/main/java/br/dev/xb/isperp/entity/SiteSettings.java) para usar `UUID id` e `@PrePersist` para inicialização segura.
- [x] **Refatoração de Repositories, Services e Controllers:**
  - Atualizar métodos para receber e buscar por `UUID` em vez de `Long`.
  - Consolidar pacote canônico sob `br.dev.xb.isperp`.
- [x] **Suíte de Testes:**
  - Criar estrutura `backend/src/test/java/br/dev/xb/isperp/`.
- [ ] Criar testes unitários do serviço de clientes com validação de CPF e integridade: `CustomerServiceTest.java`.
- [ ] Criar testes de integração com Testcontainers: `CustomerRepositoryTest.java`.
- [ ] Criar testes do controlador REST: `CustomerControllerTest.java`.

---

## 3. Critérios de Aceite e Verificação da Milestone 1

1. **Compilação e Testes:** Executar `./gradlew test` com 100% de sucesso.
2. **Flyway Migrations:** Executar migrações do Flyway contra PostgreSQL 17 sem erros de sintaxe ou tipos.
3. **Validação de UUIDv7:** Garantir que todos os registros criados possuam IDs UUID versão 7 ordenados cronologicamente.
4. **Execução em Container:** Validar que `docker-compose up -d` inicializa o PostgreSQL 17, executa as migrações e sobe o backend de forma saudável.
