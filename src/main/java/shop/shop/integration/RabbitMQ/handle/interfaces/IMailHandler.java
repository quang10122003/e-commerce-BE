// Interface định nghĩa handler xử lý message theo từng routing keyd
package shop.shop.integration.RabbitMQ.handle.interfaces;

public interface IMailHandler{
    String routingKey();
    void handle(String mess);
}
