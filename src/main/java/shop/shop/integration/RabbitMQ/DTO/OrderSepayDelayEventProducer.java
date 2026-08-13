package shop.shop.integration.RabbitMQ.DTO;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import shop.shop.integration.RabbitMQ.DTO.interfaces.DomainEvent;
@AllArgsConstructor
@NoArgsConstructor
public class OrderSepayDelayEventProducer implements DomainEvent  {
    String orderId;

    @Override
    public String routingKey() {
        return "order.sepay.delay.routingkey";
    }

}
