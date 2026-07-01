package com.transactionprocessing.transaction.logging;

import com.transactionprocessing.common.constants.HeaderConstants;
import com.transactionprocessing.common.constants.LogFields;
import com.transactionprocessing.common.logging.MdcContext;
import com.transactionprocessing.common.util.TraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates MDC context and logs the full request lifecycle for the transaction service.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            populateMdc(request);
            log.info("Request received method={} path={}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("Request completed method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            response.setHeader(HeaderConstants.CORRELATION_ID, MdcContext.getOrGenerateCorrelationId());
            response.setHeader(HeaderConstants.TRACE_ID, MdcContext.get(LogFields.TRACE_ID).orElse(""));
            MdcContext.clear();
        }
    }

    private void populateMdc(HttpServletRequest request) {
        MdcContext.setApplicationName(applicationName);
        MdcContext.setServiceName("transaction-service");
        String correlationId = headerOrGenerate(request, HeaderConstants.CORRELATION_ID);
        String traceId = headerOrGenerate(request, HeaderConstants.TRACE_ID, TraceIdGenerator::generate);
        String requestId = headerOrGenerate(request, HeaderConstants.REQUEST_ID);
        MdcContext.setCorrelationId(correlationId);
        MdcContext.setTraceId(traceId);
        MdcContext.setRequestId(requestId);
    }

    private String headerOrGenerate(HttpServletRequest request, String header) {
        return headerOrGenerate(request, header, () -> UUID.randomUUID().toString());
    }

    private String headerOrGenerate(HttpServletRequest request, String header, java.util.function.Supplier<String> supplier) {
        String value = request.getHeader(header);
        return (value == null || value.isBlank()) ? supplier.get() : value;
    }
}
