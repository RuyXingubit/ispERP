# ispERP - ERP para Provedores de Internet (Código Aberto)

[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17+-blue.svg)](https://www.postgresql.org/)
[![React 19](https://img.shields.io/badge/React-19-61dafb.svg)](https://react.dev/)
[![Vite 8](https://img.shields.io/badge/Vite-8.2%20(Rolldown)-purple.svg)](https://vite.dev/)
[![UUIDv7](https://img.shields.io/badge/UUIDv7-RFC%209562-success.svg)](https://www.rfc-editor.org/rfc/rfc9562)

Sistema ERP moderno de alto desempenho, desenvolvido especialmente para **Provedores de Internet (ISPs)** de fibra óptica e wireless, baseado em arquitetura orientada a eventos, isolamento patrimonial, engenharia de telecomunicações e conformidade regulatória plena (ANATEL, Marco Civil e Banco Central).

---

## ⚡ Stack Tecnológica

* **Backend Core:** Java 25 (Virtual Threads) + Spring Boot 4.1.1
* **Arquitetura:** Monólito Modular Orientado a Eventos (EDA) com *Transactional Outbox* e Idempotência
* **Frontend SPA:** React 19 + TypeScript + Vite 8.2 (Motor Rust Rolldown) com *Code-Splitting* e *Lazy Loading* por rota
* **Interface:** Material-UI (MUI v6) com paleta corporativa de alta densidade
* **Banco de Dados:** PostgreSQL 17+ com tipos nativos `UUID DEFAULT uuidv7()` e `JSONB`
* **Migrações:** Flyway (V1 a V31 com histórico linear e verificações integradas)
* **Segurança & Criptografia:** JWT (JJWT 0.13), AES-256 com PBKDF2 e assinaturas SHA-256
* **Compressão & Storage:** ZStandard (ZSTD) para streaming em memória e AWS S3 SDK v2 (S3, Cloudflare R2, SeaweedFS)
* **Testes Automatizados:** Testcontainers PostgreSQL 17 + JUnit 5 + Mockito (> 270 testes com 100% de aprovação)

Consulte a pasta [`docs/`](docs/) para documentação detalhada de Arquitetura, PRD, Roadmap, Eventos e Pareceres Jurídicos.

---

## 🎯 Principais Capacidades e Módulos

### 1. Assinatura Eletrônica Instantânea via Pix (SPI / BACEN)
* Baseada na **MP 2.200-2/01** e **Lei Federal nº 14.063/2020**.
* Validação de titularidade estrita do CPF do pagador diretamente contra a base do Banco Central (BACEN).
* Rejeição automática de pagamentos de terceiros e concessão de rotas oficiais de fallback (Gov.br, E-mail OTP e Cartório).
* Abatimento automático de R$ 1,00 na mensalidade do cliente e emissão de **Folha Pericial Forense** com Hash SHA-256 e End-to-End ID.

### 2. Gestão Financeira, Blindagem Patrimonial & DRE Telecom
* **Custódia Estrita por CPF:** Livro-caixa individual de dinheiro vivo sob custódia de cada colaborador, transferência de gaveta com duplo aceite obrigatório e conciliação bancária cega para CFO.
* **Plano de Contas 5 Níveis Telecom:** Árvore hierárquica contábil padrão (Receitas, Impostos, Interconexão, OPEX e CAPEX), contas a pagar e parcelamentos futuros com juros/amortização.
* **Esteira Anti-Fraude de Isenção de O.S.:** Tarifação padrão de serviços de campo com esteira de auditoria gerencial e aviso oficial automático ao cliente via WhatsApp.
* **Cockpit DRE em Tempo Real:** EBITDA real de telecom nos regimes de Competência e Caixa.
* **Motor de Desalavancagem 36M:** Cálculo matemático do Fundo do Poço (Maximum Drawdown), Data da Alforria Financeira e simulador de novos investimentos "E Se...?".
* **Payback por Projeto FTTH & Sentinela IA:** Mapa de guerra de ocupação de CTOs com alerta de vendas direcionadas e varredura pericial de fraudes.

### 3. Engenharia de Rede, FTTH & NOC Ativo
* **Documentação Óptica ABNT/TIA-598:** Código oficial de cores para tubos e fibras (6 a 144 FO), diagrama unifilar interativo e cálculo de atenuação teórica (*Power Budget*).
* **Auto-Discovery de ONUs:** Descoberta e provisionamento 1-clique em OLTs (SmartOLT / MikroTik / Huawei) com perfis de velocidade.
* **Monitoramento Ativo & Correlação de Rompimento:** NOC com diferenciação automática de *Dying Gasp* (queda de energia no bairro) vs *LOS* (rompimento de fibra).
* **FreeRADIUS Multi-Vendor:** Desconexão PoD (RFC 3576) na porta UDP 3799 para desbloqueio instantâneo pós-Pix (< 1s) e auto-corte inteligente.

### 4. Marco Civil da Internet (Lei nº 12.965/2014)
* **CGNAT Forense Reverso:** Cruzamento em milissegundos de `IP Público + Porta Lógica + Timestamp` ➔ `Mapeamento CGNAT` ➔ `IP Privado` ➔ `radacct` ➔ `Assinante e Endereço`.
* **Laudos Periciais Invioláveis:** Geração de laudo pericial oficial com Hash SHA-256 e QR Code público de autenticidade para autoridades policiais e judiciais.

### 5. Backup Multi-Destino & Disaster Recovery
* Pipeline contínuo de streaming em memória: `Dump -> ZstdOutputStream -> CipherOutputStream (AES-256) -> DigestOutputStream (SHA-256) -> Storage` sem criar arquivos intermediários gigantes no disco.
* Múltiplos destinos simultâneos (AWS S3, Cloudflare R2, MinIO, SFTP e Volumes Locais).
* Teste contínuo de restauração (*Dry-Run Restore*) e emissão de **Kit de Resgate de Emergência** em Markdown com comandos OpenSSL puros (resgate sem depender da aplicação).

---

## 🚀 Como Executar Localmente

### Pré-requisitos
* Docker e Docker Compose instalados
* Java JDK 25+ (opcional para desenvolvimento local sem container)
* Node.js 24+ (opcional para desenvolvimento local sem container)

---

### 1. Ambiente Local Completo com Simulador Operacional (Recomendado)

O projeto possui um **Simulador Operacional de 1 Ano (`DevDataSeederService`)** ativo sob o profile `dev`. Ele popula automaticamente a provedora fictícia **Nexus Fibra Telecomunicações Ltda.** com histórico contínuo de 12 meses de faturamento, DRE, contas a pagar, projetos FTTH e equipes de campo.

```bash
# 1. Clonar o repositório
git clone https://github.com/RuyXingubit/ispERP.git
cd ispERP

# 2. Configurar variáveis de ambiente
cp .env.example .env

# 3. Subir toda a stack (PostgreSQL 17, Backend Spring Boot e Frontend Nginx/React)
docker compose up -d
```

O sistema estará disponível em:
* **Frontend Web:** [http://localhost:3000](http://localhost:3000)
* **API REST & Health:** [http://localhost:8080/api/actuator/health](http://localhost:8080/api/actuator/health)
* **Swagger API Docs:** [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)

> [!TIP]
> **Como resetar o banco de teste para o estado inicial:**
> Para limpar os dados e forçar uma reexecução do seeder de 1 ano do zero:
> ```bash
> docker compose down -v && docker compose up -d
> ```

---

### 👥 Credenciais do Quadro de Colaboradores (Ambiente Local)

Todos os usuários do ambiente de teste utilizam a senha padrão: **`password123`**

| Colaborador | Usuário / Email | Papel (Role) | Atribuição no Sistema / Cenário de Teste |
| :--- | :--- | :--- | :--- |
| **Administrador Master** | `admin@nexusfibra.com.br` | `ADMIN` | Acesso irrestrito a configurações, segurança e auditoria |
| **Roberto Silveira (CFO)** | `cfo@nexusfibra.com.br` | `FINANCIAL` | Cockpit DRE, Desalavancagem 36M, Conciliação e Contas a Pagar |
| **Maria Clara** | `atendente.maria@nexusfibra.com.br` | `SUPPORT_ANALYST` | Atendimento comercial N2, vendas e contratos |
| **João Paulo** | `atendente.joao@nexusfibra.com.br` | `ADMINISTRATIVE_ASSISTANT` | Suporte SAC N1 e emissão de 2ª via de faturas |
| **Camila Santos** | `atendente.camila@nexusfibra.com.br` | `ATTENDANT` | Régua de cobrança e negociação de faturas |
| **Carlos Alberto** | `tecnico.carlos@nexusfibra.com.br` | `TECHNICIAN` | Equipe Alfa (Veículo 01) • **R$ 150,00 em dinheiro vivo** em mãos |
| **Lucas Mendes** | `tecnico.lucas@nexusfibra.com.br` | `TECHNICIAN` | Equipe Alfa (Veículo 01) • Almoxarifado móvel |
| **Marcos Rocha** | `tecnico.marcos@nexusfibra.com.br` | `TECHNICIAN` | Equipe Bravo (Veículo 02) • Máquina de fusão em custódia |
| **André Luis** | `tecnico.andre@nexusfibra.com.br` | `TECHNICIAN` | Equipe Bravo (Veículo 02) • Almoxarifado móvel |

---

### 📦 Massa Operacional Pré-Carregada (1 Ano de Histórico)

* **Financeiro & DRE em Tempo Real:**
  * **92 faturas pagas retroativamente** mês a mês, alimentando receitas líquidas reais no DRE.
  * **Contas a Pagar:** Financiamento de OLT Huawei em 24 parcelas de R$ 2.000 (12 quitadas, 12 pendentes alimentando passivo).
  * **Custos Fixos Pagos:** Link de Trânsito IP (R$ 3.500/mês) e Compartilhamento de Postes (R$ 1.200/mês) quitados nos 12 meses.
* **Mapa de Guerra de Payback FTTH:**
  * **Expansão Bairro Jardins:** Orçamento de R$ 48.000, 2 CTOs implantadas (32 portas).
  * **Expansão Bairro Alvorada:** Orçamento de R$ 65.000, 1 CTO implantada (16 portas).
  * **Alerta Comercial Ativo:** Identificação de capacidade ociosa para panfletagem e vendas direcionadas.
* **Custódia por CPF & Passagem de Turno:**
  * Técnico Carlos Alberto com **R$ 150,00 em dinheiro vivo** em custódia patrimonial para teste de transferência de gaveta e duplo aceite em `/financial/custody/cash`.
  * Máquinas de fusão (Fujikura 70S e Inno View 5) sob custódia direta no CPF dos técnicos.
* **Cenários Prontos para Teste na Interface:**
  1. **Inadimplência e Desbloqueio em Confiança:** Cliente *Marcos Vinicius Inadimplente* com fatura vencida há 12 dias (`OVERDUE`) e contrato suspenso no RADIUS.
  2. **Assinatura Eletrônica Pix Pendente:** Cliente *Thiago Alencar* com contrato aguardando assinatura digital com validação Pix.
  3. **Despacho de Instalação do Dia:** Cliente *Patrícia Ribeiro* com Ordem de Serviço matutina agendada para Carlos Alberto no Painel de Despacho.

---

### 2. Desenvolvimento Local (Sem Docker)

#### Backend (Spring Boot 4.1.1):
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### Frontend (React 19 + Vite 8):
```bash
cd frontend
npm install
npm run dev
```

---

### 3. Testes Automatizados e Integridade (TDD)
```bash
# Backend (Suíte completa de 272 testes com banco real)
cd backend
./gradlew test

# Frontend (Typecheck TypeScript e Build de Produção com Rolldown)
cd frontend
npm run typecheck
npm run build
```

---

## 🔐 Primeiro Acesso vs. Modo Desenvolvimento

* **Ambiente de Desenvolvimento (`dev`):** Como a empresa de testes (Nexus Fibra Telecom) já é injetada automaticamente no startup pelo seeder, o assistente `/setup` é automaticamente pulado, direcionando o usuário diretamente para o Login/Home.
* **Ambiente de Produção (`prod`):** Ao subir em servidor limpo sem seeder, o sistema identifica que não há registros e redireciona automaticamente para o fluxo interativo `/setup`:
  1. Cadastro do primeiro usuário Administrador master.
  2. Parametrização dos dados fiscais da empresa/ISP.
  3. Personalização do tema e logotipo.

---

## 🤝 Como Contribuir
1. Faça um Fork do projeto.
2. Crie uma branch para sua funcionalidade (`git checkout -b feature/MinhaFeature`).
3. Faça commit seguindo as diretrizes de [Conventional Commits](https://www.conventionalcommits.org/):
   * `feat:` Novas funcionalidades de domínio
   * `fix:` Correção de bugs ou falhas
   * `perf:` Otimizações de desempenho
   * `docs:` Melhorias de documentação
   * `test:` Adição ou melhoria de suítes de testes
4. Envie para o GitHub (`git push origin feature/MinhaFeature`) e abra um Pull Request.

---

## 📄 Licença
Distribuído sob a licença de Código Aberto. Consulte o arquivo [LICENSE](LICENSE) para obter mais informações.