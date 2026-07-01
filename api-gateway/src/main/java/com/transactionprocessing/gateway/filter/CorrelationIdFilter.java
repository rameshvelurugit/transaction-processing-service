package com.transactionprocessing.gateway.filter;

import com.transactionprocessing.common.constants.HeaderConstants;
import com.transactionprocessing.common.logging.MdcContext;
import com.transactionprocessing.common.util.TraceIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ensures every gateway request carries correlation, trace, and request IDs in headers and MDC for downstream propagation.
 */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = headerOrGenerate(request, HeaderConstants.CORRELATION_ID);
        String traceId = headerOrGenerate(request, HeaderConstants.TRACE_ID, TraceIdGenerator::generate);
        String requestId = headerOrGenerate(request, HeaderConstants.REQUEST_ID);

        MdcContext.setApplicationName(applicationName);
        MdcContext.setServiceName("api-gateway");
        MdcContext.setCorrelationId(correlationId);
        MdcContext.setTraceId(traceId);
        MdcContext.setRequestId(requestId);

        ServerHttpRequest mutated = request.mutate()
                .header(HeaderConstants.CORRELATION_ID, correlationId)
                .header(HeaderConstants.TRACE_ID, traceId)
                .header(HeaderConstants.REQUEST_ID, requestId)
                .build();

        exchange.getResponse().getHeaders().add(HeaderConstants.CORRELATION_ID, correlationId);
        exchange.getResponse().getHeaders().add(HeaderConstants.TRACE_ID, traceId);

        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> MdcContext.clear());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String headerOrGenerate(ServerHttpRequest request, String header) {
        return headerOrGenerate(request, header, () -> UUID.randomUUID().toString());
    }

    private String headerOrGenerate(ServerHttpRequest request, String header, java.util.function.Supplier<String> supplier) {
        String value = request.getHeaders().getFirst(header);
        return (value == null || value.isBlank()) ? supplier.get() : value;
    }
}
