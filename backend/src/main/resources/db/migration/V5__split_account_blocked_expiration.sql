alter table account_blocked
    rename column expirated_at to end_expirated_at;

alter table account_blocked
    add column start_expirated_at timestamp(6) with time zone;
