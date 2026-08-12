package shop.shop.integration.Resend.DTO.resquest;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.OrderItem;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderMailDTO {
    private String email;
    private String shippingName;
    private String orderCode;
    private String shippingPhone;
    private PaymentMethod paymentMethod;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
}
