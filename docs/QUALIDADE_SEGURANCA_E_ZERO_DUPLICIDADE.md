# Manual de Cultura de Engenharia: Segurança, Qualidade de Código e Zero Duplicidade

Este documento consolida as práticas, padrões arquiteturais e guardiões automatizados que levaram o **ispERP** ao patamar máximo de qualidade e segurança auditada (**Rating A em todas as dimensões**: `0 Bugs`, `0 Vulnerabilities`, `0 Security Hotspots`, `0 Code Smells`, `0 Dependency Risks` e `0.0% Duplicated Lines`).

O objetivo deste guia é **perenizar essa cultura de engenharia no projeto**, garantindo que o código continue seguro, limpo, testado e sem duplicidades de forma autônoma — independentemente do uso ou não de licenças comerciais do SonarQube Cloud no futuro.

---

## 1. Segurança em Primeiro Lugar (Security by Design & OWASP)

Qualquer alteração proposta no sistema deve passar por uma pergunta fundamental:
> *"Esta mudança aumenta ou diminui a segurança do sistema?"*
Nenhuma conveniência de código justifica abrir mão de segurança.

### 1.1 Prevenção de Log Injection (`CWE-117`)
Atacantes podem enviar caracteres de quebra de linha (`\r`, `\n`) em campos de requisição (headers, query params, payloads de webhook) para forjar entradas falsas nos arquivos de log e confundir auditorias.
- **Regra:** Nunca passe valores controlados pelo usuário diretamente para `log.info / warn / error`.
- **Padrão adotado:**
  ```java
  private String sanitizeForLog(@Nullable String input) {
      if (input == null) {
          return "null";
      }
      return input.replaceAll("[\\r\\n]", "_");
  }
  ```

### 1.2 Prevenção de Path Traversal (`CWE-22`) e SSRF (`CWE-918`)
- **Path Traversal:** Qualquer operação em disco (ex.: diretórios de spool/staging de backups ou templates) deve normalizar o `Path` e verificar se ele permanece estritamente dentro do diretório base autorizado:
  ```java
  Path resolved = baseDirectory.resolve(fileName).normalize();
  if (!resolved.startsWith(baseDirectory)) {
      throw new SecurityException("Tentativa de Path Traversal bloqueada");
  }
  ```
- **SSRF (Server-Side Request Forgery):** Ao permitir configuração de endpoints externos (ex.: S3 customizado, Webhooks), valide o protocolo (`https` ou `http` explícito) e bloqueie chamadas para loopback (`127.0.0.1`, `localhost`, `::1`), link-local / cloud metadata (`169.254.169.254`) e redes internas não autorizadas.

### 1.3 Criptografia Forte e Comparação em Tempo Constante
- **AES-256-GCM:** Criptografia de backups e segredos utiliza exclusivamente `AES/GCM/NoPadding` com IV aleatório de 12 bytes gerado via `SecureRandom` e tag de autenticação de 128 bits (`GCMParameterSpec(128, iv)`).
- **PBKDF2:** Derivação de chaves a partir de senha mestre utiliza `PBKDF2WithHmacSHA256` com sal aleatório e mínimo de `600.000` iterações.
- **Webhooks / HMAC:** Comparação de assinaturas criptográficas (`X-Webhook-Signature`) utiliza sempre `MessageDigest.isEqual(expectedBytes, actualBytes)` para impedir ataques de *timing side-channel*.

### 1.4 Isolamento Multi-Tenant, UUIDv7 Estrito e Blindagem de DTOs
- **Isolamento por Empresa (`companyId`):** Toda operação de leitura ou escrita deve validar o `companyId` extraído do `TenantContext` autenticado.
- **Proibição de `@Entity` nas Rotas:** Controllers recebem e retornam exclusivamente DTOs gerados pelo contrato OpenAPI (`contracts/openapi/`), eliminando vulnerabilidades de *Mass Assignment* e vazamento de colunas internas.
- **UUIDv7 Estrito (RFC 9562):** É proibido utilizar `UUID.randomUUID()` (UUIDv4) no código de produção (`src/main/java`). Todos os identificadores são gerados via `UuidCreatorUtils.generateUuidV7()` ou `UuidCreatorUtils.orGenerate(id)`, garantindo ordenação temporal nativa em índices B-Tree do PostgreSQL 17 sem perda de entropia.

### 1.5 Prevenção de Vazamento de Segredos (*Secret Scanning*)
- **Zero Segredos Hardcoded:** Chaves de API, segredos JWT, senhas de banco de dados ou credenciais AWS S3 / Cloudflare R2 nunca devem ser escritas diretamente no código-fonte ou em arquivos `.yml` / `.properties` versionados.
- Utilize sempre variáveis de ambiente injetadas em runtime (`${JWT_SECRET}`, `${DB_PASSWORD}`) e em testes unitários utilize constantes claramente identificadas como fictícias (`TEST_ONLY_...`).

---

## 2. Gestão de Dependências, SCA e Licenças (Zero Dependency Risks)

Vulnerabilidades em bibliotecas de terceiros (*Software Composition Analysis — SCA*) representam um dos maiores vetores de risco em aplicações modernas.

### 2.1 Alinhamento entre `ext` (Spring BOM) e `resolutionStrategy`
No arquivo `backend/build.gradle`, versões corrigidas de bibliotecas transitivas devem ser declaradas de forma coordenada em dois lugares:
1. No bloco `ext { ... }` (que sobrescreve as propriedades oficiais do BOM do `io.spring.dependency-management`).
2. No bloco `configurations.configureEach { resolutionStrategy.eachDependency { ... } }` (que garante que dependências fora do BOM do Spring também usem a versão segura).

> **Armadilha a evitar:** Nunca fixe uma versão antiga em `resolutionStrategy` nem sobrescreva `spring-framework.version` abaixo da versão nativa do Spring Boot atual. Sempre que atualizar o Spring Boot, confira se os pins em `ext` e `resolutionStrategy` são iguais ou superiores aos do novo BOM.

### 2.2 Redução de Superfície de Ataque (Exclusão de Transitivas Não Usadas)
Bibliotecas grandes (como Hibernate, Micrometer, Flyway e JJWT) costumam trazer dependências transitivas opcionais que o projeto não utiliza em tempo de execução:
- `org.hdrhistogram:HdrHistogram` (usado apenas se histogramas de percentil específicos forem habilitados no Micrometer).
- `org.glassfish.jaxb:jaxb-runtime` e `jaxb-core` (usados apenas para mapeamento XML legado `orm.xml`/`hbm.xml` no Hibernate, desnecessário quando usamos 100% anotações `@Entity`).
- Versões antigas transitivas do Jackson trazidas por `flyway-core` ou `jjwt-jackson`.

Manter essas exclusões no `backend/build.gradle` reduz o tamanho do artefato e elimina superfície de ataque desnecessária (como parsers XML sujeitos a XXE).

### 2.3 Conformidade Jurídica de Licenças (*License Compliance*)
O **ispERP** adota uma arquitetura **Multi-License**:
- `backend/` e `frontend-flutter/` são licenciados sob **GNU AGPLv3**.
- `frontend-customer/` (Central do Assinante) e `frontend-site/` (Site Institucional) são licenciados sob **MIT**.
- **Regra Mandatória:** Nunca adicione bibliotecas com licenças copyleft (`GPL`, `AGPL`, `LGPL`) no `package.json` de `frontend-customer` ou `frontend-site`. Nesses dois projetos, utilize apenas dependências com licenças permissivas (`MIT`, `Apache-2.0`, `BSD-2-Clause`, `BSD-3-Clause`, `ISC`).

### 2.4 Como Auditar Dependências Gratuitamente (Sem SonarQube)
Mesmo sem licença do SonarQube Cloud, você possui **3 ferramentas gratuitas** para manter `0 Dependency Risks`:
1. **GitHub Dependabot (Ativo no repositório):** Monitora automaticamente o `build.gradle` e os `package-lock.json` na aba **Security -> Dependabot alerts** do GitHub.
2. **Inspeção de Árvore do Gradle:**
   ```bash
   cd backend
   ./gradlew dependencies --configuration runtimeClasspath
   ./gradlew dependencyInsight --dependency <nome-do-pacote> --configuration runtimeClasspath
   ```
3. **OSV-Scanner (Google Open Source Vulnerabilities — Gratuito):**
   Pode ser rodado localmente a qualquer momento (`osv-scanner scan source -r .`) para cruzar todas as versões do Gradle/NPM com o banco global de CVEs/GHSAs.

---

## 3. Tolerância Zero a Duplicidade (`0.0% Duplications`)

Código duplicado multiplica o esforço de manutenção e faz com que correções de bugs sejam aplicadas em um lugar e esquecidas em outro.

### 3.1 Herança `@MappedSuperclass` para Entidades Semelhantes
Quando duas entidades JPA possuem colunas estruturalmente idênticas (ex.: `InstallationMaterialDemand` e `InstallationReturnNotice`), **não copie os campos**. Extraia uma classe base anotada com `@MappedSuperclass` (ex.: `BaseInstallationOrderItem`) contendo os campos, relacionamentos e `@PrePersist` compartilhados.

### 3.2 Meta-Anotações no MapStruct (`CommonIgnoreMappings`)
Repetir dezenas de linhas `@Mapping(target = "id", ignore = true)`, `@Mapping(target = "companyId", ignore = true)`, `@Mapping(target = "createdAt", ignore = true)` em várias interfaces MapStruct gera alerta imediato de duplicação.
- **Solução padrão do projeto:** Utilizar as meta-anotações compostas em `br.dev.xb.isperp.mapper.CommonIgnoreMappings`:
  - `@IgnoreAuditAndTenant`: ignora `companyId`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`.
  - `@IgnoreIdAuditAndTenant`: inclui `@IgnoreAuditAndTenant` + ignora `id`.

### 3.3 Extração de Métodos Auxiliares e Mappers
- Nunca copie blocos longos de `.builder()...build()` entre métodos de um Service ou Controller.
- Regra da **Segunda Ocorrência**: precisou escrever o mesmo bloco de montagem de DTO, validação de permissão ou cálculo financeiro pela segunda vez? Extraia imediatamente para um método privado ou componente compartilhado.

---

## 4. Qualidade de Código, Spring, Hibernate e Virtual Threads (`0 Bugs`, `0 Code Smells`)

### 4.1 Regras Gerais de Código Limpo (Java 25 LTS)

| Regra | Limite / Padrão | Como aplicar na prática |
| :--- | :--- | :--- |
| **Complexidade Cognitiva** (`java:S3776`) | Máximo de **15** por método | Use *guard clauses* (`if (...) return;`) no início do método e extraia blocos internos de loops e `try/catch` para métodos privados com nomes claros. |
| **Número de Parâmetros** (`java:S107`) | Máximo de **7** por método/construtor | Se um método precisar de mais de 7 dados, agrupe-os em um `record` imutável de comando/contexto. |
| **Exceções Específicas** (`java:S112`) | Zero `RuntimeException` / `Exception` genéricas | Lance sempre exceções de domínio (`BusinessException`, `BusinessRuleException`, `ResourceNotFoundException`) ou do JDK (`IllegalArgumentException`, `IllegalStateException`). |
| **Literais Duplicados** (`java:S1192`) | Máximo de **2** repetições | Se uma mesma String aparecer **3 ou mais vezes** na classe, transforme-a em `private static final String`. |
| **Recursos I/O, S3 e JDBC** (`java:S2095`) | `try-with-resources` obrigatório | Todo `S3Client`, `Connection`, `PreparedStatement`, `ResultSet`, `InputStream` ou `OutputStream` temporário deve ser aberto dentro de um bloco `try (...)`. |
| **Modernização Java 25 & Null Safety** | JSpecify `@NullMarked` + Java 25 | Use `stream().toList()`, `instanceof Tipo var`, Switch Expressions (`->`), e anote pacotes com `@NullMarked` e parâmetros opcionais com `@Nullable`. |

### 4.2 Armadilhas Específicas de Spring, Hibernate e Virtual Threads
1. **Proibição de Self-Invocation em `@Transactional` e `@Async`:**
   No Spring, anotações como `@Transactional` e `@Async` funcionam via proxies AOP. Chamar `this.metodoTransacional()` de dentro da mesma classe **ignora o proxy** (a transação não é aberta ou o método roda de forma síncrona). Extraia o método para um componente auxiliar ou injete o serviço adequadamente.
2. **Limpeza Obrigatória de `ThreadLocal` (`TenantContext` e `MDC`) em `finally`:**
   Com o uso de **Virtual Threads no Java 25**, qualquer filtro HTTP ou executor que defina `TenantContext.setCurrentTenant(...)` ou `MDC.put(...)` deve obrigatoriamente chamar `.clear()` dentro de um bloco `finally` para impedir vazamento de contexto entre requisições.
3. **Prevenção de `N+1` e Paginação em Tabelas Grandes:**
   Nunca execute `findAll()` sem paginação (`Pageable`) em tabelas de crescimento contínuo (`invoices`, `domain_events`, `audit_logs`, `radacct`). Utilize consultas paginadas ou projeções enxutas.

---

## 5. Guardiões Automatizados Offline (Substitutos Gratuitos do SonarQube)

Para que a cultura de qualidade nunca dependa de uma licença ativa do SonarQube Cloud, o projeto conta com **3 mecanismos automatizados e 100% gratuitos** integrados ao Gradle:

### 5.1 Testes de Arquitetura com ArchUnit (`ArchitectureAndQualityRulesTest`)
Localizado em `backend/src/test/java/br/dev/xb/isperp/architecture/ArchitectureAndQualityRulesTest.java`, este teste roda automaticamente em menos de 2 segundos durante todo `./gradlew test` e **quebra o build imediatamente** se alguém:
1. Chamar `UUID.randomUUID()` (UUIDv4) em qualquer classe de produção (`src/main/java`);
2. Utilizar `System.out`, `System.err` ou `printStackTrace()` em vez do SLF4J (`@Slf4j`);
3. Acoplar classes de `entity`, `repository` ou `service` à camada `controller`;
4. Criar uma entidade JPA de domínio cujo `@Id` não seja `java.util.UUID`;
5. Retornar uma `@Entity` diretamente em um endpoint de `@RestController`;
6. Remover o `@NullMarked` (JSpecify) dos pacotes principais do Backend.

### 5.2 Trava Automática e Relatório Visual de Cobertura (JaCoCo)
A meta contínua é manter **no mínimo 80% de cobertura em todo código novo**, além da trava automática (`jacocoTestCoverageVerification`) configurada no `backend/build.gradle`:
```bash
cd backend
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```
- O relatório HTML interativo gratuito é gerado em:
  `backend/build/reports/jacoco/test/html/index.html`

### 5.3 Comando Unificado de Qualidade Offline (`./gradlew qualityCheck`)
Sempre que quiser rodar uma bateria completa de auditoria local (Testes + ArchUnit + Trava de Cobertura JaCoCo + Análise Estática PMD 7 configurada em `backend/config/pmd/ruleset.xml`), execute:
```bash
cd backend
./gradlew qualityCheck
```
- Relatório visual do PMD: `backend/build/reports/pmd/main.html`
- Relatório visual do JaCoCo: `backend/build/reports/jacoco/test/html/index.html`

---

## 6. Checklist Rápido antes de cada Commit / Feature

- [ ] **Segurança:** A mudança mantém ou aumenta a segurança? Entradas externas estão validadas, logs sanitizados contra CRLF e zero segredos hardcoded?
- [ ] **Isolamento & UUIDv7:** As queries respeitam o `companyId` do `TenantContext` e todos os IDs usam `UuidCreatorUtils.generateUuidV7()`?
- [ ] **Contrato API-First:** Novas rotas ou campos nasceram primeiro em `contracts/openapi/` e foram gerados via `./scripts/generate-api.sh`?
- [ ] **Zero Duplicidade:** Usei `@IgnoreIdAuditAndTenant` / `@MappedSuperclass` / métodos auxiliares em vez de copiar e colar blocos?
- [ ] **Código Limpo & Spring:** Métodos têm complexidade cognitiva `<= 15`, no máximo `7` parâmetros, `try-with-resources` em recursos I/O/S3 e zero self-invocation de `@Transactional`?
- [ ] **Dependências & Licenças:** Se adicionei biblioteca nova, verifiquei `./gradlew dependencies` (zero CVEs) e a compatibilidade da licença (`MIT`/`Apache-2.0`/`BSD` nos frontends MIT)?
- [ ] **Testes & Guardiões:** Criei testes unitários cobrindo `>= 80%` do código novo e validei com `./gradlew test jacocoTestReport` (incluindo o `ArchitectureAndQualityRulesTest`)?
