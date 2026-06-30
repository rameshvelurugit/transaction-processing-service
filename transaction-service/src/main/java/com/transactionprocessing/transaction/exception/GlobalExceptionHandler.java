package com.transactionprocessing.transaction.exception;

import com.transactionprocessing.common.exception.BaseException;
import com.transactionprocessing.common.exception.DuplicateTransactionException;
import com.transactionprocessing.common.exception.ResourceNotFoundException;
import com.transactionprocessing.common.logging.MdcContext;
import com.transactionprocessing.common.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        log.warn("Validation failure on path={} errors={}", request.getRequestURI(), fieldErrors.size());
        return ResponseEntity.badRequest().body(baseBuilder(HttpStatus.BAD_REQUEST, request)
                .errorCode("VALIDATION_ERROR")
                .message("Request validation failed")
                .fieldErrors(fieldErrors)
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest request) {
        log.warn("Validation failure on path={}", request.getRequestURI());
        return ResponseEntity.badRequest().body(baseBuilder(HttpStatus.BAD_REQUEST, request)
                .errorCode("VALIDATION_ERROR")
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(baseBuilder(HttpStatus.NOT_FOUND, request)
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateTransactionException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(baseBuilder(HttpStatus.CONFLICT, request)
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest request) {
        log.error("Business exception on path={} code={}", request.getRequestURI(), ex.getErrorCode(), ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                baseBuilder(HttpStatus.UNPROCESSABLE_ENTITY, request)
                        .errorCode(ex.getErrorCode())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception on path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                baseBuilder(HttpStatus.INTERNAL_SERVER_ERROR, request)
                        .errorCode("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .build());
    }

    private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }

    private ErrorResponse.ErrorResponseBuilder baseBuilder(HttpStatus status, HttpServletRequest request) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .path(request.getRequestURI())
                .correlationId(MdcContext.getOrGenerateCorrelationId());
    }
}
