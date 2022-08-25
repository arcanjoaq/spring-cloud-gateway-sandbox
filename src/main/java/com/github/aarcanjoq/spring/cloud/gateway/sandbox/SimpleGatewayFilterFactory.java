package com.github.aarcanjoq.spring.cloud.gateway.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SimpleGatewayFilterFactory implements GatewayFilterFactory<SimpleGatewayFilterFactory.Config>, Ordered {

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            final HttpHeaders headers = exchange.getRequest().getHeaders();
            log.info("This is my SimpleGatewayFilter");
            log.info("Headers: {}", headers);

            // passa para o proximo filtro
            return chain.filter(exchange);
        };
    }

    @Override
    public Class<SimpleGatewayFilterFactory.Config> getConfigClass() {
        return SimpleGatewayFilterFactory.Config.class;
    }

    @Override
    public int getOrder() {
        return 1;
    }

    public static class Config {
    }
}
