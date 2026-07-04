package shop.shop.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckoutCartItemRequest(
        @NotNull(message = "ProductId không được để trống")
        Long productId,
        @NotNull(message = "Quantity không được để trống")
        @Positive(message = "Quantity phải lớn hơn 0")
        Integer quantity) {
}
