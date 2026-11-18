#!/usr/bin/env bash
set -euo pipefail
# Physical replication: allow the replication user (from .env) to connect
echo "host replication ${REPL_USER} 0.0.0.0/0 md5" >> "${PGDATA}/pg_hba.conf"