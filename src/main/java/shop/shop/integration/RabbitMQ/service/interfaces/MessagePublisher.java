package shop.shop.integration.RabbitMQ.service.interfaces;

public interface MessagePublisher {
    <T> void send(String exchange,String routingKey,T mess);
}
