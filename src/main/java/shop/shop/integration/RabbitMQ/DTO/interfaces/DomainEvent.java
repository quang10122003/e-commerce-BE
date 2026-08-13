package shop.shop.integration.RabbitMQ.DTO.interfaces;

public interface DomainEvent {
    String routingKey();
}
