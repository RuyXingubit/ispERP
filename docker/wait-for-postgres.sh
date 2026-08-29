#!/bin/bash
set -e

host="${DB_HOST:-postgres}"
port="${DB_PORT:-5432}"

echo "Aguardando PostgreSQL ($host:$port) estar disponível..."

until timeout 1 bash -c "echo > /dev/tcp/$host/$port" 2>/dev/null; do
  echo "PostgreSQL não está pronto - aguardando..."
  sleep 2
done

echo "PostgreSQL está pronto!"

# Execute o comando passado como parâmetro
exec "$@"
