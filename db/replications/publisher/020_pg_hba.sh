#!/usr/bin/env bash
set -euo pipefail
: "${REPL_USER:?REPL_USER is not set}"

# Replace the permissive default pg_hba.conf ("host all all all") with strict rules.
# samenet = only addresses from the container's own Docker network.
cat > "${PGDATA}/pg_hba.conf" <<EOF
# TYPE  DATABASE        USER              ADDRESS   METHOD
# Local socket inside the container only (init scripts, healthcheck, docker exec)
local   all             all                         trust
# Application user: only its own database, only from the Docker network
host    ${POSTGRES_DB}  ${POSTGRES_USER}  samenet   scram-sha-256
# Replication user: only physical replication, only from the Docker network
host    replication     ${REPL_USER}      samenet   scram-sha-256
EOF