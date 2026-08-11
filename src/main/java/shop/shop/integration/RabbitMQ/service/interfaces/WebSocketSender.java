package shop.shop.integration.RabbitMQ.service.interfaces;

public interface WebSocketSender {
    <T> void send(String exchange,String routingKey,T mess);
}
