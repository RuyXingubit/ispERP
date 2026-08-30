# Padronização Mandatória de TypeScript no Frontend (.ts / .tsx)

## Regra Geral do Projeto
Todas as novas funcionalidades, telas, componentes, serviços, hooks, contextos e utilitários da camada de Frontend (`frontend/src/`) **DEVEM ser implementados obrigatoriamente em TypeScript (`.ts` para lógica/serviços e `.tsx` para componentes React)**.

É expressamente evitado o uso de arquivos legados em JavaScript puro (`.js` ou `.jsx`) dentro de `frontend/src/`.

---

## Diretrizes e Boas Práticas

### 1. Extensões e Estrutura de Arquivos
- **Componentes e Páginas React:** Devem utilizar a extensão `.tsx` (ex: `src/pages/Financial/FiscalDashboard.tsx`, `src/components/Sidebar/Sidebar.tsx`).
- **Serviços, Modelos, Utilitários e Hooks:** Devem utilizar a extensão `.ts` (ex: `src/services/fiscalRegimeService.ts`, `src/utils/cpfValidator.ts`, `src/types/regime.ts`).

### 2. Tipagem de Domínio e Contratos com o Backend
- Novos DTOs, entidades e enums devem ser modelados em `src/types/` em total consonância com as entidades e DTOs Java do Backend.
- Exporte os tipos no `src/types/index.ts` para importações centralizadas.
- Evite o uso indiscriminado de `any` sem justificativa; priorize interfaces tipadas, `unknown` com type guards ou unions explícitas.

### 3. Validação Contínua de Tipos (Typecheck)
Sempre após criar ou modificar arquivos no frontend, a integridade da tipagem deve ser verificada:
```bash
cd frontend
npm run typecheck
```
Nenhum commit ou entrega deve ser finalizado com erros no `tsc --noEmit`.

---

## Resumo das Consequências
- **Type-Safety de ponta a ponta:** O frontend detecta incompatibilidades de campos ou propriedades renomeadas em tempo de compilação.
- **Autocompletion & Produtividade:** A IDE fornece intellisense e refatoração segura em todos os componentes.
- **Sincronia Fullstack:** Alinhamento estrito entre o Spring Boot 4 / Java 25 e o React 19 / Vite 6.
