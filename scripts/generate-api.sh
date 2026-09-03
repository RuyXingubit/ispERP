#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Orquestrador de Geração de Código API-First do ispERP
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "🚀 [API-First] 1/2 Compilando bundle de contratos OpenAPI..."
"${SCRIPT_DIR}/bundle-contracts.sh"

echo "⚙️ [API-First] 2/2 Gerando interfaces e DTOs no Backend (Gradle)..."
cd "${ROOT_DIR}/backend"
./gradlew openApiGenerate --no-daemon

echo "🎉 [API-First] Geração concluída com sucesso!"
