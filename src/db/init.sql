DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
SET search_path TO public;

CREATE EXTENSION "pgcrypto";

CREATE DOMAIN account_id AS CHAR(8)
    CHECK (VALUE ~ '^(SYS|ACT)-[0-9]{4}$');
CREATE DOMAIN amount_in_cents AS BIGINT;
CREATE TYPE transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER');
CREATE TYPE account_type AS ENUM ('CUSTOMER', 'SYSTEM');

CREATE TABLE accounts (
    account_id account_id PRIMARY KEY,
    account_type account_type NOT NULL DEFAULT 'CUSTOMER',
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    pin VARCHAR(100),
    balance amount_in_cents NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_non_negative_balance CHECK (
        account_type = 'SYSTEM' OR balance >= 0
    ),
    CONSTRAINT chk_account_type CHECK (
        (
            account_type = 'CUSTOMER' AND
            first_name IS NOT NULL AND
            last_name IS NOT NULL AND
            pin IS NOT NULL
        ) OR (
            account_type = 'SYSTEM' AND
            first_name IS NULL AND
            last_name IS NULL AND
            pin IS NULL
        )
    )
);

CREATE SEQUENCE account_id_seq 
    MINVALUE 0 
    START WITH 1000 
    INCREMENT BY 1;

CREATE OR REPLACE FUNCTION set_account_id()
RETURNS TRIGGER AS $$
DECLARE
    v_prefix CHAR(3);
BEGIN
    v_prefix := CASE WHEN NEW.account_type = 'SYSTEM' THEN 'SYS' ELSE 'ACT' END;
    NEW.account_id := v_prefix || '-' || lpad(nextval('account_id_seq')::TEXT, 4, '0');
    RETURN NEW; -- SYS-0000 OR ACT-0000
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_account_id
BEFORE INSERT ON accounts
FOR EACH ROW EXECUTE FUNCTION set_account_id();

CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type transaction_type NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_entries (
    entry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(transaction_id) ON DELETE RESTRICT,
    account_id account_id NOT NULL REFERENCES accounts(account_id) ON DELETE RESTRICT,
    amount amount_in_cents NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_entries_account_created 
ON ledger_entries (account_id, created_at DESC);

CREATE INDEX idx_ledger_entries_transaction 
ON ledger_entries (transaction_id);

CREATE VIEW audits AS
SELECT 
    l.account_id,
    a.first_name || ' ' || a.last_name AS account_holder,
    t.transaction_type,
    CASE 
        WHEN l.amount > 0 THEN 'CREDIT'
        ELSE 'DEBIT'
    END AS direction,
    l.amount,
    t.description,
    t.created_at
FROM ledger_entries l
JOIN transactions t ON l.transaction_id = t.transaction_id
JOIN accounts a ON a.account_id = l.account_id
-- WHERE a.account_type != 'SYSTEM'
ORDER BY t.created_at DESC;

CREATE OR REPLACE FUNCTION transfer(
    p_sender_id account_id,
    p_receiver_id account_id,
    p_amount amount_in_cents,
    p_description TEXT,
    p_type transaction_type DEFAULT 'TRANSFER'
) RETURNS UUID AS $$
DECLARE
    v_tx_id UUID;
    v_sender_balance amount_in_cents;
    v_sender_type account_type;
    v_description TEXT;
BEGIN
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Transfer amount must be positive.';
    END IF;

    IF p_sender_id = p_receiver_id THEN
        RAISE EXCEPTION 'Sender and receiver accounts must be different.';
    END IF;

    -- Lock rows in deterministic order to prevent deadlocks
    IF p_sender_id < p_receiver_id THEN
        PERFORM 1 FROM accounts WHERE account_id = p_sender_id FOR UPDATE;
        PERFORM 1 FROM accounts WHERE account_id = p_receiver_id FOR UPDATE;
    ELSE
        PERFORM 1 FROM accounts WHERE account_id = p_receiver_id FOR UPDATE;
        PERFORM 1 FROM accounts WHERE account_id = p_sender_id FOR UPDATE;
    END IF;

    -- Fetch sender balance and type
    SELECT balance, account_type 
    INTO v_sender_balance, v_sender_type 
    FROM accounts WHERE account_id = p_sender_id;

    IF v_sender_balance IS NULL THEN
        RAISE EXCEPTION 'Sender account % does not exist.', p_sender_id;
    END IF;

    -- Skip insufficient funds check if sender is SYSTEM
    IF v_sender_type != 'SYSTEM' AND v_sender_balance < p_amount THEN
        RAISE EXCEPTION 'Insufficient funds.';
    END IF;

    IF p_type = 'TRANSFER' THEN
        SELECT '[' || p_sender_id || ' → ' || p_receiver_id || '] ' || p_description INTO v_description;
    ELSE
        SELECT p_description INTO v_description;
    END IF;

    -- Create Header Transaction
    INSERT INTO transactions (transaction_type, description)
    VALUES (p_type, v_description)
    RETURNING transaction_id INTO v_tx_id;

    -- Create Balanced Ledger Entries
    INSERT INTO ledger_entries (transaction_id, account_id, amount) VALUES
        (v_tx_id, p_sender_id, -p_amount),
        (v_tx_id, p_receiver_id, p_amount);

    -- Update Cached Balances
    UPDATE accounts SET balance = balance - p_amount WHERE account_id = p_sender_id;
    UPDATE accounts SET balance = balance + p_amount WHERE account_id = p_receiver_id;

    RETURN v_tx_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION deposit(
    p_customer_id account_id,
    p_amount amount_in_cents,
    p_description TEXT DEFAULT 'Account Deposit'
) RETURNS UUID AS $$
DECLARE
    v_clearing_id account_id;
BEGIN
    SELECT account_id INTO v_clearing_id 
    FROM accounts 
    WHERE account_type = 'SYSTEM' 
    LIMIT 1;

    RETURN transfer(
        p_sender_id   => v_clearing_id,
        p_receiver_id => p_customer_id,
        p_amount      => p_amount,
        p_description => p_description,
        p_type        => 'DEPOSIT'
    );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION withdraw(
    p_customer_id account_id,
    p_amount amount_in_cents,
    p_description TEXT DEFAULT 'Account Withdrawal'
) RETURNS UUID AS $$
DECLARE
    v_clearing_id account_id;
BEGIN
    SELECT account_id INTO v_clearing_id 
    FROM accounts 
    WHERE account_type = 'SYSTEM' 
    LIMIT 1;

    RETURN transfer(
        p_sender_id   => p_customer_id,
        p_receiver_id => v_clearing_id,
        p_amount      => p_amount,
        p_description => p_description,
        p_type        => 'WITHDRAWAL'
    );
END;
$$ LANGUAGE plpgsql;

---------------------------------------------------------------

INSERT INTO accounts (account_type, first_name, last_name, pin) 
VALUES
    ('SYSTEM', NULL, NULL, NULL),
    ('CUSTOMER', 'Thomas', 'Nguyen', '1234'),
    ('CUSTOMER', 'James', 'Gosling', '5678')
RETURNING *;

SELECT deposit(
    p_customer_id => 'ACT-1001',
    p_amount      => 10000,
    p_description => 'Initial deposit'
);

SELECT transfer(
    p_sender_id   => 'ACT-1001',
    p_receiver_id => 'ACT-1002',
    p_amount      => 1500,
    p_description => 'IOU'
);

SELECT withdraw(
    p_customer_id => 'ACT-1002',
    p_amount      => 500,
    p_description => 'ATM cash withdrawal'
);

SELECT * FROM ledger_entries;
SELECT * FROM audits;
SELECT SUM(amount) FROM audits;