package shop.shop.integration.RabbitMQ.DTO;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.PaymentMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderMailProducer {
    private String email;
    private String shippingName;
    private String orderCode;
    private String shippingPhone;
    private PaymentMethod paymentMethod;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private List<OrderItemMailProducer> items;
}