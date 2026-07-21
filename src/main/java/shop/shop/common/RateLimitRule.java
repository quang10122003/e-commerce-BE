package shop.shop.common;

import org.springframework.http.HttpMethod;
import java.time.Duration;

public record RateLimitRule(
        String path, // uri api để check limit
        HttpMethod method, // method api
        long limit, // giới hạn chạm đến limit
        Duration ttl, // thời gian limit
        String keyPrefix) {} 
