package shop.shop.integration.RabbitMQ;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import shop.shop.integration.RabbitMQ.service.interfaces.WebSocketSender;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketService {
    @NonFinal
    @Value("${app.rabbitMq.exchange}")
    String exchange;
    @NonFinal
    @Value("${app.rabbitMq.orderSepayDelayRoutingKey}")
    String orderSepayDelayRoutingKey;
    
    WebSocketSender webSocketSender;



    public void sendOrderCreatedPayment(String mess) {
        webSocketSender.send(exchange, orderSepayDelayRoutingKey, mess);
    }
}
