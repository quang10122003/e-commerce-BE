package shop.shop.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import shop.shop.common.RateLimitRule;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.integration.redis.service.RateLimitService;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final RateLimitRuleRegistry ruleRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        RateLimitRule rule = ruleRegistry.match(request.getRequestURI(), request.getMethod());

        if (rule == null) {
            return true; // API này không cần rate-limit
        }

        String ip = getClientIp(request);
        String redisKey = rule.keyPrefix() + ":" + ip;

        boolean allowed = rateLimitService.isAllowed(redisKey, rule.limit(), rule.ttl());

        if (!allowed) {
            throw new ApiError(ErrorCode.LIMIT_REQUEST);
        }

        return true;
    }

    // lấy ip của user
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}