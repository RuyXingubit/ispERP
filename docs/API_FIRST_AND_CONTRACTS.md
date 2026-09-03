# Arquitetura e Diretrizes: API-First e Contratos de API no ispERP
**Documento Oficial de Engenharia & Governança**  
**Data de Consolidação:** 2026-09-03  
**Status:** Oficial / Consolidado  
**Regra Vinculada:** [`.agents/rules/api-first-contracts.md`](file:///Users/ruy/Code/ispERP/.agents/rules/api-first-contracts.md)  

---

## 1. Visão Geral e Princípios Fundamentais

O **ispERP** adota oficialmente a estratégia **API-First (Design-First)** como padrão arquitetural para toda comunicação entre serviços, clientes e integrações externas.

Nesse modelo, a interface de comunicação não é tratada como um subproduto do código do backend, mas sim como um **ativo de engenharia de primeira classe**, versionado e estritamente tipado antes de qualquer linha de implementação.

### Objetivos Estratégicos de Longo Prazo
1. **Suporte Nativo a Múltiplos Clientes:** O mesmo contrato alimenta o Frontend Administrativo Web (React), o Portal do Assinante, o futuro Aplicativo Mobile dos Técnicos em Campo (Flutter ou React Native) e integrações de parceiros (Redes Neutras / Open Finance).
2. **Evolução Fluida para Microsserviços:** Domínios de negócio com alta carga ou requisitos regulatórios específicos (Faturamento/Billing, Autenticação RADIUS, Triagem de O.S., Fiscal/NFCom) podem ser desacoplados em serviços autônomos preservando 100% de compatibilidade com os clientes.
3. **Desenvolvimento Paralelo Sem Bloqueios:** O time de frontend pode construir telas e fluxos inteiros utilizando servidores de mock locais (`prism`), sem depender do backend ter tabelas ou migrations prontas.
4. **Eliminação de Retrabalho:** Acaba a digitação manual de interfaces TypeScript e métodos Axios redundantes, bem como a criação braçal de DTOs no Java.

---

## 2. Diagnóstico do Modelo Legado (Code-First)

Antes da transição para API-First, o ispERP operava no modelo Code-First manual, onde o backend gerava o Swagger via Springdoc e o frontend escrevia chamadas e tipos manualmente. Uma auditoria no código identificou divergências críticas em produção:

| Módulo | Frontend (`frontend/src/services/`) | Backend (`backend/src/main/.../controller/`) | Impacto Real |
| :--- | :--- | :--- | :--- |
| **Ordens de Serviço** | `api.put('/work-orders/${id}/schedule')`<br>`api.put('/work-orders/${id}/complete')` | `@PostMapping("/{id}/schedule")`<br>`@PostMapping("/{id}/complete")` | **`405 Method Not Allowed`** em operações essenciais de campo. |
| **Ordens de Serviço** | `api.put('/work-orders/${id}/assign')`<br>`api.post('/work-orders')` | Inexistentes no controller principal (alocação foi para `InstallationDispatchController`). | **`404 Not Found`** em chamadas legadas do frontend. |
| **Faturamento** | `api.put('/invoices/${id}/pay')`<br>`api.put('/invoices/${id}/cancel')` | `@PostMapping("/{id}/pay")`<br>`@PostMapping("/{id}/cancel")` | **`405 Method Not Allowed`** na baixa e cancelamento de faturas. |
| **Faturamento** | `api.post('/invoices/generate-monthly')` | `@PostMapping("/trigger-recurring-billing")` | **`404 Not Found`** no disparo de rotina mensal. |
| **Contratos** | `api.put('/contracts/${id}/status', { status })` (JSON body) | `@PatchMapping("/{id}/status")` (`@RequestParam status` na URL) | **`400 Bad Request`** ou status ignorado. |
| **Clientes** | `api.get('/customers/search?q=...')` | `@GetMapping("/search/name")`<br>`@GetMapping("/search/cpf")` | **`404 Not Found`** na busca unificada de clientes. |

---

## 3. Diretriz de Segurança e Blindagem Patrimonial (Zero Trust DTOs)

A adoção do API-First aumenta substancialmente a segurança do ispERP:

1. **Prevenção contra *Mass Assignment* (CWE-915):**
   - NUNCA uma entidade JPA (`@Entity`) é exposta ou recebida diretamente nos endpoints.
   - O gerador de código força os controllers a receberem exclusivamente os DTOs declarados no contrato, garantindo que campos internos (`password_hash`, `tenant_id`, `audit_flags`, `role`) nunca sejam manipulados por requisições maliciosas.
2. **Validação Estrita Centralizada:**
   - Regras de validação (formatos RFC de UUIDv7, regex de CPF/CNPJ, tamanho de strings, valores monetários positivos) são declaradas uma única vez no YAML do contrato e aplicadas automaticamente via Bean Validation (`@Valid`, `@NotNull`, `@Pattern`) no Java e tipagem estrita no TypeScript.
3. **Padronização RFC 7807 (Problem Details):**
   - Respostas de erro nunca expõem nomes de tabelas, stacktraces do Spring ou detalhes de infraestrutura do PostgreSQL. As falhas seguem um JSON padronizado e seguro.
4. **Proteção de Dados em Trânsito:**
   - Elimina-se o uso indevido de `@RequestParam` para dados sensíveis em operações de escrita/alteração, garantindo que dados trafeguem apenas no corpo criptografado via HTTPS, sem vazamento em logs de URLs de proxies reversos.

---

## 4. Topologia Modular dos Contratos (`contracts/openapi/`)

Para manter alta legibilidade e evitar conflitos em branches do Git, a especificação é modularizada com `$ref`:

```text
ispERP/
├── contracts/
│   └── openapi/
│       ├── openapi.yaml                 # Ponto de entrada central (Info, Servers, Security, Tags)
│       ├── components/
│       │   ├── security.yaml            # Esquema JWT Bearer (RFC 7519)
│       │   ├── errors.yaml              # Padronização RFC 7807 (Problem Details)
│       │   └── common.yaml              # Tipos utilitários (UUIDv7, Paginação, Enums de auditoria)
│       └── domains/
│           ├── workorders/              # O.S. e Execução Técnica de Campo
│           │   ├── workorders.yaml      # Endpoints: /work-orders, /schedule, /complete
│           │   ├── execution.yaml       # Endpoints: /technician/execution (OLT Auto-Discovery, RADIUS)
│           │   └── schemas.yaml         # DTOs de O.S. e equipamentos
│           ├── billing/                 # Faturamento e Cobrança
│           │   ├── invoices.yaml        # Endpoints: /invoices, baixa, cancelamento, lote
│           │   └── schemas.yaml         # DTOs de fatura e transação
│           ├── contracts/               # Contratos e Assinatura Digital
│           │   ├── contracts.yaml       # Endpoints: /contracts, status
│           │   └── schemas.yaml         # DTOs de minuta e assinatura
│           └── customers/               # Clientes e Busca
│               ├── customers.yaml       # Endpoints: /customers, /search unificado
│               └── schemas.yaml         # DTOs de cadastro e consulta
```

---

## 5. Ferramentas e Pipeline de Automação

```mermaid
flowchart LR
    YAML["contracts/openapi/ (Modular YAML)"] --> Lint["Spectral / Redocly Lint"]
    Lint --> Bundle["Redocly Bundle -> openapi.bundled.json"]
    
    Bundle --> Gradle["Gradle Plugin (openapi-generator)"]
    Bundle --> Orval["Orval (TypeScript Generator)"]
    Bundle --> Prism["Prism (Mock Server Local)"]
    
    Gradle --> JavaStubs["build/generated/ (Interfaces *Api.java + DTOs)"]
    Orval --> TSClient["src/api/generated/ (Tipos + Clientes HTTP)"]
```

* **Linter de Contratos (Spectral / Redocly):** Garante que nenhuma rota seja criada sem descrição, sem esquema de segurança ou sem códigos de erro padronizados.
* **Bundler (Redocly):** Resolve todas as árvores de `$ref` e gera o artefato canônico `openapi.bundled.json`.
* **Backend Generator (OpenAPI Generator Gradle Plugin):**
  - Configurado com `interfaceOnly = true`. Gera apenas as interfaces Spring MVC e os DTOs Java 25.
  - Os Controllers Java simplesmente implementam a interface (ex: `public class WorkOrderController implements WorkOrdersApi`). Se a interface mudar, o compilador Java acusa o erro imediatamente.
* **Frontend Generator (Orval):**
  - Gera clientes Axios integrados ao interceptor JWT existente em `src/services/api.ts`.
  - Gera 100% das interfaces TypeScript. Se o contrato mudar, o `tsc --noEmit` falha na hora.
* **Servidor de Mocks (Prism):**
  - Permite rodar `npx prism mock contracts/openapi/openapi.bundled.json -p 4010` para testar telas do frontend com respostas sintéticas válidas.

---

## 6. Fluxo de Trabalho Diário (Step-by-Step)

Sempre que uma nova funcionalidade, campo ou rota for necessária:

1. **Passo 1 (Contrato Primeiro):** Declarar o endpoint ou o novo campo no arquivo YAML do domínio correspondente em `contracts/openapi/domains/`.
2. **Passo 2 (Codegen Automatizado):** Executar `./scripts/generate-api.sh`.
   - O Java recebe as novas interfaces e DTOs.
   - O TypeScript recebe as novas tipagens e métodos de cliente prontos.
3. **Passo 3 (Implementação Desacoplada):**
   - **No Backend:** Implementar a lógica de negócio no Controller/Service (sem perder tempo criando DTOs ou anotações HTTP manuais).
   - **No Frontend:** Consumir a função tipada nos componentes React (sem perder tempo criando interfaces ou métodos de Axios manuais).
4. **Passo 4 (Loop Ágil):** Se durante a implementação do frontend faltar um campo, basta inseri-lo no YAML, rodar o codegen e continuar.

---

## 7. Roteiro Estruturado de Migração

A migração do ispERP para o modelo API-First é executada em 4 fases incrementais, sem paralisar o desenvolvimento do sistema:

* **Fase 1 — Infraestrutura de Contratos (Atual):**
  - Criação da pasta `contracts/openapi/` e dos domínios críticos (`WorkOrders`, `Billing`, `Contracts`, `Customers`).
  - Criação do script de bundling e validação (`scripts/bundle-contracts.sh`).
* **Fase 2 — Migração do Backend:**
  - Configuração do plugin `org.openapi.generator` no `build.gradle`.
  - Conexão dos Controllers Spring Boot existentes com as interfaces geradas (começando pelo módulo piloto de Ordens de Serviço).
* **Fase 3 — Migração do Frontend:**
  - Configuração do `Orval` no `frontend/package.json`.
  - Substituição das chamadas manuais nos componentes React pelos clientes tipados gerados.
* **Fase 4 — CI/CD & Governança Automática:**
  - Inclusão do job de validação de contratos no GitHub Actions (`fail-fast`), impedindo merges com divergências de contrato.
