# DIRETRIZ MANDATÓRIA: CULTURA DE QUALIDADE, SEGURANÇA (OWASP/SCA) E ZERO DUPLICIDADE

Esta diretriz consolida os padrões de engenharia atingidos na certificação nota **A (100%)** do projeto **ispERP** (`0 Bugs`, `0 Vulnerabilities`, `0 Code Smells`, `0 Security Hotspots`, `0 Dependency Risks` e `0.0% Duplications`) para que toda evolução futura mantenha rigorosamente o mesmo nível técnico, independentemente de ferramentas externas pagas.

---

## 1. Segurança de Código em Primeiro Lugar (Security by Design)

1. **Prevenção de Log Injection (`CWE-117` / `javasecurity:S5145`):**
   - **NUNCA** registre diretamente em logs (`log.info`, `log.warn`, `log.error`) valores vindos de parâmetros HTTP, headers, payloads ou banco sem sanitizar quebras de linha (`\r`, `\n`).
   - Sempre sanitize entradas externas antes do log (`valor.replaceAll("[\\r\\n]", "_")`) ou logue apenas identificadores internos confiáveis (`UUID`).

2. **Prevenção de Path Traversal (`CWE-22` / `javasecurity:S2083`) e SSRF (`CWE-918` / `javasecurity:S5144`):**
   - Ao manipular arquivos locais, sempre normalize o caminho e valide a fronteira do diretório base:
     ```java
     Path target = baseDir.resolve(fileName).normalize();
     if (!target.startsWith(baseDir)) {
         throw new SecurityException("Acesso fora do diretório permitido");
     }
     ```
   - Ao validar URLs ou endpoints externos (ex.: S3/Webhooks), bloqueie esquemas não-HTTP(S), endereços de loopback (`127.0.0.1`, `localhost`, `::1`), link-local/metadata (`169.254.169.254`) e faixas privadas RFC 1918 quando não autorizadas explicitamente.

3. **Criptografia e Assinaturas Seguras:**
   - Criptografia simétrica exclusivamente com **AES-256-GCM** (`AES/GCM/NoPadding`), IV aleatório de 12 bytes gerado via `SecureRandom` e `GCMParameterSpec(128, iv)`.
   - Derivação de chaves com **PBKDF2WithHmacSHA256** (mínimo de `600.000` iterações em produção).
   - Validação de assinaturas de Webhooks (HMAC-SHA256) sempre em tempo constante via `MessageDigest.isEqual(...)` para prevenir ataques de timing.

4. **Isolamento Multi-Tenant e Proteção contra Mass Assignment:**
   - Toda consulta e mutação de domínio deve filtrar estritamente pelo `companyId` do `TenantContext`.
   - **Proibido** expor ou receber entidades JPA (`@Entity`) em Controllers; utilize exclusivamente os DTOs do contrato OpenAPI.
   - Todos os identificadores primários devem utilizar **UUIDv7** (`UuidCreatorUtils.v7()`).

---

## 2. Gestão de Dependências e SCA (Zero Dependency Risks)

1. **Controle Explícito de Versões Seguras no `backend/build.gradle`:**
   - Sempre que atualizar o Spring Boot ou adicionar uma biblioteca, verifique a árvore de dependências com:
     ```bash
     cd backend && ./gradlew dependencies
     ```
   - Mantenha as versões seguras alinhadas simultaneamente no bloco `ext` (propriedades do BOM do Spring Dependency Management) **e** no `resolutionStrategy.eachDependency`.
   - **Atenção:** Nunca fixe uma versão em `resolutionStrategy` sem atualizar a propriedade correspondente no `ext` (ex.: `jackson-2-bom.version`, `jackson-bom.version`, `logback.version`, `log4j2.version`, `tomcat.version`, `postgresql.version`), evitando downgrades acidentais de transitivas.

2. **Redução de Superfície de Ataque (Exclusão de Transitivas Não Utilizadas):**
   - Exclua bibliotecas transitivas opcionais que não são utilizadas em runtime pelo projeto (ex.: `org.hdrhistogram:HdrHistogram`, `org.glassfish.jaxb:jaxb-runtime`, `org.glassfish.jaxb:jaxb-core` ou transitivas legadas do Jackson trazidas por `flyway` e `jjwt-jackson`).
   - Após qualquer exclusão ou upgrade, execute obrigatoriamente a suíte completa de testes unitários e de integração (`./gradlew test jacocoTestReport`).

---

## 3. Tolerância Zero a Duplicidade de Código (`0.0% Duplications`)

1. **Entidades JPA com Estrutura Compartilhada:**
   - Quando duas ou mais entidades compartilharem o mesmo conjunto de campos (ex.: itens de ordem de serviço ou tabelas espelho), extraia uma superclasse `@MappedSuperclass` (como `BaseInstallationOrderItem`) em vez de copiar campos, getters e callbacks `@PrePersist`.

2. **Mappers MapStruct sem Repetição de `@Mapping`:**
   - **PROIBIDO** repetir listas de `@Mapping(target = "id", ignore = true)`, `@Mapping(target = "companyId", ignore = true)`, `@Mapping(target = "createdAt", ignore = true)` em múltiplos métodos.
   - Utilize sempre as meta-anotações centralizadas em `CommonIgnoreMappings` (`@IgnoreAuditAndTenant`, `@IgnoreIdAuditAndTenant`) ou crie uma meta-anotação `@Retention(RetentionPolicy.CLASS)` dedicada.

3. **Services, Controllers e Construtor de DTOs:**
   - Nunca copie e cole blocos de `.builder()...build()`, validações de tenant ou conversões de resposta entre métodos ou classes. Extraia métodos auxiliares privados ou componentes compartilhados a partir da **segunda** ocorrência.

---

## 4. Qualidade de Código e Manutenibilidade (`0 Bugs`, `0 Code Smells`)

1. **Complexidade Cognitiva (`<= 15` por método — `java:S3776`):**
   - Métodos com múltiplos `if/else`, loops aninhados ou blocos `try/catch` devem ser decompostos em métodos privados pequenos e coesos com nomes descritivos.
   - Prefira *early returns* (guard clauses) para evitar aninhamento profundo.

2. **Limite de Parâmetros (`<= 7` parâmetros — `java:S107`):**
   - Nenhum método ou construtor deve receber mais de 7 parâmetros. Agrupe parâmetros coesos em um `record` de comando/contexto ou DTO.

3. **Exceções Específicas (`java:S112`) e Tratamento Limpo:**
   - **PROIBIDO** lançar `new RuntimeException(...)` ou `new Exception(...)` diretamente no código de produção.
   - Utilize exceções de domínio semânticas (`BusinessRuleException`, `ResourceNotFoundException`, `StorageIntegrationException`, `IllegalArgumentException`, `IllegalStateException`).

4. **Constantes para Literais Repetidos (`java:S1192`):**
   - Qualquer literal de String repetido **3 ou mais vezes** na mesma classe (nomes de campos, mensagens de erro, chaves JSON, cabeçalhos) deve ser extraído para uma constante `private static final String`.

5. **Idiomas Modernos do Java 25 LTS:**
   - Utilize `stream().toList()` (imutável) em vez de `Collectors.toList()`.
   - Utilize *Pattern Matching for `instanceof`* (`if (obj instanceof MeuTipo variavel)`) e *Switch Expressions* (`switch (...) { case X -> ... }`).
   - Utilize sempre `try-with-resources` para qualquer `AutoCloseable` (`Connection`, `PreparedStatement`, `ResultSet`, `InputStream`, `OutputStream`).
   - Respeite estritamente o `@NullMarked` (JSpecify) em todos os pacotes.

---

## 5. Cobertura de Testes Obrigatória (`>= 80%` em Código Novo)

- Toda nova feature, correção de bug ou refatoração deve vir acompanhada de testes unitários e/ou de integração cobrindo os caminhos felizes e os desvios de erro/segurança.
- Antes de concluir qualquer entrega no Backend, valide localmente o relatório do JaCoCo:
  ```bash
  cd backend && ./gradlew test jacocoTestReport
  ```
  *(O relatório visual gratuito fica disponível em `backend/build/reports/jacoco/test/html/index.html`).*
