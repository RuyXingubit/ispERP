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

## 🚀 Como Usar

### Pré-requisitos
* Docker e Docker Compose instalados
* Java JDK 25+
* Node.js 24+

### 1. Inicialização Rápida com Docker
```bash
# Clonar o repositório
git clone https://github.com/RuyXingubit/ispERP.git
cd ispERP

# Configurar variáveis de ambiente
cp .env.example .env

# Subir containers (PostgreSQL 17, Backend e Frontend)
docker-compose up -d
```
O sistema estará disponível em:
* **Frontend:** `http://localhost:3000`
* **Swagger API Docs:** `http://localhost:8080/swagger-ui.html`

### 2. Ambiente de Desenvolvimento Local (Sem Docker)

#### Backend (Spring Boot 4.1.1):
```bash
cd backend
./gradlew bootRun
```

#### Frontend (React 19 + Vite 8):
```bash
cd frontend
npm install
npm run dev
```

### 3. Testes Automatizados (TDD)
```bash
# Backend (Unitários + Testcontainers com PostgreSQL 17 real)
cd backend
./gradlew test

# Frontend (Typecheck TypeScript e Build de Produção)
cd frontend
npm run typecheck
npm run build
```

---

## 🔐 Primeiro Acesso (Wizard de Setup)
Na primeira execução, o sistema redireciona automaticamente para o fluxo interativo `/setup`:
1. Cadastro do primeiro usuário Administrador master.
2. Parametrização das informações cadastrais da empresa/ISP.
3. Definição das diretrizes visuais e operacionais.

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