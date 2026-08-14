// class để tạo bean cho ICacheService
package shop.shop.integration.redis.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

@Service
public class RedisCacheAdapter extends AbstractRedisCacheService {

    public RedisCacheAdapter(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate, objectMapper);
    }
}
