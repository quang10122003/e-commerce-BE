package shop.shop.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminNewOrderOverview {
    Long id; // Id của order.
    LocalDateTime createdAt;
    String shippingName;
    BigDecimal totalAmount;
    PaymentMethod methodPayment; // Của bảng payment trong DB.
    OrderStatus statusOrder ;
}
