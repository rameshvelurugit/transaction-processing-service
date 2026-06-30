package com.transactionprocessing.transaction.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

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
