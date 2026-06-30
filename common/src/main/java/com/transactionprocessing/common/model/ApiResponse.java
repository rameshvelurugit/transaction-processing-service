package com.transactionprocessing.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    Instant timestamp;
    String correlationId;
    T data;

    public static <T> ApiResponse<T> of(T data, String correlationId) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .data(data)
                .build();
    }
}
