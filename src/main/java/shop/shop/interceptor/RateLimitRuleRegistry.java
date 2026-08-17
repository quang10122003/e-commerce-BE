package shop.shop.interceptor;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import shop.shop.common.RateLimitRule;
import shop.shop.config.RateLimitProperties;

// OCP: registry không còn "biết trước" danh sách rule nào tồn tại - toàn bộ
// đến từ RateLimitProperties (bind từ YAML). Registry chỉ làm 2 việc: quy
// đổi cấu hình thô sang domain object 1 lần lúc khởi động, và match request
// vào đúng rule.
@Component
public class RateLimitRuleRegistry {

    private final List<RateLimitRule> rules;

    public RateLimitRuleRegistry(RateLimitProperties properties) {
        this.rules = properties.rules().stream()
                .map(rule -> new RateLimitRule(
                        rule.path(),
                        HttpMethod.valueOf(rule.method()),
                        rule.limit(),
                        rule.ttl(),
                        rule.keyPrefix()))
                .toList();
    }

    // match interceptor vào đúng rule
    public RateLimitRule match(String requestPath, String method) {
        return rules.stream()
                .filter(r -> r.path().equals(requestPath)
                        && r.method().matches(method))
                .findFirst()
                .orElse(null);
    }
}