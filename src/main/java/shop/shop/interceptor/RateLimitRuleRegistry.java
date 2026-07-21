package shop.shop.interceptor;

import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import shop.shop.common.RateLimitRule;

@Component
public class RateLimitRuleRegistry {
     private final List<RateLimitRule> rules = List.of(
        new RateLimitRule("/api/auth/signup", HttpMethod.POST, 5, Duration.ofDays(1), "register")
    );
    // match innterceptor vào đúng rule
    public RateLimitRule match(String requestPath, String method) {
        return rules.stream()
                .filter(r -> r.path().equals(requestPath)
                        && r.method().matches(method))
                .findFirst()
                .orElse(null);
    }
}
