-- =========================================================================
-- Cloud Spanner DDL Schema (PostgreSQL Dialect)
-- Deployed via Spanner Studio or gcloud CLI:
-- gcloud spanner databases ddl update spanner-bank-db --instance=test-instance --ddl-file=schema.sql
-- =========================================================================

-- 1. Customers Table (UUID Primary Key to prevent monotonic write hotspots)
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL OPTIONS (allow_commit_timestamp = true),
    PRIMARY KEY (customer_id)
);

-- 2. Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    balance NUMERIC NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL OPTIONS (allow_commit_timestamp = true),
    PRIMARY KEY (account_id)
);

-- Secondary Index on customer_id to optimize account lookups by customer
CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);

-- 3. Payment Transactions Table
CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id VARCHAR(36) NOT NULL,
    source_account_id VARCHAR(36) NOT NULL,
    target_account_id VARCHAR(36) NOT NULL,
    amount NUMERIC NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL OPTIONS (allow_commit_timestamp = true),
    PRIMARY KEY (transaction_id)
);

-- Secondary Index for listing transactions by source account
CREATE INDEX IF NOT EXISTS idx_payments_source ON payment_transactions(source_account_id);
