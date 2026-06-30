package com.transactionprocessing.transaction.entity;

public enum TransactionStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    FAILED,
    RETRY_PENDING,
    DUPLICATE,
    PENDING
}
