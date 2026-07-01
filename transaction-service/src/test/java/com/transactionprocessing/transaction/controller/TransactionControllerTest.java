package com.transactionprocessing.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactionprocessing.transaction.dto.TransactionRequest;
import com.transactionprocessing.transaction.dto.TransactionResponse;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import com.transactionprocessing.transaction.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests verifying transaction REST endpoint behavior, status codes, and response structure.
 */
@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void submitReturnsAccepted() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .transactionId("TXN-1")
                .requestId("REQ-1")
                .sequenceNumber(1)
                .accountId("ACC-1001")
                .amount(new BigDecimal("10.00"))
                .type("CREDIT")
                .build();

        when(transactionService.submit(any())).thenReturn(TransactionResponse.builder()
                .id(1L)
                .transactionId("TXN-1")
                .requestId("REQ-1")
                .status(TransactionStatus.RECEIVED)
                .build());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.transactionId").value("TXN-1"));
    }

    @Test
    void getByIdReturnsTransaction() throws Exception {
        when(transactionService.getById(1L)).thenReturn(TransactionResponse.builder()
                .id(1L)
                .transactionId("TXN-1")
                .status(TransactionStatus.PROCESSED)
                .build());

        mockMvc.perform(get("/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSED"));
    }

    @Test
    void validationFailsForInvalidRequest() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .transactionId("")
                .requestId("REQ-1")
                .sequenceNumber(0)
                .accountId("BAD")
                .amount(new BigDecimal("-1"))
                .type("INVALID")
                .build();

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
