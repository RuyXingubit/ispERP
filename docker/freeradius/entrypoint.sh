#!/usr/bin/env bash
set -eo pipefail

echo "============================================================"
echo " FreeRADIUS ispERP Integration Entrypoint"
echo "============================================================"

DB_HOST="${DATABASE_HOST:-postgres}"
DB_PORT="${DATABASE_PORT:-5432}"
DB_NAME="${DATABASE_NAME:-isperp}"
DB_USER="${DATABASE_USER:-isperp}"
DB_PASS="${DATABASE_PASS:-password}"

echo "[FreeRADIUS] Aguardando banco PostgreSQL em ${DB_HOST}:${DB_PORT}..."
MAX_ATTEMPTS=30
ATTEMPT=0

until (echo > /dev/tcp/"${DB_HOST}"/"${DB_PORT}") >/dev/null 2>&1; do
    ATTEMPT=$((ATTEMPT + 1))
    if [ "$ATTEMPT" -ge "$MAX_ATTEMPTS" ]; then
        echo "[FreeRADIUS] ERRO: Timeout aguardando conexao com PostgreSQL em ${DB_HOST}:${DB_PORT}."
        exit 1
    fi
    echo "[FreeRADIUS] Banco indisponivel ainda... tentativa ${ATTEMPT}/${MAX_ATTEMPTS}. Aguardando 2s..."
    sleep 2
done

echo "[FreeRADIUS] Conexao TCP com PostgreSQL estabelecida com sucesso."

SQL_CONF="/etc/raddb/mods-available/sql"

if [ -f "$SQL_CONF" ]; then
    echo "[FreeRADIUS] Configurando modulo rlm_sql_postgresql..."
    
    # 1. Ajusta dialect e driver
    sed -i 's/^[[:space:]]*dialect = .*/\tdialect = "postgresql"/' "$SQL_CONF"
    sed -i 's/^[[:space:]]*driver = .*/\tdriver = "rlm_sql_postgresql"/' "$SQL_CONF"

    # 2. Injeta string de conexao segura do PostgreSQL
    sed -i "s|^[[:space:]]*radius_db = .*|\tradius_db = \"host=${DB_HOST} port=${DB_PORT} dbname=${DB_NAME} user=${DB_USER} password=${DB_PASS}\"|" "$SQL_CONF"

    # 3. Habilita leitura dinamica de concentradores BNG/NAS direto da tabela 'nas'
    sed -i 's/^[[:space:]]*#[[:space:]]*read_clients = yes/\tread_clients = yes/' "$SQL_CONF"
    sed -i 's/^[[:space:]]*client_table = .*/\tclient_table = "nas"/' "$SQL_CONF"

    # 4. Remove sessoes antigas/mortas
    sed -i 's/^[[:space:]]*delete_stale_sessions = .*/\tdelete_stale_sessions = yes/' "$SQL_CONF"

    # 5. Habilita o modulo sql em mods-enabled
    mkdir -p /etc/raddb/mods-enabled
    ln -sf "$SQL_CONF" /etc/raddb/mods-enabled/sql

    echo "[FreeRADIUS] Modulo SQL configurado e habilitado."
else
    echo "[FreeRADIUS] AVISO: Arquivo $SQL_CONF nao encontrado!"
fi

echo "[FreeRADIUS] Validando sintaxe da configuracao com freeradius -C..."
freeradius -C

echo "[FreeRADIUS] Configuracao valida. Inicializando servidor..."
exec "$@"
