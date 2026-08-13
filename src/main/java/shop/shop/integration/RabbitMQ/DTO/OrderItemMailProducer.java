package shop.shop.integration.RabbitMQ.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.integration.RabbitMQ.DTO.interfaces.DomainEvent;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemMailProducer implements DomainEvent {
    private Long productId;
    private String productName;
    private String categoryName;
    private BigDecimal price;
    private Integer quantity;
    private String thumbnail;
    @Override
    public String routingKey() {
        return "email.order-create";
       
    }
}