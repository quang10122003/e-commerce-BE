package shop.shop.category.dto.response;

import java.time.LocalDateTime;

public record CategorySummaryResponse(
        Long id,
        String name,
        String image,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
