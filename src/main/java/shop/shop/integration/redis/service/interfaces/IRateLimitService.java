// interface định nghĩa class RateLimitService
package shop.shop.integration.redis.service.interfaces;

import java.time.Duration;

public interface IRateLimitService {
    public boolean isAllowed(String key, long limit, Duration ttl);
}
