# README.md

## ERP para Provedores de Internet - Código Aberto

Bem-vindo ao repositório do nosso ERP desenvolvido especialmente para Provedores de Internet. Este é um projeto de código aberto e estamos entusiasmados em tê-lo a bordo como contribuidor!

### Visão Geral
Sistema ERP moderno para provedores de internet (ISPs) com:
- **Backend:** Java 25 + Spring Boot 4.1.1 (Arquitetura Modular Orientada a Eventos - EDA)
- **Frontend:** React 19 + Vite 6
- **Banco de dados:** PostgreSQL 17+ com suporte nativo a `UUIDv7`
- **Migrações:** Flyway
- **Contêineres:** Docker e Docker Compose para desenvolvimento e produção

Consulte a pasta [`docs/`](docs/) para a documentação detalhada de Arquitetura, PRD, Roadmap e Eventos.

### Como Usar

#### Pré-requisitos
- Docker e Docker Compose instalados
- Java JDK 25+
- Node.js 24+

#### Configuração Inicial
1. Clone o repositório:
```bash
git clone https://github.com/RuyXingubit/ispERP.git
```

2. Configure as variáveis de ambiente:
```bash
cp .env.example .env
```

3. Inicie os containers:
```bash
docker-compose up -d
```

O sistema estará disponível em `http://localhost:3000`

### Primeiro Acesso
Na primeira execução, você será direcionado para a tela de Setup onde deverá:
1. Cadastrar o primeiro usuário admin
2. Configurar informações básicas da empresa
3. Definir preferências do site institucional

Após esta configuração inicial, a tela de Setup não será mais exibida.

### Como Contribuir
Adoramos contribuições! Siga estes passos:

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Faça commit de suas alterações seguindo o padrão [Conventional Commits](https://www.conventionalcommits.org/) (ex: `feat: add some AmazingFeature` ou `fix: resolve issue with customer address`)
   > Utilize prefixos semânticos como: `feat:` (novas funcionalidades), `fix:` (correção de bugs), `docs:` (documentação), `refactor:` (refatorações), `test:` (testes) ou `chore:` (tarefas de build/manutenção).
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

### Ambiente de Desenvolvimento
Para rodar localmente sem Docker:
```bash
# Backend
./gradlew bootRun

# Frontend
cd frontend
npm install
npm start
```

### Testes
Execute os testes com:
```bash
# Backend
./gradlew test

# Frontend
cd frontend
npm test
```

### Docker
Imagens Docker são geradas automaticamente via GitHub Actions em cada commit.

Para produção:
```bash
docker-compose -f docker-compose.prod.yml up -d
```