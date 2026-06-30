package com.transactionprocessing.common.exception;

public class DuplicateTransactionException extends BaseException {

    public DuplicateTransactionException(String message) {
        super(message, "DUPLICATE_TRANSACTION");
    }
}
