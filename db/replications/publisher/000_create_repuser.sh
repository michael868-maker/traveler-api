#!/usr/bin/env bash
set -euo pipefail

# Replication user is created from environment variables (.env), not hardcoded
: "${REPL_USER:?REPL_USER is not set}"
: "${REPL_PASSWORD:?REPL_PASSWORD is not set}"

# psql variables (:"name" for identifiers, :'name' for literals) are escaped safely
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -v repl_user="$REPL_USER" -v repl_password="$REPL_PASSWORD" <<'SQL'
CREATE ROLE :"repl_user" WITH LOGIN REPLICATION PASSWORD :'repl_password';
SQL