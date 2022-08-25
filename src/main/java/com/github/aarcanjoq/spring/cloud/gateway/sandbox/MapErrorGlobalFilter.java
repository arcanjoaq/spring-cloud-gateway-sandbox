package com.github.aarcanjoq.spring.cloud.gateway.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MapErrorGlobalFilter implements GlobalFilter, Ordered {

    public static final String ORIGINAL_STATUS_CODE_ATTRIBUTE = "originalStatusCode";

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> exchange.getResponse().beforeCommit(() -> {
                    final HttpStatus originalStatusCode = exchange.getResponse().getStatusCode();
                    exchange.getAttributes().put(ORIGINAL_STATUS_CODE_ATTRIBUTE, originalStatusCode);
                    if (originalStatusCode == HttpStatus.UNAUTHORIZED || originalStatusCode == HttpStatus.FORBIDDEN) {
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    return Mono.empty();
                })));
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}