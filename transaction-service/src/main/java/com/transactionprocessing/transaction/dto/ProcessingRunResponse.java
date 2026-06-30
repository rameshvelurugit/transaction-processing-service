package com.transactionprocessing.transaction.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProcessingRunResponse {

    int processedCount;
    int failedCount;
    int pendingCount;
    int duplicateCount;
    String message;
}
