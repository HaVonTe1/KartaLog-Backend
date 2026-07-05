#!/bin/bash
set -e

SQL=$(cat <<EOSQL
CREATE SCHEMA IF NOT EXISTS watcher;

create user watcher_readonly with password '${WATCHER_READONLY_PWD}' login;
alter role watcher_readonly in database kartalogdb set search_path = watcher,public;
grant connect on database kartalogdb to watcher_readonly;
grant create, connect on database kartalogdb to watcher_readonly;
grant create, usage on schema watcher to watcher_readonly;

create user watcher_mig with password '${WATCHER_MIG_PWD}' login;
alter role watcher_mig in database kartalogdb set search_path = watcher,public;
grant connect on database kartalogdb to watcher_mig;
grant create, connect on database kartalogdb to watcher_mig;
grant create, usage on schema watcher to watcher_mig;

create user watcher_app with password '${WATCHER_APP_PWD}';
alter role watcher_app in database kartalogdb set search_path = watcher,public;
grant connect on database kartalogdb to watcher_app;
grant usage on schema watcher to watcher_app;


alter default privileges for role watcher_mig
    grant select, insert, update, delete, truncate, references on tables to watcher_app;
alter default privileges for role watcher_mig
    grant usage, select, update on sequences to watcher_app;

create extension if not exists "uuid-ossp" schema "public";
EOSQL
)

echo "$SQL" | psql -U "$POSTGRES_USER" -d kartalogdb
