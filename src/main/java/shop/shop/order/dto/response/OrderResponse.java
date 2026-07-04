package shop.shop.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private OrderStatus status;

    // Mã đơn hàng để FE điều hướng sang trang thanh toán.
    private String orderCode;

    private String shippingName;
    private String shippingPhone;
    private String shippingAddress; 

    // Phương thức thanh toán của đơn hàng.
    private PaymentMethod paymentMethod;

    // Thời điểm hết hạn thanh toán, không có thì trả về null.
    private LocalDateTime expiredAt;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;
}
