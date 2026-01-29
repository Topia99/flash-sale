package com.flashSale.gatewayserver.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // 1) 优先取 X-Forwarded-For（为后续上LB/Nginx铺路）
            String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String ip = xff.split(",")[0].trim();
                return Mono.just(ip);
            }

            // 2) 本地直连时，用 remoteAddress
            var remote = exchange.getRequest().getRemoteAddress();
            if (remote != null && remote.getAddress() != null) {
                return Mono.just(remote.getAddress().getHostAddress());
            }

            // 3) 保底（不建议长期用，但避免NPE）
            return Mono.just("unknown");
        };
    }
}
