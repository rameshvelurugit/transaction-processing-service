package com.transactionprocessing.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Structured error payload returned by REST controllers and the API gateway when a request fails.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    Instant timestamp;
    int status;
    String error;
    String errorCode;
    String message;
    String path;
    String correlationId;
    List<FieldError> fieldErrors;

    /**
     * Describes a single field-level validation failure included in an error response.
     */
    @Value
    @Builder
    public static class FieldError {
        String field;
        String message;
        Object rejectedValue;
    }
}
