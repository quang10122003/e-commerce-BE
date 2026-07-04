package shop.shop.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;

// Dữ liệu xem trước checkout gồm sản phẩm đã chọn và tổng tiền phải thanh toán.
public record CheckoutCartResponse(
        List<CartItemResponse> items,
        Integer totalQuantity,
        BigDecimal totalAmount) {
}
