package com.transactionprocessing.common.constants;

/**
 * MDC (Mapped Diagnostic Context) key names used for structured, correlatable log output.
 */
public final class LogFields {

    public static final String APPLICATION_NAME = "applicationName";
    public static final String SERVICE_NAME = "serviceName";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String REQUEST_ID = "requestId";

    private LogFields() {
    }
}
