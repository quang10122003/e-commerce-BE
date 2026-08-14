/**
 * Interface định nghĩa các thao tác cơ bản để quản lý dữ liệu cache

 */
package shop.shop.integration.redis.service.interfaces;

import java.time.Duration;

import tools.jackson.core.type.TypeReference;

public interface ICacheService {
    // Lấy dữ liệu cache thô theo key từ Redis.
    Object get(String key);

    // Lấy payload và deserialize sang kiểu class cụ thể, ví dụ Order, User,
    // Product.
    <T> T getPayload(String key, Class<T> type);

    // Lấy payload và deserialize sang kiểu generic, ví dụ List<Order>, Map<String,
    // Order>.
    <T> T getPayload(String key, TypeReference<T> type);

    // set cache với thời hạn ttl
    void set(String key, Object value, Duration ttl);

    // xóa cache theo key
    void delete(String key);
// xóa nhiều key trong Redis dựa trên một pattern
    void deleteByPattern(String pattern);
}
