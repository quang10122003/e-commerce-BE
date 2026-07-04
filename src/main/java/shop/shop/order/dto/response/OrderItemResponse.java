package shop.shop.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private String categoryName;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private String thumbnail;
}