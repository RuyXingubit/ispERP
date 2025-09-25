#!/bin/bash
set -e

host="mysql"
port="3306"
user="isperp"
password="root"

echo "Aguardando MySQL estar disponível..."

until mysql -h"$host" -P"$port" -u"$user" -p"$password" --skip-ssl -e "SELECT 1;" > /dev/null 2>&1; do
  echo "MySQL não está pronto - aguardando..."
  sleep 2
done

echo "MySQL está pronto!"

# Execute o comando passado como parâmetro
exec "$@"