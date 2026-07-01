package com.transactionprocessing.common.exception;

/**
 * Thrown when a transaction submission is identified as a duplicate of an already-processed request.
 */
public class DuplicateTransactionException extends BaseException {

    public DuplicateTransactionException(String message) {
        super(message, "DUPLICATE_TRANSACTION");
    }
}
