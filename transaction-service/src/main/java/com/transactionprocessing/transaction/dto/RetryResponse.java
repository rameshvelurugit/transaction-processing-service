package com.transactionprocessing.transaction.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RetryResponse {

    int retriedCount;
    int successCount;
    int stillFailedCount;
    String message;
}
