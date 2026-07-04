package shop.shop.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckoutCartTotalRequest(
        @NotEmpty(message = "Danh sách items không được để trống")
        @Valid List<CheckoutCartItemRequest> items) {
}
