package com.transactionprocessing.transaction.service;

import com.transactionprocessing.common.exception.ResourceNotFoundException;
import com.transactionprocessing.common.logging.MdcContext;
import com.transactionprocessing.transaction.dto.ProcessingRunResponse;
import com.transactionprocessing.transaction.dto.ProcessingSummaryResponse;
import com.transactionprocessing.transaction.dto.RetryResponse;
import com.transactionprocessing.transaction.dto.TransactionRequest;
import com.transactionprocessing.transaction.dto.TransactionResponse;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.mapper.TransactionMapper;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final DuplicateDetectionService duplicateDetectionService;
    private final TransactionProcessingService transactionProcessingService;
    private final ProcessingSummaryService processingSummaryService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public TransactionResponse submit(TransactionRequest request) {
        MdcContext.setTransactionId(request.getTransactionId());
        MdcContext.setRequestId(request.getRequestId());

        if (duplicateDetectionService.isDuplicate(request.getTransactionId(), request.getRequestId())) {
            Transaction existing = duplicateDetectionService
                    .findExistingTransaction(request.getTransactionId(), request.getRequestId())
                    .map(duplicateDetectionService::markAsDuplicate)
                    .orElseGet(() -> {
                        Transaction duplicate = buildTransaction(request);
                        duplicate.setStatus(TransactionStatus.DUPLICATE);
                        return transactionRepository.save(duplicate);
                    });
            meterRegistry.counter("transactions.ingested", "status", "duplicate").increment();
            return transactionMapper.toResponse(existing);
        }

        return duplicateDetectionService.findExistingTransaction(request.getTransactionId(), request.getRequestId())
                .map(existing -> {
                    meterRegistry.counter("transactions.ingested", "status", "existing").increment();
                    return transactionMapper.toResponse(existing);
                })
                .orElseGet(() -> {
                    Transaction saved = transactionRepository.save(buildTransaction(request));
                    meterRegistry.counter("transactions.ingested", "status", "received").increment();
                    log.info("Transaction ingested id={} status=RECEIVED", saved.getId());
                    return transactionMapper.toResponse(saved);
                });
    }

    public TransactionResponse getById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        return transactionMapper.toResponse(transaction);
    }

    public List<TransactionResponse> getAll() {
        return transactionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public ProcessingRunResponse triggerProcessing() {
        int processed = transactionProcessingService.processPendingTransactions();
        return ProcessingRunResponse.builder()
                .processedCount(processed)
                .message("Background processing completed")
                .build();
    }

    @Transactional
    public RetryResponse retryFailed() {
        List<Transaction> retryCandidates = transactionRepository.findByStatus(TransactionStatus.FAILED);
        retryCandidates.addAll(transactionRepository.findByStatus(TransactionStatus.RETRY_PENDING));

        int retried = 0;
        int success = 0;
        int stillFailed = 0;

        for (Transaction transaction : retryCandidates) {
            if (transaction.getStatus() == TransactionStatus.FAILED
                    && transaction.getRetryCount() >= 3) {
                stillFailed++;
                continue;
            }
            transaction.setStatus(TransactionStatus.RETRY_PENDING);
            transactionRepository.save(transaction);
            retried++;
            TransactionProcessingService.ProcessingOutcome outcome =
                    transactionProcessingService.processSingle(transaction);
            if (outcome == TransactionProcessingService.ProcessingOutcome.PROCESSED) {
                success++;
            } else if (outcome == TransactionProcessingService.ProcessingOutcome.FAILED) {
                stillFailed++;
            }
        }

        log.info("Retry batch completed: retried={}, success={}, stillFailed={}", retried, success, stillFailed);
        return RetryResponse.builder()
                .retriedCount(retried)
                .successCount(success)
                .stillFailedCount(stillFailed)
                .message("Retry processing completed")
                .build();
    }

    public ProcessingSummaryResponse getSummary() {
        return processingSummaryService.getSummary();
    }

    private Transaction buildTransaction(TransactionRequest request) {
        return Transaction.builder()
                .transactionId(request.getTransactionId())
                .requestId(request.getRequestId())
                .sequenceNumber(request.getSequenceNumber())
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .type(request.getType())
                .status(TransactionStatus.RECEIVED)
                .retryCount(0)
                .build();
    }
}
