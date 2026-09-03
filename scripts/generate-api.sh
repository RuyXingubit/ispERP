#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Orquestrador de Geração de Código API-First do ispERP
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "🚀 [API-First] 1/3 Compilando bundle de contratos OpenAPI..."
"${SCRIPT_DIR}/bundle-contracts.sh"

echo "⚙️ [API-First] 2/3 Gerando interfaces e DTOs no Backend (Gradle)..."
(cd "${ROOT_DIR}/backend" && ./gradlew openApiGenerate eclipse idea --no-daemon)

echo "💻 [API-First] 3/3 Gerando modelos e clientes HTTP no Frontend (Orval)..."
(cd "${ROOT_DIR}/frontend" && npm run codegen)

echo "🎉 [API-First] Geração completa de contratos (Backend + Frontend) concluída com sucesso!"
