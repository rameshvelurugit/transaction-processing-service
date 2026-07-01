package com.transactionprocessing.transaction.controller;

import com.transactionprocessing.common.model.ApiResponse;
import com.transactionprocessing.common.util.CorrelationIdHolder;
import com.transactionprocessing.transaction.dto.ProcessingRunResponse;
import com.transactionprocessing.transaction.dto.ProcessingSummaryResponse;
import com.transactionprocessing.transaction.dto.RetryResponse;
import com.transactionprocessing.transaction.dto.TransactionRequest;
import com.transactionprocessing.transaction.dto.TransactionResponse;
import com.transactionprocessing.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing endpoints to submit, query, process, retry, and summarize transactions.
 */
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> submit(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(response, CorrelationIdHolder.current()));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(transactionService.getById(id), CorrelationIdHolder.current());
    }

    @GetMapping
    public ApiResponse<List<TransactionResponse>> getAll() {
        return ApiResponse.of(transactionService.getAll(), CorrelationIdHolder.current());
    }

    @GetMapping("/summary")
    public ApiResponse<ProcessingSummaryResponse> summary() {
        return ApiResponse.of(transactionService.getSummary(), CorrelationIdHolder.current());
    }

    @PostMapping("/process")
    public ApiResponse<ProcessingRunResponse> process() {
        return ApiResponse.of(transactionService.triggerProcessing(), CorrelationIdHolder.current());
    }

    @PostMapping("/retry")
    public ApiResponse<RetryResponse> retry() {
        return ApiResponse.of(transactionService.retryFailed(), CorrelationIdHolder.current());
    }
}
