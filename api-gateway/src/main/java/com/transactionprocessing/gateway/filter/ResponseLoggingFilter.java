package com.transactionprocessing.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ResponseLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long start = exchange.getAttribute("requestStartTime");
            long duration = start == null ? -1 : System.currentTimeMillis() - start;
            log.info("Gateway request completed method={} path={} status={} durationMs={}",
                    exchange.getRequest().getMethod().name(),
                    exchange.getRequest().getURI().getPath(),
                    exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0,
                    duration);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
