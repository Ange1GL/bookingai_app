ALTER TABLE account_blocked
    ADD CONSTRAINT uk_account_blocked_user_id UNIQUE (user_id);
