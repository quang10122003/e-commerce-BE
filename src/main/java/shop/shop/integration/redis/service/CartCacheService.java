package shop.shop.integration.redis.service;

import java.time.Duration;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.cache.CacheKeys;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// Redis cache helper cho cart state theo user và invalidation toàn bộ cart cache khi product thay đổi.
public class CartCacheService {
    RedisTemplate<String, Object> redisTemplate;

    // Lấy dữ liệu cache theo key từ Redis và ép kiểu trả về.
    public <T> T get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }

        return (T) value;
    }

    // Lưu dữ liệu cache vào Redis theo key và thời gian hết hạn.
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    // Xóa một key cache cụ thể trong Redis.
    public void Del(String key) {
        redisTemplate.delete(key);
    }

    // Xóa nhiều key cache theo pattern.
    public void DelByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        redisTemplate.delete(keys);
    }

    // Ghi lại cart state mới sau khi transaction commit thành công.
    public void registerCartCacheUpdateAfterCommit(Long userId, Object cartResponse) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            CartCacheService.this.set(CacheKeys.cartByUser(userId), cartResponse, Duration.ofDays(7));
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CartCacheService.this.set(CacheKeys.cartByUser(userId), cartResponse, Duration.ofDays(7));
            }
        });
    }

    // Xóa toàn bộ cart cache theo pattern `cart:user:*` khi product thay đổi làm dữ liệu cart snapshot bị stale.
    public void registerCartCacheDeleteAfterCommit() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            DelByPattern("cart:user:*");
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CartCacheService.this.DelByPattern("cart:user:*");
            }
        });
    }
}
