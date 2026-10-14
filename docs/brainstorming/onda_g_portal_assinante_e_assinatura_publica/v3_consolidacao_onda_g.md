# Brainstorming Onda G — Consolidação Final: Portal do Assinante (`frontend-customer`) & Assinatura Pública (`/sign/{token}`)
**Versão:** v3 (Consolidação Final Aprovada)
**Data:** 2026-10-14

## 1. Resumo Executivo das Decisões Aprovadas

### Frente 1: Página Pública de Assinatura Eletrônica Forense (`/sign/{token}`) no `frontend-customer`
1. **Roteamento Híbrido (`extractSignatureTokenFromUrl`)**:
   - Suporta `/sign/{token}` no `pathname` e `?token={token}` ou `?sign={token}` na `search` query string.
   - Oculta a tela de login e exibe `#view-signature`.
2. **Serviço de Assinatura Pública (`src/services/signatureService.ts`)**:
   - `fetchPublicSignatureView(token)` -> `GET /public/signatures/{token}`
   - `fetchPublicSignatureStatus(token)` -> `GET /public/signatures/{token}/status`
   - `selectPublicSignatureFallback(token, fallbackMethod, justification)` -> `POST /public/signatures/{token}/fallback`
   - `getSignedContractPdfUrl(token)` -> `${config.apiBaseUrl}/public/signatures/${token}/pdf`
3. **Jornada de Consentimento, PIX Simbólico (R$ 1,00) e Certificado Forense**:
   - Renderiza dados do contrato (`contractName`, `customerName`, `customerDocumentMasked`, `companyName`, `documentSha256Hash`) e o conteúdo integral do contrato (`renderedContent`).
   - Exige marcação do checkbox da cláusula de consentimento (`#chk-signature-consent`) antes de liberar o painel do PIX Simbólico (`#signature-pix-panel`).
   - Faz polling automático a cada 5 segundos (`GET /public/signatures/{token}/status`) e botão manual "Já paguei / Verificar agora".
   - Quando `SIGNED`, exibe o painel de sucesso com dados bancários validados (`payerName`, `payerBankName`) e link direto para download do PDF Forense (`/api/public/signatures/{token}/pdf`).
   - Quando `REJECTED_DIVERGENT_DOCUMENT` (ou sob solicitação de contingência), exibe o motivo de rejeição anti-fraude e permite selecionar uma rota de Fallback (`GOV_BR`, `EMAIL_OTP`, `PHYSICAL_NOTARY`) via `POST /public/signatures/{token}/fallback`.

### Frente 2: Completude do Dashboard Autenticado no `frontend-customer` & Alinhamento Documental
1. **Detalhes do Contrato e Encargos/Descontos na Fatura**:
   - Exibe o identificador e status do contrato no card de conexão (`#contract-meta`).
   - Nos cartões de fatura, exibe desconto de onboarding/fidelidade (`discountAmount`), multa (`penaltyAmount`) e juros (`interestAmount`) quando presentes, além do botão de download de **2ª Via PDF / NFCom** (`inv.pdfUrl`).
2. **Seção de Planos Disponíveis para Upgrade (`#upgrade-plans-section`)**:
   - Quando `availableUpgradePlans` possuir planos disponíveis, renderiza os cartões de upgrade no Dashboard do assinante.
3. **Alinhamento Documental**:
   - Atualização de `docs/IMPLEMENTATION_PLAN.md` e `docs/SDD.md` refletindo 100% de conclusão de todas as fases e ondas.
