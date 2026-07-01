package com.transactionprocessing.transaction.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Aggregated transaction status counts and metrics from the most recent processing run.
 */
@Value
@Builder
public class ProcessingSummaryResponse {

    long total;
    long received;
    long processing;
    long processed;
    long failed;
    long retryPending;
    long duplicate;
    long pending;
    int processedInLastRun;
    Map<String, Long> statusBreakdown;
}
