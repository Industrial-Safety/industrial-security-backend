#!/bin/bash
set -e
echo "Configurando replicación en pg_hba.conf..."
echo "host replication replicator 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
echo "host replication all 0.0.0.0/0 trust" >> "$PGDATA/pg_hba.conf"
echo "Listo."