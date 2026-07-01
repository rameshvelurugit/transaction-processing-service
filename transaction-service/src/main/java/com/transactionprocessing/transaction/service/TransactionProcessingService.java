package com.transactionprocessing.transaction.service;

import com.transactionprocessing.common.constants.TransactionConstants;
import com.transactionprocessing.common.exception.RetryableException;
import com.transactionprocessing.common.logging.MdcContext;
import com.transactionprocessing.transaction.entity.Account;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.AccountRepository;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import com.transactionprocessing.transaction.util.ProcessingSimulator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core processing engine that applies balance updates, enforces per-account sequencing, and handles retries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IdempotencyService idempotencyService;
    private final RetryService retryService;
    private final ProcessingSummaryService processingSummaryService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public int processPendingTransactions() {
        log.info("Background processing started");
        List<TransactionStatus> eligible = List.of(
                TransactionStatus.RECEIVED,
                TransactionStatus.RETRY_PENDING
        );
        List<Transaction> candidates = transactionRepository.findByStatusInOrderByAccountIdAscSequenceNumberAsc(eligible);

        int processedCount = 0;
        int failedCount = 0;
        int pendingCount = 0;
        int duplicateCount = 0;

        for (Transaction transaction : candidates) {
            ProcessingOutcome outcome = processSingle(transaction);
            switch (outcome) {
                case PROCESSED -> processedCount++;
                case FAILED -> failedCount++;
                case PENDING -> pendingCount++;
                case DUPLICATE -> duplicateCount++;
                default -> { }
            }
        }

        int chainProcessed = processPendingChains();
        processedCount += chainProcessed;

        processingSummaryService.setProcessedInLastRun(processedCount);
        log.info("Background processing completed: processed={}, failed={}, pending={}, duplicate={}",
                processedCount, failedCount, pendingCount, duplicateCount);
        meterRegistry.counter("transactions.processing.runs").increment();
        return processedCount;
    }

    @Transactional
    public ProcessingOutcome processSingle(Transaction transaction) {
        MdcContext.setTransactionId(transaction.getTransactionId());
        MdcContext.setRequestId(transaction.getRequestId());

        if (duplicateDetectionService.hasProcessedRecord(transaction.getTransactionId(), transaction.getRequestId())) {
            duplicateDetectionService.markAsDuplicate(transaction);
            meterRegistry.counter("transactions.duplicate").increment();
            return ProcessingOutcome.DUPLICATE;
        }

        if (!isSequenceReady(transaction)) {
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            meterRegistry.counter("transactions.pending").increment();
            return ProcessingOutcome.PENDING;
        }

        transaction.setStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        try {
            applyBalanceUpdate(transaction);
            transaction.setStatus(TransactionStatus.PROCESSED);
            transaction.setProcessedAt(Instant.now());
            transaction.setErrorMessage(null);
            transactionRepository.save(transaction);
            idempotencyService.recordProcessed(transaction);
            meterRegistry.counter("transactions.processed").increment();
            activateNextPending(transaction.getAccountId());
            return ProcessingOutcome.PROCESSED;
        } catch (RetryableException ex) {
            retryService.handleRetryableFailure(transaction, ex);
            return ProcessingOutcome.RETRY_PENDING;
        } catch (Exception ex) {
            retryService.markPermanentFailure(transaction, ex.getMessage());
            return ProcessingOutcome.FAILED;
        }
    }

    private void applyBalanceUpdate(Transaction transaction) {
        if (ProcessingSimulator.isTransientFailureAmount(transaction.getAmount())
                && transaction.getRetryCount() < TransactionConstants.MAX_RETRY_ATTEMPTS) {
            throw new RetryableException("Simulated transient failure for amount " + transaction.getAmount());
        }

        Account account = accountRepository.findById(transaction.getAccountId())
                .orElseGet(() -> accountRepository.save(Account.builder()
                        .accountId(transaction.getAccountId())
                        .balance(BigDecimal.ZERO)
                        .build()));

        if (ProcessingSimulator.isInsufficientFunds(account.getBalance(), transaction.getType(), transaction.getAmount())) {
            throw new IllegalStateException("Insufficient funds for debit operation");
        }

        BigDecimal newBalance = TransactionConstants.TYPE_CREDIT.equals(transaction.getType())
                ? account.getBalance().add(transaction.getAmount())
                : account.getBalance().subtract(transaction.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    private boolean isSequenceReady(Transaction transaction) {
        if (transaction.getSequenceNumber() == 1) {
            return true;
        }
        return transactionRepository.isSequenceProcessed(
                transaction.getAccountId(),
                transaction.getSequenceNumber() - 1);
    }

    private void activateNextPending(String accountId) {
        List<Transaction> pending = transactionRepository
                .findByAccountIdAndStatusOrderBySequenceNumberAsc(accountId, TransactionStatus.PENDING);
        for (Transaction txn : pending) {
            if (isSequenceReady(txn)) {
                processSingle(txn);
            }
        }
    }

    private int processPendingChains() {
        List<Transaction> pending = transactionRepository.findByStatus(TransactionStatus.PENDING);
        int count = 0;
        List<Transaction> snapshot = new ArrayList<>(pending);
        for (Transaction txn : snapshot) {
            if (txn.getStatus() == TransactionStatus.PENDING && isSequenceReady(txn)) {
                if (processSingle(txn) == ProcessingOutcome.PROCESSED) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Outcome of processing a single transaction, used to tally results in batch runs.
     */
    public enum ProcessingOutcome {
        PROCESSED, FAILED, PENDING, DUPLICATE, RETRY_PENDING, SKIPPED
    }
}
