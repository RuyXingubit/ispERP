# Diretriz Mandatória: Sobriedade Terminológica, Vocabulário Profissional e Zero Exageros ("Sem Encher Linguiça Verbal")

## Propósito
Blindar o software (telas, menus, botões, modais, DTOs, logs e documentações) contra vocabulário melodramático, jargões bélicos, termos policiais pomposos e qualquer forma de "enfeitação de pavão" ou "encher linguiça verbal". O ispERP é um sistema empresarial para provedores de internet (ISPs) e deve comunicar suas funções com máxima sobriedade, clareza técnica e respeito ao tempo do usuário.

---

## Regras Mandatórias de Execução (WORKSPACE & GLOBAL)

### 1. Proibição Estrita de Jargões Hiperbólicos e Melodramáticos
- **Termos Proibidos**: NUNCA utilize palavras sensacionalistas para descrever rotinas operacionais ou administrativas normais.
  - ❌ *Dossiê Forense / Dossiê de Auditoria Forense* ➔ ✅ **Detalhes do Evento**, **Auditoria** ou **Logs de Auditoria**
  - ❌ *Guerra Comercial / Mapa de Guerra* ➔ ✅ **Payback & Expansão de Rede** ou **Ocupação de Rede & Payback**
  - ❌ *Blindagem Patrimonial / Blindagem de Caixa* ➔ ✅ **Backup & Disaster Recovery** ou **Custódia de Caixa**
  - ❌ *Criptografia Militar* ➔ ✅ **Criptografia AES-256-GCM** (especificar o padrão técnico real, sem adjetivos de marketing)
  - ❌ *Varredura Pericial / Dossiês Periciais* ➔ ✅ **Auditoria de Desvios**, **Verificação de Consistência** ou **Alertas Pendentes**
  - ❌ *Central de Investigação Policial* ➔ ✅ **Marco Civil da Internet (Identificação de Conexão)**
  - ❌ *Folha Pericial Forense* ➔ ✅ **Certificado de Autenticidade Digital** (conforme MP 2.200-2/01)

### 2. Princípio da Sobriedade: "Menos é Mais"
- Se a função da tela ou modal é apenas exibir o conteúdo de um log ou entidade, intitule-a pelo que ela é: **"Detalhes do Evento"**, **"Auditoria"** ou **"Visualizar Registro"**.
- Não acrescente adjetivos para tentar fazer uma rotina simples parecer "impressionante". A excelência do software se manifesta na solidez, zero bugs, performance e aderência às regras de negócio reais, não em títulos inflados.

### 3. Nomenclatura Jurídica e Regulatória Sóbria
- Em módulos com respaldo legal (Marco Civil da Internet - Lei 12.965/2014, BACEN Pix, SEFAZ/NFCom), adote estritamente o vocabulário técnico-jurídico formal:
  - Utilize **"Laudo de Identificação de Conexão"**, **"Atendimento a Ofícios Judiciais"**, **"Certificado de Autenticidade"**.
  - O provedor atua como custodiante técnico de registros de conexão, não como órgão policial ou tribunal.

### 4. Coerência Rígida entre Menus, Rotas e Telas
- O título exibido no cabeçalho de uma tela DEVE refletir com precisão e sobriedade o mesmo nome ou conceito do item de menu correspondente.
- É proibido ter um item de navegação simples (ex: "Trilha de Auditoria") que abre uma página com título hiperbólico (ex: "Trilha de Auditoria Forense").

### 5. Check-list Obrigatório em Toda Nova Entrega
Antes de concluir qualquer tarefa de frontend, backend ou documentação:
1. Ler criticamente todos os rótulos de botões, tooltips, modais e títulos de cards.
2. Perguntar: *"Este texto está direto ao ponto ou tem floreio/encher linguiça?"*
3. Se houver qualquer exagero ou termo teatral, substituir imediatamente pelo termo técnico e sóbrio correspondente.
