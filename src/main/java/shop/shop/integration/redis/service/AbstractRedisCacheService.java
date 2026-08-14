/**
 * Abstract base class cung cấp các thao tác quản lý cache dùng chung
 * trên Redis, bao gồm đọc, ghi, chuyển đổi dữ liệu và xóa cache.
 */
package shop.shop.integration.redis.service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;

import shop.shop.common.dto.response.ApiResponse;
import shop.shop.integration.redis.service.interfaces.ICacheService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public abstract class AbstractRedisCacheService implements ICacheService {
    protected final RedisTemplate<String, Object> redisTemplate;
    protected final ObjectMapper objectMapper;

    protected AbstractRedisCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public <T> T getPayload(String key, Class<T> type) {
        Object value = get(key);
        return value == null ? null : convertValue(value, type);
    }

    @Override
    public <T> T getPayload(String key, TypeReference<T> type) {
        Object value = get(key);
        return value == null ? null : convertValue(value, type);
    }

    // convet dữ liệu cache thô theo từ Redis về 1 obj cụ thế 
    private <T> T convertValue(Object value, Class<T> type) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        Object payload = unwrapApiResponsePayload(value);
        return payload == null ? null : objectMapper.convertValue(payload, type);
    }

    // convet dữ liệu cache thô theo từ Redis về 1 List,v.v obj cụ thể
    private <T> T convertValue(Object value, TypeReference<T> type) {
        Object payload = unwrapApiResponsePayload(value);
        return payload == null ? null : objectMapper.convertValue(payload, type);
    }

    // Tách data từ cache cũ từng lưu nguyên ApiResponse để tránh lỗi cast
    // LinkedHashMap.
    private Object unwrapApiResponsePayload(Object value) {
        if (value instanceof ApiResponse<?> response) {
            return response.data();
        }
        if (value instanceof Map<?, ?> map && map.containsKey("data") && map.containsKey("success")) {
            return map.get("data");
        }
        return value;
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys);
    }
}
