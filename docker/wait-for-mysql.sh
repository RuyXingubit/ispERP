#!/bin/bash
set -e

host="mysql"
port="3306"

echo "Aguardando MySQL estar disponível..."

until timeout 1 bash -c "echo > /dev/tcp/$host/$port" 2>/dev/null; do
  echo "MySQL não está pronto - aguardando..."
  sleep 2
done

echo "MySQL está pronto!"

# Execute o comando passado como parâmetro
exec "$@"