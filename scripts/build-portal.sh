#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script de Construção e Empacotamento do Portal de Documentação OpenAPI (ispERP)
# ==============================================================================
# Uso:
#   ./scripts/build-portal.sh [DIRETORIO_DESTINO]
#   Padrão: docs/portal
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_DIR="${1:-${ROOT_DIR}/docs/portal}"
CONTRACTS_DIR="${ROOT_DIR}/contracts/openapi"
BUNDLED_SPEC="${CONTRACTS_DIR}/openapi.bundled.json"

echo "=================================================================="
echo "🚀 [Portal] Iniciando compilação do Portal de Documentação ispERP"
echo "📂 [Portal] Destino: ${OUTPUT_DIR}"
echo "=================================================================="

# 1. Garantir que os contratos OpenAPI estão validados e compilados
echo "📦 [Portal] 1/5 Executando validação e bundling OpenAPI..."
"${SCRIPT_DIR}/bundle-contracts.sh"

# 2. Criar diretório de destino
echo "📁 [Portal] 2/5 Preparando diretório de destino..."
mkdir -p "${OUTPUT_DIR}"

# 3. Gerar documentação estática Redocly em api.html
echo "📄 [Portal] 3/5 Compilando documentação Redocly em api.html..."
npx -y @redocly/cli build-docs "${BUNDLED_SPEC}" \
  --output "${OUTPUT_DIR}/api.html" \
  --title "ispERP — Contratos de API (OpenAPI Reference)"

# 4. Injetar cabeçalho unificado e estilos no api.html
echo "🎨 [Portal] 4/5 Injetando navegação unificada e design tokens no api.html..."
node -e '
const fs = require("fs");
const targetPath = process.argv[1];

let html = fs.readFileSync(targetPath, "utf-8");

const headSnippet = `
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet" />
  <style>
    :root {
      --bg-base: #0a0d14;
      --bg-surface: #101522;
      --bg-surface-elevated: #161d2e;
      --border-subtle: #1f293d;
      --border-strong: #2e3d5b;
      --border-focus: #38bdf8;
      --text-primary: #f1f5f9;
      --text-secondary: #94a3b8;
      --text-tertiary: #64748b;
      --accent-blue: #0284c7;
      --accent-cyan: #06b6d4;
      --accent-green: #10b981;
      --font-sans: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      --font-mono: "JetBrains Mono", ui-monospace, monospace;
    }
    .portal-nav {
      position: sticky;
      top: 0;
      z-index: 10000;
      height: 58px;
      background: rgba(10, 13, 20, 0.96);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      box-sizing: border-box;
      font-family: var(--font-sans);
    }
    .nav-container {
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
      padding: 0 1.5rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1.5rem;
      flex-wrap: nowrap;
      box-sizing: border-box;
    }
    .nav-left {
      display: flex;
      align-items: center;
      gap: 1.5rem;
      min-width: 0;
      flex-shrink: 0;
    }
    .nav-brand {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      text-decoration: none;
      color: var(--text-primary);
      flex-shrink: 0;
    }
    .nav-brand-logo {
      font-family: var(--font-mono);
      font-weight: 700;
      font-size: 1.15rem;
      letter-spacing: -0.03em;
      line-height: 1;
    }
    .nav-brand-logo span {
      color: var(--accent-cyan);
    }
    .badge-oss {
      font-family: var(--font-mono);
      font-size: 0.68rem;
      font-weight: 600;
      padding: 0.15rem 0.45rem;
      background: rgba(16, 185, 129, 0.1);
      color: var(--accent-green);
      border: 1px solid rgba(16, 185, 129, 0.3);
      border-radius: 4px;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      line-height: 1;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      flex-shrink: 0;
      white-space: nowrap;
    }
    .nav-link {
      color: var(--text-secondary);
      text-decoration: none;
      font-size: 0.84rem;
      font-weight: 500;
      padding: 0.35rem 0.65rem;
      border-radius: 4px;
      white-space: nowrap;
      transition: color 0.15s ease, background-color 0.15s ease;
      line-height: 1.2;
    }
    .nav-link:hover {
      color: var(--text-primary);
      background-color: var(--bg-surface-elevated);
    }
    .nav-link.active {
      color: var(--text-primary);
      background-color: var(--bg-surface-elevated);
      border: 1px solid var(--border-subtle);
    }
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      flex-shrink: 0;
      white-space: nowrap;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.45rem;
      padding: 0.4rem 0.85rem;
      font-family: var(--font-sans);
      font-size: 0.82rem;
      font-weight: 600;
      border-radius: 4px;
      text-decoration: none;
      cursor: pointer;
      white-space: nowrap;
      line-height: 1;
      height: 34px;
      transition: all 0.15s ease;
      box-sizing: border-box;
    }
    .btn-icon {
      padding: 0.4rem;
      width: 34px;
      height: 34px;
    }
    .btn-outline {
      background: transparent;
      color: var(--text-primary);
      border: 1px solid var(--border-strong);
    }
    .btn-outline:hover {
      background: var(--bg-surface);
      border-color: var(--text-secondary);
    }
    .btn-solid {
      background: var(--accent-blue);
      color: #ffffff;
      border: 1px solid var(--accent-blue);
    }
    .btn-solid:hover {
      background: #0369a1;
      border-color: #0369a1;
    }
    @media (max-width: 1080px) {
      .nav-links .nav-link-secondary { display: none; }
    }
    @media (max-width: 768px) {
      .nav-links { display: none; }
      .btn-text-hide { display: none; }
    }
  </style>
`;

const navSnippet = `
  <nav class="portal-nav">
    <div class="nav-container">
      <div class="nav-left">
        <a href="index.html" class="nav-brand">
          <div class="nav-brand-logo">
            isp<span>ERP</span>
          </div>
          <span class="badge-oss">Open Source</span>
        </a>

        <div class="nav-links">
          <a href="index.html" class="nav-link">Visão Geral</a>
          <a href="index.html#comparativo" class="nav-link nav-link-secondary">Comparativo</a>
          <a href="index.html#operacao" class="nav-link nav-link-secondary">Operação Telecom</a>
          <a href="index.html#explorer" class="nav-link">Módulos & Contratos</a>
          <a href="api.html" class="nav-link active">Contratos de API (Redocly)</a>
        </div>
      </div>

      <div class="nav-actions">
        <a href="openapi.bundled.json" download="ispERP-openapi.json" class="btn btn-outline" title="Baixar especificação OpenAPI 3.0.3 (JSON)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span class="btn-text-hide">Spec JSON</span>
        </a>
        <a href="https://github.com/RuyXingubit/ispERP" target="_blank" rel="noopener noreferrer" class="btn btn-outline btn-icon" title="Código no GitHub">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
          </svg>
        </a>
      </div>
    </div>
  </nav>
`;

if (!html.includes("portal-nav")) {
  html = html.replace("</head>", headSnippet + "\n</head>");
  html = html.replace(/<body[^>]*>/, "$&\n" + navSnippet);
  fs.writeFileSync(targetPath, html, "utf-8");
}
' "${OUTPUT_DIR}/api.html"

# 5. Copiar index.html, spec bundled e criar .nojekyll
echo "📑 [Portal] 5/5 Copiando assets e gerando .nojekyll..."
if [ "${OUTPUT_DIR}" != "${ROOT_DIR}/docs/portal" ]; then
  cp "${ROOT_DIR}/docs/portal/index.html" "${OUTPUT_DIR}/index.html"
fi

cp "${BUNDLED_SPEC}" "${OUTPUT_DIR}/openapi.bundled.json"
touch "${OUTPUT_DIR}/.nojekyll"

echo "=================================================================="
echo "✅ [Portal] Portal compilado com sucesso em: ${OUTPUT_DIR}"
ls -lh "${OUTPUT_DIR}"
echo "=================================================================="
