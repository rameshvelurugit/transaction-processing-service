package com.transactionprocessing.transaction.scheduler;

import com.transactionprocessing.transaction.service.TransactionProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers background processing of pending transactions on a configurable fixed delay.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProcessorScheduler {

    private final TransactionProcessingService transactionProcessingService;

    @Value("${transaction.processing.scheduler.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${transaction.processing.scheduler.fixed-delay-ms:30000}")
    public void runScheduledProcessing() {
        if (!enabled) {
            return;
        }
        log.debug("Scheduled background processing tick");
        transactionProcessingService.processPendingTransactions();
    }
}
