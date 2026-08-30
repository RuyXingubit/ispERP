# PDR.md (Product Development Roadmap - Legado / Original)

> Documento original mantido para histórico e rastreabilidade da evolução do projeto.
> A versão atualizada e detalhada está em [PRD.md](PRD.md) e [ARCHITECTURE.md](ARCHITECTURE.md).

## Visão do Produto
ERP completo para provedores de internet com:
- Site institucional customizável
- Gestão de clientes e serviços
- Controle financeiro
- Suporte técnico

## Fase 1: Configuração Inicial
- [x] Tela de Setup inicial
- [x] Cadastro do primeiro admin
- [x] CRUD de informações da empresa
- [x] Customização do site institucional (logo, cores, conteúdo)

## Arquitetura Inicial
- Backend: Java Spring Boot
- Frontend: React.js
- Banco: MySQL com Flyway para migrações (Atualizado para PostgreSQL)
- Docker para todos os ambientes

## Princípios de Desenvolvimento
1. Código limpo e legível
2. Orientação a objetos bem definida
3. Facilidade de adição de novas funcionalidades
4. Documentação completa
5. Testes automatizados (TDD)
6. Event-Driven Architecture (EDA)
7. Identificadores com UUIDv7
