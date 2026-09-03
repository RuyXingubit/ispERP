#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script de Validação e Bundling dos Contratos OpenAPI do ispERP
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CONTRACTS_DIR="${ROOT_DIR}/contracts/openapi"
INPUT_SPEC="${CONTRACTS_DIR}/openapi.yaml"
OUTPUT_BUNDLE="${CONTRACTS_DIR}/openapi.bundled.json"

echo "🔍 [API-First] Validando contratos em: ${INPUT_SPEC}"

if [ ! -f "${INPUT_SPEC}" ]; then
  echo "❌ Erro: Arquivo de especificação não encontrado em ${INPUT_SPEC}"
  exit 1
fi

echo "📦 [API-First] Compilando e resolvendo \$ref com Redocly..."
npx -y @redocly/cli bundle "${INPUT_SPEC}" --output "${OUTPUT_BUNDLE}"

echo "🛡️ [API-First] Executando linting na especificação compilada..."
npx -y @redocly/cli lint "${OUTPUT_BUNDLE}"

echo "✅ [API-First] Contratos compilados com sucesso em: ${OUTPUT_BUNDLE}"
