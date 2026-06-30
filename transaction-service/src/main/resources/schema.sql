CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(16) PRIMARY KEY,
    balance DECIMAL(19, 2) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    sequence_number INT NOT NULL,
    account_id VARCHAR(16) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    CONSTRAINT uk_transaction_request UNIQUE (transaction_id, request_id)
);

CREATE TABLE IF NOT EXISTS processed_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    internal_transaction_id BIGINT NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_processed_txn_request UNIQUE (transaction_id, request_id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_account_seq ON transactions(account_id, sequence_number);
