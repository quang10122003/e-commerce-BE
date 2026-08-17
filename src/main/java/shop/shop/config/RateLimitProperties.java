// Nguồn duy nhất cho rate-limit rules, đọc từ application.yml
package shop.shop.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(List<Rule> rules) {
// method để dạng String vì YAML/HttpMethod không tự bind trực tiếp -
    // convert sang HttpMethod thật ở RateLimitRuleRegistry.
    public record Rule(String path, String method, long limit, Duration ttl, String keyPrefix) {
    }
}