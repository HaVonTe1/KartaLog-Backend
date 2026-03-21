#!/bin/bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
DB_CONTAINER="${DB_CONTAINER:-tcgwatcher-backend-postgres-1}"
DB_NAME="${DB_NAME:-tcgwatcherdb}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASSWORD="${POSTGRES_PASSWORD:-}"

export PGPASSWORD="$DB_PASSWORD"

show_backups() {
    echo "Available backups:"
    ls -lh "$BACKUP_DIR"/${DB_NAME}_*.sql.gz 2>/dev/null || echo "  No backups found in $BACKUP_DIR"
}

if [ $# -eq 0 ]; then
    echo "Usage: $0 <backup_file>"
    echo "       $0 --list"
    show_backups
    exit 1
fi

if [ "$1" = "--list" ]; then
    show_backups
    exit 0
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Restoring database ${DB_NAME} from $BACKUP_FILE..."

if [ -t 0 ]; then
    echo "WARNING: Interactive restore detected."
    read -p "This will overwrite the current database. Continue? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo "Restore cancelled."
        exit 0
    fi
fi

gunzip -c "$BACKUP_FILE" |  psql -U "$DB_USER" -d "$DB_NAME"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Restore completed successfully"
