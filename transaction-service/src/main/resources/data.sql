-- L1: Basic happy-path and duplicate scenarios
INSERT INTO accounts (account_id, balance, updated_at) VALUES ('ACC-1001', 1000.00, CURRENT_TIMESTAMP);
INSERT INTO accounts (account_id, balance, updated_at) VALUES ('ACC-1002', 500.00, CURRENT_TIMESTAMP);
INSERT INTO accounts (account_id, balance, updated_at) VALUES ('ACC-2001', 2000.00, CURRENT_TIMESTAMP);

-- L1 seq 1: credit (will process)
INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L1-001', 'REQ-L1-001', 1, 'ACC-1001', 100.00, 'CREDIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- L1 seq 2: debit (will process after seq 1)
INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L1-002', 'REQ-L1-002', 2, 'ACC-1001', 50.00, 'DEBIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- L1 duplicate scenario is exercised via API/tests (unique constraint prevents duplicate seed row)

-- L2: Out-of-order (seq 2 arrives before seq 1 for ACC-2001)
INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L2-OO-002', 'REQ-L2-OO-002', 2, 'ACC-2001', 75.00, 'DEBIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L2-OO-001', 'REQ-L2-OO-001', 1, 'ACC-2001', 150.00, 'CREDIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- L2: Retry scenario (transient failure amount 999.99)
INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L2-RETRY', 'REQ-L2-RETRY', 1, 'ACC-1002', 999.99, 'CREDIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- L2: Insufficient funds (invalid for processing - permanent failure)
INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L2-FAIL', 'REQ-L2-FAIL', 1, 'ACC-1002', 10000.00, 'DEBIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Additional L1 credit on ACC-1002
INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L1-003', 'REQ-L1-003', 2, 'ACC-1002', 25.00, 'CREDIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_id, request_id, sequence_number, account_id, amount, type, status, retry_count, created_at, updated_at)
VALUES ('TXN-L1-004', 'REQ-L1-004', 1, 'ACC-1002', 10.00, 'CREDIT', 'RECEIVED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
