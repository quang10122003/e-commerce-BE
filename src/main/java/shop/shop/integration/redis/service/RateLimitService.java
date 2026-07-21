package shop.shop.integration.redis.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// Redis helper dùng để đếm request cho rate-limit interceptor.
public class RateLimitService {
    StringRedisTemplate redisStringTemplate;
    
    // Kiểm tra một key còn trong giới hạn cho phép trong khoảng TTL đã cấu hình.
    public boolean isAllowed(String key, long limit, Duration ttl) {
        Long count = redisStringTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisStringTemplate.expire(key, ttl);
        }

        return count != null && count <= limit;
    }

}
