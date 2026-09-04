#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script de Validação e Bundling dos Contratos OpenAPI do ispERP
# ==============================================================================

# Desativar telemetria e avisos de update para evitar bloqueios ou travamentos em CI/CD
export REDOCLY_TELEMETRY=off
export REDOCLY_SUPPRESS_UPDATE_NOTICE=true
export CI=true

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

REDOCLY_BIN="npx -y @redocly/cli"
if command -v redocly >/dev/null 2>&1; then
  REDOCLY_BIN="redocly"
fi

echo "📦 [API-First] Compilando e resolvendo \$ref com Redocly..."
${REDOCLY_BIN} bundle "${INPUT_SPEC}" --output "${OUTPUT_BUNDLE}"

echo "🛡️ [API-First] Executando linting na especificação compilada..."
${REDOCLY_BIN} lint "${OUTPUT_BUNDLE}"

echo "✅ [API-First] Contratos compilados com sucesso em: ${OUTPUT_BUNDLE}"
