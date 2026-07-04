package shop.shop.product.dto.response;

import shop.shop.common.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductSummaryResponse(
        Long id,
        String name,
        Integer purchases, 
        String description,
        BigDecimal price,
        Integer stock,
        ProductStatus status,
        Long categoryId,
        String thumbnail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
