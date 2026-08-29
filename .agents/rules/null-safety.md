# Null Safety com JSpecify (@NullMarked e @Nullable)

## Regra Global
Este projeto adota a especificação padrão **JSpecify** (`org.jspecify.annotations.*`), o padrão oficial do Spring Boot 4 / Spring Framework 7.
Todos os packages Java utilizam `@NullMarked` via `package-info.java`.
Isso significa que **todos os tipos, parâmetros, retornos e campos dentro do package são NON-NULL por padrão**.

---

## Consequências Práticas

### ❌ NÃO faça
```java
// @NonNull é redundante — @NullMarked no package já garante que é non-null
public Customer getById(@NonNull UUID id) { ... }
```

### ✅ Faça
```java
// Sem anotação = NON-NULL por padrão (via @NullMarked)
public Customer getById(UUID id) { ... }

// Use @Nullable (org.jspecify.annotations.Nullable) APENAS quando aceitar null legitimamente
public void close(UUID id, @Nullable String notes) { ... }
```

---

## Regras

1. **Nunca adicione `@NonNull`** em parâmetros ou retornos — é redundante com `@NullMarked`.
2. **Use `@Nullable` (`org.jspecify.annotations.Nullable`) explicitamente** quando um valor pode ser `null` (ex: campos opcionais, ratings, notas, parâmetros de consulta opcionais).
3. **Novos packages DEVEM ter `package-info.java`** com `@NullMarked`:
   ```java
   @NullMarked
   package br.dev.xb.isperp.novo.pacote;

   import org.jspecify.annotations.NullMarked;
   ```
4. **Retornos de bibliotecas externas** (ex: `UuidCreator`) devem ser envoltos com `Objects.requireNonNull()`.
5. **Dados de auditoria (customerId, contractId, etc.) NUNCA devem ser null** — ISPs são obrigados a manter rastreabilidade por exigência regulatória (Anatel/processos judiciais).

---

## Prevenção de Warnings com @NullMarked

### Causa 1: JDK/Library false-positives
APIs do JDK (`LocalDateTime.now()`, `UUID.randomUUID()`, `String.format()`, `BigDecimal.ZERO`) e Spring (`MediaType.APPLICATION_JSON`, `ResponseEntity.notFound().build()`) podem não possuir anotações de nulidade no compilador Eclipse/IDE.

**Solução**: Usar `@SuppressWarnings("null")`:
- **Nível de CLASSE** — para classes com muitas chamadas JDK/Spring (services, controllers, consumers, gateways)
- **Nível de MÉTODO** — para classes com poucas chamadas, mantendo a análise ativa no restante

```java
// Classe com muitas chamadas JDK → nível de classe
@Service
@SuppressWarnings("null")
public class InvoiceService { ... }

// Classe com poucas chamadas → nível de método
public class MyService {
    @SuppressWarnings("null")
    public void metodoComJDK() {
        LocalDateTime now = LocalDateTime.now();
    }
}
```

### Causa 2: Entity fields nullable
Campos de entidade JPA que podem ser null (`nullable = true` ou sem `nullable = false`) DEVEM ser anotados com `@Nullable`:
```java
@Column(name = "paid_at")
@Nullable
private LocalDateTime paidAt; // null até o pagamento

@Column(name = "notes", columnDefinition = "text")
@Nullable
private String notes; // campo opcional
```

### Causa 3: Implementação de interfaces externas (ConstraintValidator, WebMvcConfigurer)
Interfaces de libraries externas não declaram `@NonNull` em seus parâmetros. Ao implementar métodos de interfaces de terceiros que aceitam null, adicione `@Nullable`:
```java
@Override
public void initialize(@Nullable ValidCpf constraintAnnotation) { ... }

@Override
public boolean isValid(@Nullable String cpf, @Nullable ConstraintValidatorContext context) { ... }
```

### Causa 4: Código em testes
Testes usam Mockito/JUnit cujas APIs não possuem anotações `@NonNull`. **Toda classe de teste DEVE ter `@SuppressWarnings("null")`**:
```java
@SuppressWarnings("null")
class InvoiceServiceTest {
    // UUID.randomUUID(), Mockito.when().thenReturn(), etc.
}
```

### Causa 5: Map.get() e dados de payload
`Map.get()` pode retornar null. NUNCA suprimir com `@SuppressWarnings`. Sempre validar:
```java
// ❌ NÃO suprima
@SuppressWarnings("null")
String customerId = (String) data.get("customerId");

// ✅ FAÇA — valide explicitamente
String customerIdStr = (String) data.get("customerId");
Objects.requireNonNull(customerIdStr, "customerId é obrigatório no payload");
UUID customerId = UUID.fromString(customerIdStr);
```

### Causa 6: Dead Code por null checks defensivos
Quando um parâmetro é non-null via `@NullMarked`, um `if (param == null)` pode gerar alerta de dead code.
- Se o null check é **programação defensiva legítima** (API pública, métodos utilitários) → adicione `@Nullable` no parâmetro
- Se o null check é **realmente impossível** → remova o null check redundante

```java
// ✅ Correto: marca como @Nullable pois é API pública e aceita null defensivamente
public static @Nullable String clean(@Nullable String cpf) {
    if (cpf == null) return null;
}
```

### Causa 7: @RequestParam(required = false) e @RequestHeader(required = false)
Parâmetros Spring `@RequestParam(required = false)` podem receber `null`, mas o `@NullMarked` diz o contrário. **SEMPRE adicione `@Nullable`**:
```java
@GetMapping
public ResponseEntity<List<Invoice>> getAll(
        @RequestParam(required = false) @Nullable InvoiceStatus status) { ... }
```

---

## Checklist para Novo Código
- [ ] O package tem `package-info.java` com `@NullMarked` (`org.jspecify.annotations.NullMarked`)?
- [ ] Nenhum `@NonNull` redundante foi adicionado?
- [ ] Parâmetros opcionais estão marcados com `@Nullable` (`org.jspecify.annotations.Nullable`)?
- [ ] Campos de entidade nullable estão marcados com `@Nullable`?
- [ ] Classes com chamadas JDK têm `@SuppressWarnings("null")` (classe ou método)?
- [ ] Classes de teste têm `@SuppressWarnings("null")`?
- [ ] `@RequestParam(required = false)` tem `@Nullable`?
- [ ] Interfaces externas implementadas têm `@Nullable` nos params do override quando aplicável?
- [ ] Null checks defensivos usam `@Nullable` no parâmetro (não `@SuppressWarnings`)?
- [ ] Dados de auditoria (IDs de cliente/contrato) estão sempre preenchidos?
- [ ] `Map.get()` em payloads está sendo validado (não suprimido)?
