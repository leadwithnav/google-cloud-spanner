-- =========================================================================
-- Cloud Spanner Advanced DDL Schema (PostgreSQL Dialect) - Lab 15
-- Features:
-- 1. Parent Table: customers (UUID Primary Key)
-- 2. Interleaved Child Table: accounts (INTERLEAVE IN PARENT customers ON DELETE CASCADE)
--    Physical co-location stores child accounts on the exact same split node as parent customer
-- 3. Secondary Covering Indexes with STORING clause:
--    Eliminates 2-step table backjoins by storing payload columns directly in the index split
-- =========================================================================

-- 0. Clean Up Previous Schema (Drop Child Tables & Indexes First to Prevent Dependency Conflicts)
DROP INDEX IF EXISTS idx_payments_source;
DROP INDEX IF EXISTS idx_accounts_type;
DROP INDEX IF EXISTS idx_accounts_customer;
DROP TABLE IF EXISTS payment_transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS customers;

-- 1. Customers Table (Parent Table)
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (customer_id)
);

-- 2. Accounts Table (Interleaved Child Table)
-- BEST PRACTICE 1: INTERLEAVE IN PARENT customers co-locates accounts rows physically with parent customer
CREATE TABLE IF NOT EXISTS accounts (
    customer_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    balance NUMERIC NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (customer_id, account_id)
) INTERLEAVE IN PARENT customers ON DELETE CASCADE;

-- BEST PRACTICE 2: Covering Index with INCLUDE clause eliminates 2-step table backjoins in PostgreSQL dialect
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type) INCLUDE (currency, balance, updated_at);

-- 3. Payment Transactions Table
CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id VARCHAR(36) NOT NULL,
    source_account_id VARCHAR(36) NOT NULL,
    target_account_id VARCHAR(36) NOT NULL,
    amount NUMERIC NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (transaction_id)
);

-- BEST PRACTICE 2: Covering Index with INCLUDE clause for high-speed transaction auditing queries
CREATE INDEX IF NOT EXISTS idx_payments_source ON payment_transactions(source_account_id) INCLUDE (target_account_id, amount, status, created_at);
