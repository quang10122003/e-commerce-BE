package shop.shop.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminOrderItemRepone {
    Long id;
    String orderCode;
    Long userId;
    OrderStatus status;
    CancelledBy cancelledBy;
    PaymentMethod paymentMethod;
    String shippingName;
    String shippingPhone;
    String shippingAddress;
    BigDecimal totalAmount;
    LocalDateTime createdAt;
    List<AdminOrderProductItemRepone> items;
}
