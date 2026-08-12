package shop.shop.integration.RabbitMQ;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import shop.shop.integration.RabbitMQ.DTO.CreateOrderMailProducer;
import shop.shop.integration.RabbitMQ.DTO.ResetPasswordProducer;
import shop.shop.integration.RabbitMQ.service.interfaces.WebSocketSender;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueService {
    @NonFinal
    @Value("${app.rabbitMq.exchange}")
    String exchange;
    @NonFinal
    @Value("${app.rabbitMq.orderSepayDelayRoutingKey}")
    String orderSepayDelayRoutingKey;
    
    WebSocketSender webSocketSender;

    String resetPasswordRouting = "email.reset-password";

    String CreateOrderRouting = "email.order-create";



    public void sendOrderCreatedPayment(String mess) {
        webSocketSender.send(exchange, orderSepayDelayRoutingKey, mess);
    }

    public void sendResetPasswordMailEvent(ResetPasswordProducer mess){
        webSocketSender.send(exchange, resetPasswordRouting, mess);
    }

    public void sendCreateOrderMailEvent(CreateOrderMailProducer mess) {
        webSocketSender.send(exchange, CreateOrderRouting, mess);
    }

}
