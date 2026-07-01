package com.transactionprocessing.common.exception;

/**
 * Thrown when a requested resource, such as a transaction by ID, does not exist.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
}
