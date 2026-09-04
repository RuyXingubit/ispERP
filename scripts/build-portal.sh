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
      --bg-dark: #090d16;
      --border-color: rgba(255, 255, 255, 0.08);
      --primary: #0284c7;
      --primary-light: #38bdf8;
      --text-main: #f8fafc;
      --text-muted: #94a3b8;
      --font-main: "Inter", -apple-system, BlinkMacSystemFont, sans-serif;
    }
    .portal-nav {
      position: sticky;
      top: 0;
      z-index: 10000;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.75rem 2rem;
      background: rgba(9, 13, 22, 0.96);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border-color);
      font-family: var(--font-main);
      box-sizing: border-box;
    }
    .nav-brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
      color: var(--text-main);
    }
    .nav-logo-icon {
      width: 34px;
      height: 34px;
      border-radius: 9px;
      background: linear-gradient(135deg, #0284c7 0%, #6366f1 100%);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .nav-brand-text {
      font-size: 1.15rem;
      font-weight: 700;
      letter-spacing: -0.02em;
    }
    .nav-brand-text span {
      background: linear-gradient(135deg, #38bdf8, #818cf8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .nav-badge {
      font-size: 0.68rem;
      font-weight: 600;
      padding: 0.15rem 0.5rem;
      background: rgba(56, 189, 248, 0.15);
      color: #38bdf8;
      border: 1px solid rgba(56, 189, 248, 0.3);
      border-radius: 9999px;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .nav-tab {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      text-decoration: none;
      color: var(--text-muted);
      font-size: 0.88rem;
      font-weight: 500;
      padding: 0.45rem 0.85rem;
      border-radius: 8px;
      transition: all 0.2s ease;
    }
    .nav-tab:hover {
      color: var(--text-main);
      background: rgba(255, 255, 255, 0.05);
    }
    .nav-tab.active {
      color: #38bdf8;
      background: rgba(56, 189, 248, 0.1);
      border: 1px solid rgba(56, 189, 248, 0.25);
    }
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      padding: 0.45rem 0.95rem;
      font-size: 0.82rem;
      font-weight: 600;
      border-radius: 7px;
      text-decoration: none;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .btn-secondary {
      background: rgba(255, 255, 255, 0.08);
      color: #f8fafc;
      border: 1px solid rgba(255, 255, 255, 0.15);
    }
    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.15);
    }
    .btn-primary {
      background: linear-gradient(135deg, #0284c7 0%, #2563eb 100%);
      color: #ffffff;
      border: none;
    }
    @media (max-width: 768px) {
      .portal-nav { padding: 0.6rem 1rem; }
      .nav-links { display: none; }
    }
  </style>
`;

const navSnippet = `
  <nav class="portal-nav">
    <a href="index.html" class="nav-brand">
      <div class="nav-logo-icon">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242"></path>
          <path d="M12 12v9"></path>
          <path d="m8 17 4 4 4-4"></path>
        </svg>
      </div>
      <div class="nav-brand-text">
        isp<span>ERP</span>
      </div>
      <span class="nav-badge">v1.0.0</span>
    </a>

    <div class="nav-links">
      <a href="index.html" class="nav-tab">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
          <polyline points="9 22 9 12 15 12 15 22"></polyline>
        </svg>
        Sobre o Projeto
      </a>
      <a href="api.html" class="nav-tab active">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
          <polyline points="2 17 12 22 22 17"></polyline>
          <polyline points="2 12 12 17 22 12"></polyline>
        </svg>
        Contratos de API (Redocly)
      </a>
      <a href="index.html#pilares" class="nav-tab">Arquitetura</a>
      <a href="index.html#modulos" class="nav-tab">Módulos (21)</a>
    </div>

    <div class="nav-actions">
      <a href="openapi.bundled.json" download="ispERP-openapi.json" class="btn btn-secondary" title="Baixar especificação OpenAPI 3.0.3 compilada">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        Spec JSON
      </a>
      <a href="https://github.com/RuyXingubit/ispERP" target="_blank" rel="noopener noreferrer" class="btn btn-secondary">
        GitHub
      </a>
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
