package com.transactionprocessing.common.constants;

/**
 * HTTP header names used for distributed tracing and request correlation across services.
 */
public final class HeaderConstants {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String TRACE_ID = "X-Trace-Id";
    public static final String REQUEST_ID = "X-Request-Id";

    private HeaderConstants() {
    }
}
