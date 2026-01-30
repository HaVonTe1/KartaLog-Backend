-- sehr gute Beschreibung: https://dba.stackexchange.com/questions/117109/how-to-manage-default-privileges-for-users-on-a-database-vs-schema

-- Database and default public privileges are managed externally.
-- Connecting to the target database is handled by Spring datasource.
create schema watcher;

create user watcher_readonly with password '${watcher_readonly.password}' login;
alter role watcher_readonly in database tcgwatcherdb set search_path = watcher,public;
grant connect on database tcgwatcherdb to watcher_readonly;
grant create, connect on database tcgwatcherdb to watcher_readonly;
grant create, usage on schema watcher to watcher_readonly;

create user migration with password '${migration.password}' login;
alter role migration in database tcgwatcherdb set search_path = watcher,public;
grant connect on database tcgwatcherdb to migration;
grant create, connect on database tcgwatcherdb to migration;
grant create, usage on schema watcher to migration;

create user watcher_mig with password '${watcher_mig.password}' login;
alter role watcher_mig in database tcgwatcherdb set search_path = watcher,public;
grant connect on database tcgwatcherdb to watcher_mig;
grant create, connect on database tcgwatcherdb to watcher_mig;
grant create, usage on schema watcher to watcher_mig;



create user watcher_app with password '${watcher_app.password}';
alter role watcher_app in database tcgwatcherdb set search_path = watcher,public;
grant connect on database tcgwatcherdb to watcher_app;
grant usage on schema watcher to watcher_app;

create user application with password '${application.password}';
alter role application in database tcgwatcherdb set search_path = watcher,public;
grant connect on database tcgwatcherdb to application;
grant usage on schema watcher to application;

alter default privileges for role migration
    grant select, insert, update, delete, truncate, references on tables to application;
alter default privileges for role migration
    grant usage, select, update on sequences to application;

alter default privileges for role watcher_mig
    grant select, insert, update, delete, truncate, references on tables to watcher_app;
alter default privileges for role watcher_mig
    grant usage, select, update on sequences to watcher_app;

create extension if not exists "uuid-ossp" schema "public";
