package shop.shop.cart.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        String thumbnail,
        BigDecimal unitPrice,
        Integer stock,
        Integer quantity,
        BigDecimal totalPrice) {
}
