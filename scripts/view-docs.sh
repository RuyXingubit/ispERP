#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Visualizador de Documentação Interativa OpenAPI / Redocly do ispERP
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUNDLE_PATH="${ROOT_DIR}/contracts/openapi/openapi.bundled.json"
PORT="${PORT:-8085}"

echo "📦 [Docs] 1/2 Garantindo que o bundle OpenAPI está atualizado..."
"${SCRIPT_DIR}/bundle-contracts.sh"

# Modo de compilação do portal completo (Landing Page + Redocly)
if [[ "${1:-}" == "--portal" ]]; then
  echo "🌐 [Docs] Compilando e abrindo o Portal ispERP..."
  "${SCRIPT_DIR}/build-portal.sh"
  if command -v open >/dev/null 2>&1; then
    open "${ROOT_DIR}/docs/portal/index.html"
  fi
  exit 0
fi

# Modo de exportação para arquivo HTML único estático
if [[ "${1:-}" == "--build" || "${1:-}" == "--html" ]]; then
  OUTPUT_HTML="${ROOT_DIR}/docs/api-reference.html"
  echo "📄 [Docs] Gerando arquivo HTML estático em: ${OUTPUT_HTML}..."
  npx -y @redocly/cli build-docs "${BUNDLE_PATH}" --output "${OUTPUT_HTML}"
  echo "✅ [Docs] Documentação estática compilada com sucesso!"
  if command -v open >/dev/null 2>&1; then
    open "${OUTPUT_HTML}"
  fi
  exit 0
fi

# Modo servidor interativo com Live-Reload
echo "📖 [Docs] 2/2 Iniciando servidor interativo Redocly em http://localhost:${PORT}..."
echo "💡 Dica: Para parar o servidor, pressione Ctrl+C no terminal."

# Abre a aba no navegador automaticamente no macOS
if command -v open >/dev/null 2>&1; then
  (sleep 2 && open "http://localhost:${PORT}") &
fi

npx -y @redocly/cli preview-docs "${BUNDLE_PATH}" --port "${PORT}"
