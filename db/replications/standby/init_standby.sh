#!/usr/bin/env bash
set -euo pipefail

# Physical standby: on the first start clone the master with pg_basebackup,
# then run PostgreSQL in hot standby mode (read-only, replays master's WAL).

if [ ! -s "$PGDATA/PG_VERSION" ]; then
  echo "Empty data directory: waiting for master..."
  until pg_isready -h db -p 5432 -U repuser; do
    sleep 2
  done

  echo "Cloning master with pg_basebackup..."
  mkdir -p "$PGDATA"
  chown -R postgres:postgres "$PGDATA"
  chmod 700 "$PGDATA"

  # -X stream: copy WAL during the backup; -C -S: create a physical replication
  # slot on the master so it keeps WAL until the replica has received it
  su-exec postgres env PGPASSWORD=rep_pass \
    pg_basebackup -h db -p 5432 -U repuser -D "$PGDATA" -X stream -C -S replica1_slot

  # Connection to the master; application_name identifies this replica
  echo "primary_conninfo = 'host=db port=5432 user=repuser password=rep_pass application_name=replica1'" \
    >> "$PGDATA/postgresql.auto.conf"
  echo "primary_slot_name = 'replica1_slot'" >> "$PGDATA/postgresql.auto.conf"

  # standby.signal tells PostgreSQL to start as a replica
  su-exec postgres touch "$PGDATA/standby.signal"
  echo "Standby created."
fi

exec su-exec postgres postgres -c listen_addresses='*' -c hot_standby=on