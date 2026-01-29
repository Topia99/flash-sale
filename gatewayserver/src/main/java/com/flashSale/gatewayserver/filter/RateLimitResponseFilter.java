package com.flashSale.gatewayserver.filter;


import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RateLimitResponseFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.defer(() -> {
            var response = exchange.getResponse();
            if (response.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) {
                return Mono.empty();
            }

            // 若已写入 body，就别重复写
            if (response.isCommitted()) {
                return Mono.empty();
            }

            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = """
          {"code":"RATE_LIMITED","message":"Too many requests. Please retry later."}
          """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }));
    }

}
