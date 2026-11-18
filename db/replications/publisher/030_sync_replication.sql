-- Enable synchronous replication only after the initialization.
-- If it is passed in "command", the temporary init server waits for a replica
-- that cannot exist yet, and CREATE DATABASE hangs forever (deadlock).
-- ALTER SYSTEM writes postgresql.auto.conf, applied when the real server starts.
ALTER SYSTEM SET synchronous_standby_names = 'FIRST 1 (replica1)';