# README.md

## ERP para Provedores de Internet - Código Aberto

Bem-vindo ao repositório do nosso ERP desenvolvido especialmente para Provedores de Internet. Este é um projeto de código aberto e estamos entusiasmados em tê-lo a bordo como contribuidor!

### Visão Geral
Sistema ERP web com:
- Backend em Java
- Frontend em React
- Banco de dados MySQL
- Migrações com Flyway
- Docker para desenvolvimento e produção

### Como Usar

#### Pré-requisitos
- Docker e Docker Compose instalados
- Java JDK 11+
- Node.js 14+

#### Configuração Inicial
1. Clone o repositório:
```bash
git clone ${repo_url}
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
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
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