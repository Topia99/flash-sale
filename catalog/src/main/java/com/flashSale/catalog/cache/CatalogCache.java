package com.flashSale.catalog.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CatalogCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final Duration BASE_TTL = Duration.ofSeconds(60);

    public String eventDetailKey(Long eventId) {
        return "event:" + eventId + ":detail";
    }

    public String ticketKey(Long ticketId) {
        return "ticket:" + ticketId;
    }

    public Duration ttlWithJitter() {
        // TTL + 0~10% random
        long base = BASE_TTL.toSeconds();
        long jitter = (long) (base * Math.random() * 0.1);
        return Duration.ofSeconds(base + jitter);
    }

    public <T> T getJson(String key, Class<T> clazz) {
        String json = redis.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            // 反序列化失败：删掉脏缓存，回源
            redis.delete(key);
            return null;
        }
    }

    public void setJson(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            // 写缓存失败不影响主流程
        }
    }

    public void evict(String key) {
        redis.delete(key);
    }

    public void evictEventDetail(Long eventId) {
        redis.delete(eventDetailKey(eventId));
    }

    public void evictTicket(Long ticketId) {
        redis.delete(ticketKey(ticketId));
    }
}
