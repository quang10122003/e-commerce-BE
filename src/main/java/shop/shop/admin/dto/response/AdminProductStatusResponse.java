package shop.shop.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.ProductStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductStatusResponse {
    private Long productId;
    private ProductStatus status;
}
