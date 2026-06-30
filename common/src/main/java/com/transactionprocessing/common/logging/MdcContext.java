package com.transactionprocessing.common.logging;

import com.transactionprocessing.common.constants.LogFields;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MdcContext {

    private MdcContext() {
    }

    public static void set(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    public static void setTransactionId(String transactionId) {
        set(LogFields.TRANSACTION_ID, transactionId);
    }

    public static void setRequestId(String requestId) {
        set(LogFields.REQUEST_ID, requestId);
    }

    public static void setCorrelationId(String correlationId) {
        set(LogFields.CORRELATION_ID, correlationId);
    }

    public static void setTraceId(String traceId) {
        set(LogFields.TRACE_ID, traceId);
    }

    public static void setServiceName(String serviceName) {
        set(LogFields.SERVICE_NAME, serviceName);
    }

    public static void setApplicationName(String applicationName) {
        set(LogFields.APPLICATION_NAME, applicationName);
    }

    public static Optional<String> get(String key) {
        return Optional.ofNullable(MDC.get(key));
    }

    public static String getOrGenerateCorrelationId() {
        return get(LogFields.CORRELATION_ID).orElseGet(() -> {
            String id = UUID.randomUUID().toString();
            setCorrelationId(id);
            return id;
        });
    }

    public static void clear() {
        MDC.clear();
    }

    public static void setContext(Map<String, String> context) {
        context.forEach(MdcContext::set);
    }
}
