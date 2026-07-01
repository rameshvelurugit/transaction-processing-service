package com.transactionprocessing.transaction.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Result summary returned after triggering an on-demand background processing run.
 */
@Value
@Builder
public class ProcessingRunResponse {

    int processedCount;
    int failedCount;
    int pendingCount;
    int duplicateCount;
    String message;
}
