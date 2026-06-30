package com.transactionprocessing.common.exception;

public class RetryableException extends BaseException {

    public RetryableException(String message) {
        super(message, "RETRYABLE_ERROR");
    }

    public RetryableException(String message, Throwable cause) {
        super(message, "RETRYABLE_ERROR", cause);
    }
}
