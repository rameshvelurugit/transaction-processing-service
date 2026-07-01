package com.transactionprocessing.transaction.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Summary of a manual retry batch operation, including success and remaining failure counts.
 */
@Value
@Builder
public class RetryResponse {

    int retriedCount;
    int successCount;
    int stillFailedCount;
    String message;
}
