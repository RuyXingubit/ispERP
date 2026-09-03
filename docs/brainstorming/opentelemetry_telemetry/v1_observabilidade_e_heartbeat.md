# Brainstorming & Arquitetura: Observabilidade & Telemetria no ispERP

> **Status:** Rascunho Vivo / Em Discussão  
> **Data:** 2026-09-01  
> **Objetivo:** Definir a estratégia de OpenTelemetry (OTel), observabilidade interna (APM/NOC) e Telemetria Global da Comunidade (Heartbeat anônimo para estatísticas no site oficial e guia de dimensionamento de hardware).

---

## 1. Visão Geral: Dois Tipos de Telemetria

Para clareza arquitetural, o ispERP divide sua telemetria em duas vertentes:

```mermaid
flowchart TD
    subgraph InstanciaISP["Instância Local do ISP (Self-Hosted)"]
        App[ispERP Spring Boot]
        DB[(PostgreSQL)]
        Micrometer[Micrometer / OTel Core]
        
        App -->|Métricas de Performance & DB| Micrometer
        App -->|Coleta de Agregados Anônimos| HeartbeatJob[Heartbeat Ping Job\n(1x por dia / 24h)]
    end

    subgraph ObservabilidadeInterna["1. Observabilidade Interna (NOC do ISP)"]
        Micrometer -->|OTLP / Prometheus| LocalGrafana[Grafana / Prometheus Local do ISP]
    end

    subgraph TelemetriaComunidade["2. Telemetria Global Open-Source (ispERP Core)"]
        HeartbeatJob -->|HTTPS POST JSON Anônimo| CentralServer[telemetry.isperp.dev\n(Servidor Central)]
        CentralServer --> DashboardSite[Site Oficial / Landing Page\n(Contador de ISPs, Clientes & Stats)]
        CentralServer --> SizingGuide[Guia de Dimensionamento de Hardware\n(Capacity Planning baseado em dados reais)]
    end
```

1. **Observabilidade Interna (APM / NOC Local):**
   - Focada no operador daquela instalação para monitorar seus próprios servidores, OLTs, transações e latências via Grafana/Prometheus local (OpenTelemetry / Micrometer).
2. **Telemetria Global de Produto (Anonymous Open-Source Heartbeat):**
   - Focada no projeto ispERP (mantenedores e comunidade) para coletar dados agregados, medir adoção global, retenção/desistência, provar tração no site oficial e construir um guia científico de dimensionamento de hardware.

---

## 2. Áreas de Aplicação do OpenTelemetry (Observabilidade Interna)

### A. Rastreamento Distribuído no Motor de Eventos (Transactional Outbox Tracing)
- **Problema:** Um `POST /api/sales` insere na Outbox e encerra a requisição HTTP. Depois, o `OutboxDispatcher` pega o evento em outra thread e dispara múltiplos consumidores (`ContractService`, `InventoryStockConsumer`, `NetworkProvisioningConsumer`, `NotificationEventConsumer`).
- **Solução OTel:**
  - Injetar o contexto W3C (`traceparent`, `tracestate`) nos metadados do `OutboxEvent`.
  - Ao processar no dispatcher/consumidor, abrir um *Child Span* vinculado ao Trace ID original.
  - **Resultado:** No Grafana Tempo/Jaeger, uma única busca pelo CPF ou ID da venda mostra o fluxo completo: da requisição Web até o provisionamento físico na OLT e envio de e-mail.

### B. Spans & Latência de Equipamentos de Telecom (Drivers de Rede)
- Medir latência de comandos na SmartOLT API, requisições RouterOS API e pacotes CoA/PoD do FreeRADIUS.
- Identificar se a lentidão no onboarding ocorreu na OLT, no banco ou no envio de mensagens.

### C. Gateways de Pagamento (Pix BACEN) & Fiscal (NFCom SEFAZ)
- Rastreamento ponta a ponta dos Webhooks Pix: chegada do webhook ➡️ baixa da fatura ➡️ emissão de `InvoicePaidEvent` ➡️ desbloqueio de rede no RADIUS (< 1s).

---

## 3. Telemetria Global Anônima (Product Heartbeat)

### A. Coleta Técnica Nativa (Fase 1 - Ativa por Padrão)
- **Privacidade Absoluta:** O payload **NUNCA** envia dados sensíveis (sem nomes, sem CPFs, sem senhas, sem credenciais de gateways e sem IPs gravados no banco analítico).
- Como se trata de software livre com código 100% público e auditável, contendo estritamente contadores numéricos e métricas de hardware/sistema, a coleta roda nativamente para alimentar o site oficial e o guia de dimensionamento.

### B. Payload do Heartbeat Anônimo:
```json
{
  "instance_id": "sha256_hash_gerado_no_primeiro_boot",
  "app_version": "1.4.2",
  "uptime_days": 47,
  "first_installed_at": "2026-03-15T10:00:00Z",
  "environment": "PRODUCTION",
  
  "aggregates": {
    "active_customers_count": 3420,
    "active_contracts_count": 3580,
    "active_onus_count": 3410,
    "monthly_invoices_generated": 3600,
    "ftth_ctos_count": 210,
    "olt_count": 3,
    "radius_enabled": true,
    "fiscal_nfcom_enabled": true
  },
  
  "system_hardware": {
    "available_processors": 8,
    "jvm_max_memory_mb": 4096,
    "jvm_used_memory_mb": 1820,
    "os_name": "Linux",
    "os_arch": "amd64",
    "java_version": "25.0.2",
    "db_version": "PostgreSQL 17.2",
    "db_size_mb": 420
  },
  
  "performance_benchmarks": {
    "avg_http_response_ms": 14.2,
    "p95_http_response_ms": 45.8,
    "avg_outbox_lag_ms": 120.5
  }
}
```

---

## 4. Telemetria Avançada para Contratos de Suporte (Fase 2)
- Quando o provedor contrata um plano de suporte e insere sua chave de licença (`support_license_key`), o ispERP habilita o canal de **Monitoramento Pró-Ativo**:
  - Alerta de disco > 85% ou falhas de I/O.
  - Alertas de backup com falha ou storage inacessível.
  - Monitoramento de integridade do PostgreSQL e deadlocks.
