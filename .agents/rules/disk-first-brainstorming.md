# Diretriz Obrigatória: Disk-First Brainstorming, Versionamento Incremental & Consolidação Final

## Propósito
Blindar o fluxo de trabalho contra perda de contexto, análises, decisões de negócio ou ideias caso a IDE trave, feche inesperadamente ou a sessão seja reiniciada. A memória do chat é efêmera; o disco e o Git são permanentes.

---

## Regras Mandatórias de Execução (GLOBAL)

### 1. Criação de Pasta Dedicada por Tema (Disk-First)
- Ao iniciar qualquer debate de ideias, brainstorming, levantamento de requisitos, arquitetura de novos módulos ou discussão de regras de negócio em qualquer projeto, o assistente **DEVE criar imediatamente uma pasta de trabalho dentro de `docs/brainstorming/<tema>/`** (ex: `docs/brainstorming/backup_disaster_recovery/` ou `docs/brainstorming/rede_neutra/`).
- NUNCA mantenha discussões complexas, regras de negócio ou decisões exclusivamente na memória do chat.

### 2. Versionamento Incremental Imutável (v1, v2, v3...)
- A cada nova rodada de mensagens onde novas ideias, regras, ajustes ou detalhes técnicos forem definidos, o assistente **DEVE criar um NOVO arquivo versionado sequencial** (ex: `v1_conceito_inicial.md`, `v2_criptografia_e_resgate.md`, `v3_wizard_e_docker.md`).
- **NUNCA sobrescreva ou apague conteúdos anteriores durante a discussão**, preservando 100% do histórico e de insights que foram maturados nas rodadas anteriores.

### 3. Consolidação Final pós-Aprovação Explícita do Usuário
- Somente quando o usuário confirmar explicitamente que a discussão, ideia ou prototipagem foi concluída e se der por satisfeito, o assistente **DEVE compilar, totalizar e unificar tudo em um documento finalizado oficial na pasta `docs/`** (ex: `docs/BACKUP_DISASTER_RECOVERY.md` ou `docs/PRD_<tema>.md`).

### 4. Recuperação Automática pós-Restart
- Ao iniciar uma nova conversa ou ao detectar que houve uma reinicialização da IDE, o assistente deve verificar as pastas recentes em `docs/brainstorming/` e no Git para resgatar imediatamente a última versão (`vN`) e todo o histórico da discussão.
