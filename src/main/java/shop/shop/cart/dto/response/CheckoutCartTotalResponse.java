package shop.shop.cart.dto.response;

import java.math.BigDecimal;

public record CheckoutCartTotalResponse(
        BigDecimal totalAmount) {
}
