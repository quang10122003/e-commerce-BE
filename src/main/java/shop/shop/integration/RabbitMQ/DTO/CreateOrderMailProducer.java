package shop.shop.integration.RabbitMQ.DTO;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.PaymentMethod;
import shop.shop.integration.RabbitMQ.DTO.interfaces.DomainEvent;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderMailProducer  implements DomainEvent {
    private String email;
    private String shippingName;
    private String orderCode;
    private String shippingPhone;
    private PaymentMethod paymentMethod;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private List<OrderItemMailProducer> items;

    @Override
    public String routingKey() {
       
        return "email.order-create";
    }
}