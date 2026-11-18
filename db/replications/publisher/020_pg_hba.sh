#!/usr/bin/env bash
set -euo pipefail
# Physical replication: allow repuser to connect to the special "replication" database
echo "host replication repuser 0.0.0.0/0 md5" >> "${PGDATA}/pg_hba.conf"