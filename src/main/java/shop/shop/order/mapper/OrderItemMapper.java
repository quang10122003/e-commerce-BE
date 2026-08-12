package shop.shop.order.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import shop.shop.integration.RabbitMQ.DTO.OrderItemMailProducer;
import shop.shop.order.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    
    OrderItemMailProducer toOrderItemMailProducer(OrderItem item);

    List<OrderItemMailProducer> toOrderItemMailProducers(List<OrderItem> items);
}