package shop.shop.integration.RabbitMQ.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemMailProducer {
    private Long productId;
    private String productName;
    private String categoryName;
    private BigDecimal price;
    private Integer quantity;
    private String thumbnail;
}