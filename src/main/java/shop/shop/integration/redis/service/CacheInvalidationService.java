package shop.shop.integration.redis.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import shop.shop.common.cache.CacheKeys;
import shop.shop.integration.redis.service.interfaces.ICacheService;

@Service
@RequiredArgsConstructor
public class CacheInvalidationService {
    private static final Duration CART_CACHE_TTL = Duration.ofDays(7);

    private final ICacheService cacheService;

    // Xóa nhiều pattern cache sau khi transaction commit.
    public void afterCommitPatterns(String... patterns) {
        TransactionUtils.runAfterCommit(() -> {
            for (String pattern : patterns) {
                cacheService.deleteByPattern(pattern);
            }
        });
    }

    // Xóa nhiều cache key cụ thể sau khi transaction commit.
    public void afterCommitKeys(String... keys) {
        TransactionUtils.runAfterCommit(() -> {
            for (String key : keys) {
                cacheService.delete(key);
            }
        });
    }

    // Ghi lại cart state mới sau khi transaction commit thành công.
    public void cartChanged(Long userId, Object cartResponse) {
        TransactionUtils.runAfterCommit(() -> cacheService.set(CacheKeys.cartByUser(userId), cartResponse, CART_CACHE_TTL));
    }

    // Xóa cache danh mục và các danh sách sản phẩm liên quan.
    public void categoryChanged() {
        afterCommitKeys(CacheKeys.categoriesAll());
        afterCommitPatterns(
                CacheKeys.productListAllPattern(),
                CacheKeys.adminProductListAllPattern());
    }

    // Xóa cache sản phẩm, danh sách catalog/admin và cart snapshot.
    public void productChanged(Long productId) {
        afterCommitKeys(CacheKeys.productDetail(productId));
        afterCommitPatterns(
                CacheKeys.productListAllPattern(),
                CacheKeys.adminProductListAllPattern(),
                CacheKeys.cartAllPattern());
    }

    // Xóa cache nhiều sản phẩm và các danh sách catalog/admin liên quan.
    public void productsChanged(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        afterCommitKeys(productIds.stream()
                .map(CacheKeys::productDetail)
                .toArray(String[]::new));
        afterCommitPatterns(
                CacheKeys.productListAllPattern(),
                CacheKeys.adminProductListAllPattern());
    }
}
