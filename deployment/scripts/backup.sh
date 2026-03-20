#!/bin/bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
DB_CONTAINER="${DB_CONTAINER:-tcgwatcher-backend-postgres-1}"
DB_NAME="${DB_NAME:-tcgwatcherdb}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASSWORD="${POSTGRES_PASSWORD:-}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

export PGPASSWORD="$DB_PASSWORD"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting backup of ${DB_NAME}..."

docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --no-acl | gzip > "$BACKUP_FILE"

BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup created: $BACKUP_FILE ($BACKUP_SIZE)"

if [ -n "$RETENTION_DAYS" ] && [ "$RETENTION_DAYS" -gt 0 ]; then
    find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime "+$RETENTION_DAYS" -delete
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Old backups (>${RETENTION_DAYS} days) removed"
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup completed successfully"
