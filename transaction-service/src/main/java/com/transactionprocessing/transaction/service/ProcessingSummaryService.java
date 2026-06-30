package com.transactionprocessing.transaction.service;

import com.transactionprocessing.transaction.dto.ProcessingSummaryResponse;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProcessingSummaryService {

    private final TransactionRepository transactionRepository;
    private volatile int processedInLastRun;

    public ProcessingSummaryResponse getSummary() {
        Map<String, Long> breakdown = new HashMap<>();
        for (TransactionStatus status : TransactionStatus.values()) {
            breakdown.put(status.name(), transactionRepository.countByStatus(status));
        }

        long total = breakdown.values().stream().mapToLong(Long::longValue).sum();

        return ProcessingSummaryResponse.builder()
                .total(total)
                .received(breakdown.getOrDefault(TransactionStatus.RECEIVED.name(), 0L))
                .processing(breakdown.getOrDefault(TransactionStatus.PROCESSING.name(), 0L))
                .processed(breakdown.getOrDefault(TransactionStatus.PROCESSED.name(), 0L))
                .failed(breakdown.getOrDefault(TransactionStatus.FAILED.name(), 0L))
                .retryPending(breakdown.getOrDefault(TransactionStatus.RETRY_PENDING.name(), 0L))
                .duplicate(breakdown.getOrDefault(TransactionStatus.DUPLICATE.name(), 0L))
                .pending(breakdown.getOrDefault(TransactionStatus.PENDING.name(), 0L))
                .processedInLastRun(processedInLastRun)
                .statusBreakdown(breakdown)
                .build();
    }

    public void setProcessedInLastRun(int count) {
        this.processedInLastRun = count;
    }
}
