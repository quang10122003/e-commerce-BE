package shop.shop.integration.wedsocket.service.interfaces;

public interface IWebSocketSender {

    void send(String destination, Object message);
}