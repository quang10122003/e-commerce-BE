package shop.shop.integration.redis.service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.cache.CacheKeys;
import shop.shop.common.dto.response.ApiResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// Redis cache helper cho catalog read-path và invalidation sau commit cho product/category/admin product list.
public class CatalogCacheService {
    RedisTemplate<String, Object> redisTemplate;
    ObjectMapper objectMapper;

    // Lấy dữ liệu cache thô theo key từ Redis.
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Lấy dữ liệu cache dạng payload và chuyển về đúng DTO cần dùng.
    public <T> T getPayload(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }

        return convertValue(value, type);
    }


    // Lấy dữ liệu cache dạng payload generic như List/Map/Page response và chuyển về đúng kiểu.
    public <T> T getPayload(String key, TypeReference<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }

        return convertValue(value, type);
    }

    // Chuyển dữ liệu Redis trả về thành đúng DTO cần dùng.
    private <T> T convertValue(Object value, Class<T> type) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        Object payload = unwrapApiResponsePayload(value);
        if (payload == null) {
            return null;
        }

        return objectMapper.convertValue(payload, type);
    }

    // Chuyển dữ liệu Redis trả về thành đúng DTO generic cần dùng.
    private <T> T convertValue(Object value, TypeReference<T> type) {
        Object payload = unwrapApiResponsePayload(value);
        if (payload == null) {
            return null;
        }

        return objectMapper.convertValue(payload, type);
    }

    // Tách data từ cache cũ từng lưu nguyên ApiResponse để tránh lỗi cast LinkedHashMap.
    private Object unwrapApiResponsePayload(Object value) {
        if (value instanceof ApiResponse<?> response) {
            return response.data();
        }

        if (value instanceof Map<?, ?> map && map.containsKey("data") && map.containsKey("success")) {
            return map.get("data");
        }

        return value;
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

    // Xóa cache danh sách product admin sau khi transaction commit.
    public void registerAdminProductListCacheDeleteAfterCommit() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            DelByPattern("admin:product:list:*");
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CatalogCacheService.this.DelByPattern("admin:product:list:*");
            }
        });
    }

    // Xóa cache product detail và các danh sách product liên quan sau khi transaction commit.
    public void registerProductCacheDeleteAfterCommit(Long productId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            CatalogCacheService.this.Del(CacheKeys.productDetail(productId));
            CatalogCacheService.this.DelByPattern("catalog:product:list:*");
            CatalogCacheService.this.DelByPattern("admin:product:list:*");
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() {
                CatalogCacheService.this.Del(CacheKeys.productDetail(productId));
                CatalogCacheService.this.DelByPattern("catalog:product:list:*");
                CatalogCacheService.this.DelByPattern("admin:product:list:*");
            }
        });
    }

    // Xóa cache danh mục và các danh sách product liên quan sau khi transaction category commit.
    public void registerCategoryCacheDeleteAfterCommit() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            CatalogCacheService.this.Del(CacheKeys.categoriesAll());
            CatalogCacheService.this.DelByPattern("catalog:product:list:*");
            CatalogCacheService.this.DelByPattern("admin:product:list:*");
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CatalogCacheService.this.Del(CacheKeys.categoriesAll());
                CatalogCacheService.this.DelByPattern("catalog:product:list:*");
                CatalogCacheService.this.DelByPattern("admin:product:list:*");
            }
        });
    }

}
