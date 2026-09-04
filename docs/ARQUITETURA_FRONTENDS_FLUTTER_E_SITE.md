# Arquitetura de Frontends: Frontend Flutter Multiperfil & Site Vitrine Comercial

**Data de Consolidação:** 2026-09-04  
**Branch:** `feat/frontend-flutter`  
**Status:** Arquitetura Oficial Aprovada em Desenvolvimento  

---

## 1. Visão Geral & Contexto Estratégico

O ecossistema **ispERP** adota a arquitetura de **1 API Central + 2 Frontends Especializados**. Essa decisão resolve o dilema histórico em sistemas de gestão para provedores de internet (ISPs), onde tentar unificar a operação interna e o site comercial em um único frontend acarreta prejuízos para ambos os públicos:

```
                       ┌──────────────────────────────────────────────┐
                       │          ispERP Backend Central (Java 25)    │
                       │           OpenAPI 3.0.3 (Single Source)      │
                       └──────────────────────┬───────────────────────┘
                                              │
                    ┌─────────────────────────┴─────────────────────────┐
                    │                                                   │
                    ▼                                                   ▼
       ┌─────────────────────────┐                         ┌─────────────────────────┐
       │   ERP Operacional       │                         │   Site Vitrine ISP      │
       │   (Flutter Multiplataforma)│                       │   (Web Leve com SEO)    │
       ├─────────────────────────┤                         ├─────────────────────────┤
       │ • Desktop (Windows/Linux)│                        │ • HTML5/CSS3 Puro/Next  │
       │ • Web Interna           │                         │ • 100% Indexável Google │
       │ • Mobile (Android/iOS)  │                         │ • Consome Dados da API  │
       │ • Uso: Colaboradores    │                         │ • Uso: Clientes / Leads │
       └─────────────────────────┘                         └─────────────────────────┘
```

### A Separação das Responsabilidades

1. **Frontend Operacional (`frontend-flutter/`)**:
   - **Público:** Colaboradores do provedor (Diretoria, Financeiro, Atendimento, Vendas, Técnicos de Campo).
   - **Necessidades:** Densidade de informação, atalhos de teclado, velocidade de digitação, mapas, suporte offline para técnicos, leitor de código de barras para ONUs, e execução nativa em múltiplos sistemas operacionais.
   - **SEO:** Zero necessidade de indexação em motores de busca.

2. **Site Vitrine Comercial (`frontend-site/`)**:
   - **Público:** Clientes finais e potenciais assinantes (leads) do provedor de internet.
   - **Necessidades:** "A homenagem a Tim Berners-Lee e a World Wide Web" — a porta de entrada da empresa na internet. Tempo de carregamento inferior a 1 segundo em redes 4G, conformidade rigorosa com Core Web Vitals, metadados OpenGraph para redes sociais, campanhas de tráfego pago (Google Ads, Meta Ads) com rastreamento UTM, e catálogo de planos atualizado dinamicamente.

---

## 2. Frontend Operacional Flutter: Modelo Multiperfil & Self-Hosted

### 2.1 Modelo de Conexão Self-Hosted Multi-Servidor (Lojas de Aplicativos)

O ispERP é uma solução open source que pode ser hospedada em infraestrutura própria de cada provedor (on-premises, VPS, nuvem). Para viabilizar a distribuição de um único aplicativo oficial na **Google Play Store** e na **Apple App Store**, o aplicativo adota a arquitetura de **conexão dinâmica ao servidor**:

```
[ Usuário baixa o ispERP na App Store / Play Store ou instala Desktop ]
                               │
                               ▼
            ┌──────────────────────────────────────┐
            │   1. Tela de Conexão com Servidor    │
            │   "Informe o endereço do seu ispERP" │
            │   Ex: https://erp.provedor.com.br    │
            └──────────────────┬───────────────────┘
                               │ Validação: GET /actuator/health ou /api/v1/ping
                               ▼
            ┌──────────────────────────────────────┐
            │   2. Tela de Autenticação            │
            │   Usuário / E-mail e Senha           │
            │   POST /api/auth/login               │
            └──────────────────┬───────────────────┘
                               │ Retorno: JWT + Roles do Usuário
                               ▼
            ┌──────────────────────────────────────┐
            │   3. Shell e Rotas por Perfil (RBAC) │
            │   Carrega Interface Especializada    │
            └──────────────────────────────────────┘
```

#### Regras de Segurança e Persistência da Conexão:
- **`flutter_secure_storage`**: Armazena de forma criptografada o Token JWT (`accessToken`) e o token de renovação (`refreshToken`) no Keychain (iOS/macOS) e Keystore (Android).
- **`shared_preferences`**: Persiste a URL base do servidor ativo (`apiBaseUrl`), histórico de servidores salvos e preferências visuais.
- **Multiprovedor:** Permite alternar rapidamente entre servidores sem desinstalar o app (útil para diretores, consultores ou técnicos que atendem mais de uma filial).

---

### 2.2 Matriz de Perfis e Telas Dedicadas (Role-Based UI)

Em vez de menus genéricos e telas poluídas, a interface do Flutter se adapta automaticamente ao cargo do colaborador autenticado:

| Perfil (Role) | Plataforma Preferencial | Foco da Interface | Rotinas & Telas Chave |
| :--- | :--- | :--- | :--- |
| **`ADMIN`** (Diretoria & Gestão Geral) | Desktop / Web | Gestão Global 360º | Visão executiva completa, auditoria do sistema, cadastro de filiais, configuração de OLTs/RADIUS, DRE consolidado e controle de permissões. |
| **`FINANCIAL`** (Administrativo & Financeiro) | Desktop / Web | Tesouraria e Conformidade Fiscal | Contas a Pagar, Conciliação Bancária com OCR/OFX, Fechamento de Caixa, DRE gerencial em tempo real e Emissão de NFCom Modelo 62. |
| **`SUPPORT`** (Atendimento & Suporte Técnico) | Desktop / Web | Resolução Ágil de Demandas | Consulta unificada de clientes (CPF, Nome, MAC), Fila de chamados (SLA ANATEL), Desbloqueio em Confiança, Emissão de 2ª via e Diagnóstico de Sinal Óptico. |
| **`SALES`** (Comercial & Vendas) | Desktop / Mobile / Tablet | Captação e Fechamento | Funil de Vendas (CRM), Consulta de Viabilidade Técnica por CEP/Bairro, Catálogo de Planos Ativos e Emissão Rápida de Proposta/Contrato. |
| **`TECHNICIAN`** (Técnico de Campo) | Mobile (Android / iOS) | Execução Rápida em Campo | Minhas O.S. do dia, Integração com GPS (Waze/Google Maps), Leitor de Código de Barras de ONU/Roteador pela câmera, Fotos da Instalação e Assinatura Digital do Assinante na tela. |

---

### 2.3 Shell de Navegação Adaptativo (Responsive Navigation)

- **Telas Grandes (Desktop Windows/Linux/macOS e Web - largura > 800px):**
  - Barra lateral (*NavigationRail* / *Sidebar*) retrátil com atalhos de teclado rápidos (`Ctrl+K` para busca global, `F2` para nova O.S./cliente).
  - Suporte nativo a múltiplos monitores e impressão direta de comprovantes em impressoras térmicas (ESC/POS).
- **Telas Pequenas (Mobile Android/iOS - largura ≤ 800px):**
  - Barra inferior (*NavigationBar*) com os 4 ou 5 destinos mais críticos do perfil do usuário.
  - Gaveta rápida (*Drawer*) para ações secundárias e troca de perfil/servidor.

---

### 2.4 Consumo API-First e Tipagem Estrita em Dart

Toda a comunicação do Flutter com o backend Java 25 segue o princípio **API-First**:
- O contrato oficial em `contracts/openapi/openapi.bundled.json` é a fonte única da verdade (*Single Source of Truth*).
- O cliente HTTP e os DTOs são gerados em Dart com a biblioteca `Dio`, garantindo que:
  - Todas as respostas de erro usem a especificação `ProblemDetails` (RFC 7807).
  - Headers `Authorization: Bearer <token>` sejam injetados automaticamente por um interceptor de rede.
  - Tokens expirados (HTTP 401) disparem o fluxo de renovação silenciosa (*refresh token*) ou redirecionem à tela de login.

---

## 3. Site Vitrine Comercial (`frontend-site/`)

### 3.1 O Propósito do Site Vitrine

O site comercial do provedor é a vitrine aberta para o público da internet. Ele não exige autenticação e opera como ferramenta de marketing e vendas.

### 3.2 Dinâmica de Consumo da API Central

O site vitrine consome dados públicos da API do ispERP de forma assíncrona ou em Server-Side Rendering (SSR), eliminando a necessidade de reescrever código quando o negócio muda:

1. **Identidade e Unidades (`GET /companies`)**:
   - Razão social, nome fantasia, CNPJ, telefone do SAC, WhatsApp de vendas e endereços das lojas físicas vêm diretamente do ERP. Mudou o endereço da sede? O site atualiza sem novo deploy.
2. **Catálogo de Planos e Preços (`GET /plans/active`)**:
   - Planos vigentes, velocidades de download/upload, tecnologia (fibra/rádio) e valores mensais são lidos do ERP. Quando um plano antigo é inativado ou uma nova velocidade é lançada, o site atualiza instantaneamente.
3. **Consulta de Viabilidade Técnica (`GET /commercial/coverage`)**:
   - O visitante digita o CEP ou escolhe sua cidade/bairro para checar se o provedor atende aquele endereço.
4. **Atribuição de Marketing e Rastreamento de Campanhas (UTMs)**:
   - A equipe de marketing cria campanhas no ERP (ex: `CAMPANHA_BLACK_FRIDAY`).
   - Os links patrocinados no Instagram, Facebook ou Google Ads incluem parâmetros como `?utm_source=instagram&utm_campaign=black_friday`.
   - Ao preencher o formulário de assinatura no site, esses parâmetros são enviados ao ERP (`POST /sales`), permitindo ao diretor visualizar no DRE exatamente o ROI de cada campanha.
5. **Liberdade de Customização:**
   - O projeto entrega um template de alta performance com design moderno, mas 100% aberto. Agências terceirizadas ou a equipe interna do ISP podem modificar o CSS, as cores e as fotos livremente.

---

## 4. Governança de Branches e Ciclo de Desenvolvimento

Para preservar a integridade do sistema em produção e dos deploys de documentação:

1. **Branch `feat/frontend-flutter` (Isolamento Total)**:
   - Todo o desenvolvimento do Flutter (`frontend-flutter/`) e do template do site vitrine (`frontend-site/`) ocorre com exclusividade nesta branch.
   - A branch `main` permanece intocada, estável e com o portal de documentação operacional no GitHub Pages.
2. **Validação Prática & Aprovação**:
   - Apenas após a implementação completa das Fases 1 a 4, com testes de widget e validação de usabilidade nas plataformas Desktop e Mobile, será agendada a revisão final com o usuário para decidir entre:
     - **Opção A:** Realizar o *Merge* da branch `feat/frontend-flutter` na `main`.
     - **Opção B:** Promover a branch `feat/frontend-flutter` a nova branch padrão (`main`).
