package com.github.aarcanjoq.spring.cloud.gateway.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.aarcanjoq.spring.cloud.gateway.sandbox.MapErrorGlobalFilter.ORIGINAL_STATUS_CODE_ATTRIBUTE;

@Slf4j
@Component
public class LogGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
        final URI originalUri = getOriginalUri(exchange);
        final Map<String, String> originalRequestHeaders = toMap(exchange.getRequest().getHeaders());
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> log(originalUri, originalRequestHeaders, exchange)));
    }

    private void log(final URI originalUri, final Map<String, String> originalHeaders, final ServerWebExchange exchange) {
        final Route route = (Route) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        final ServerHttpRequest request = exchange.getRequest();
        final HttpMethod method = request.getMethod();

        final URI gatewayUri = getGatewayUri(exchange);
        final Map<String, String> gatewayHeaders = toMap(request.getHeaders());

        final ServerHttpResponse response = exchange.getResponse();
        final Map<String, String> responseHeaders = toMap(exchange.getResponse().getHeaders());

        final HttpStatus o = (HttpStatus) exchange.getAttributes().get(ORIGINAL_STATUS_CODE_ATTRIBUTE);

        log.info("{} curl -vv -X {} {} {} -> curl -vv -X {} {} {} -> response headers: {}, status code: {}, original status code: {}",
                route.getId(),
                method, toRequestHeaders(originalHeaders), originalUri,
                method, toRequestHeaders(gatewayHeaders), gatewayUri,
                toResponseHeaders(responseHeaders), response.getRawStatusCode(),
                o != null ? o.value() : response.getRawStatusCode());
    }

    private URI getGatewayUri(ServerWebExchange exchange) {
        return exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
    }

    private URI getOriginalUri(final ServerWebExchange exchange) {
        return exchange.getRequest().getURI();
    }

    private Map<String, String> toMap(final HttpHeaders headers) {
        return headers.entrySet()
                .stream().map(entry -> new AbstractMap.SimpleEntry<String, String>(entry.getKey(), String.join(",", entry.getValue())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private String toRequestHeaders(final Map<String, String> headers) {
        return headers.entrySet().stream()
                .map(entry -> String.format("-H '%s: %s'", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" "));
    }

    private String toResponseHeaders(final Map<String, String> headers) {
        return headers.entrySet().stream()
                .map(entry -> String.format("'%s: %s'", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" "));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}