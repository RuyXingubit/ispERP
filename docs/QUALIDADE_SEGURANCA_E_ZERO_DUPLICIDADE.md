# Manual de Cultura de Engenharia: Segurança, Qualidade de Código e Zero Duplicidade

Este documento consolida as práticas, padrões arquiteturais e verificações que levaram o **ispERP** ao patamar máximo de qualidade e segurança auditada (**Rating A em todas as dimensões**: `0 Bugs`, `0 Vulnerabilities`, `0 Security Hotspots`, `0 Code Smells`, `0 Dependency Risks` e `0.0% Duplicated Lines`).

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

### 1.4 Isolamento Multi-Tenant, UUIDv7 e Blindagem de DTOs
- **Isolamento por Empresa (`companyId`):** Toda operação de leitura ou escrita deve validar o `companyId` extraído do `TenantContext` autenticado.
- **Proibição de `@Entity` nas Rotas:** Controllers recebem e retornam exclusivamente DTOs gerados pelo contrato OpenAPI (`contracts/openapi/`), eliminando vulnerabilidades de *Mass Assignment* e vazamento de colunas internas.
- **UUIDv7 (RFC 9562):** Todas as chaves primárias são geradas via `UuidCreatorUtils.v7()`, garantindo ordenação temporal nativa em índices B-Tree do PostgreSQL 17 sem perda de entropia.

---

## 2. Gestão de Dependências e SCA (Zero Dependency Risks)

Vulnerabilidades em bibliotecas de terceiros (*Software Composition Analysis — SCA*) representam um dos maiores vetores de risco em aplicações Java modernas.

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

Manter essas exclusões no `backend/build.gradle` reduz o tamanho do artefato e elimina falsos positivos e superfície de ataque (como parsers XML sujeitos a XXE).

### 2.3 Como Auditar Dependências Gratuitamente (Sem SonarQube)
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

## 4. Qualidade de Código e Manutenibilidade (`0 Bugs`, `0 Code Smells`)

Para manter o código com leitura fluida e livre de *Code Smells*, siga estas 6 regras em todo código escrito ou revisado:

| Regra | Limite / Padrão | Como aplicar na prática |
| :--- | :--- | :--- |
| **Complexidade Cognitiva** (`java:S3776`) | Máximo de **15** por método | Use *guard clauses* (`if (...) return;`) no início do método e extraia blocos internos de loops e `try/catch` para métodos privados com nomes claros. |
| **Número de Parâmetros** (`java:S107`) | Máximo de **7** por método/construtor | Se um método precisar de mais de 7 dados, agrupe-os em um `record` imutável de comando/contexto. |
| **Exceções Específicas** (`java:S112`) | Zero `RuntimeException` / `Exception` genéricas | Lance sempre exceções de domínio (`BusinessRuleException`, `ResourceNotFoundException`, `StorageIntegrationException`) ou do JDK (`IllegalArgumentException`, `IllegalStateException`). |
| **Literais Duplicados** (`java:S1192`) | Máximo de **2** repetições | Se uma mesma String aparecer **3 ou mais vezes** na classe, transforme-a em `private static final String`. |
| **Recursos I/O e JDBC** (`java:S2095`) | `try-with-resources` obrigatório | Todo `Connection`, `PreparedStatement`, `ResultSet`, `InputStream` ou `OutputStream` deve ser aberto dentro de um bloco `try (...)`. |
| **Modernização Java 25 & Null Safety** | JSpecify `@NullMarked` + Java 25 | Use `stream().toList()`, `instanceof Tipo var`, Switch Expressions (`->`), e anote pacotes com `@NullMarked` e parâmetros opcionais com `@Nullable`. |

---

## 5. Cobertura de Testes e Auditoria Local Gratuita

A regra de ouro para evitar regressões é manter **no mínimo 80% de cobertura de testes em todo código novo**.

### Como validar tudo localmente (100% Gratuito, sem SonarQube)
Antes de abrir um Pull Request ou fazer push na `main`, execute:

```bash
cd backend
./gradlew test jacocoTestReport
```

1. **Execução de Testes:** Valida todos os testes unitários, slices `@WebMvcTest` e testes de integração E2E com PostgreSQL 17 real via Testcontainers.
2. **Relatório Visual de Cobertura (JaCoCo):**
   Abra o arquivo gerado em:
   `backend/build/reports/jacoco/test/html/index.html`
   Ele exibe gratuitamente, pacote por pacote e linha por linha (em verde/amarelo/vermelho), quais ramos (`branches`) e instruções estão cobertos por testes, permitindo manter a meta de `>= 80%` permanentemente sem depender de ferramentas externas.

---

## 6. Checklist Rápido antes de cada Commit / Feature

- [ ] **Segurança:** A mudança mantém ou aumenta a segurança? Entradas externas estão validadas e logs sanitizados contra CRLF?
- [ ] **Isolamento:** As queries e operações respeitam o `companyId` do `TenantContext` e usam `UuidCreatorUtils.v7()`?
- [ ] **Contrato API-First:** Novas rotas ou campos nasceram primeiro em `contracts/openapi/` e foram gerados via `./scripts/generate-api.sh`?
- [ ] **Zero Duplicidade:** Usei `@IgnoreIdAuditAndTenant` / `@MappedSuperclass` / métodos auxiliares em vez de copiar e colar blocos?
- [ ] **Código Limpo:** Métodos têm complexidade cognitiva `<= 15`, no máximo `7` parâmetros, zero `RuntimeException` genérica e constantes para Strings repetidas 3+ vezes?
- [ ] **Dependências:** Se adicionei biblioteca nova, verifiquei `./gradlew dependencies` para garantir que não trouxe transitivas vulneráveis?
- [ ] **Testes Unitários:** Criei testes unitários cobrindo no mínimo 80% do código novo e validei com `./gradlew test jacocoTestReport`?
