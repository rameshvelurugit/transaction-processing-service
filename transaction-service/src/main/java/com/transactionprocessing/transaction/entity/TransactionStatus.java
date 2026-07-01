package com.transactionprocessing.transaction.entity;

/**
 * Lifecycle states a transaction passes through from ingestion to final outcome.
 */
public enum TransactionStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    FAILED,
    RETRY_PENDING,
    DUPLICATE,
    PENDING
}
