package shop.shop.integration.RabbitMQ.service.comsumer;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.RabbitMQ.service.MailHandlerRegistry;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@Service
public class MailComsumer {
    MailHandlerRegistry registry;
    @RabbitListener(queues = "mailQueue")

    public void handleMail(
            Message message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY)
            String routingKey
    ) {
        
        String mess = extractBody(message);

        registry.getHandler(routingKey).handle(mess);
    }

    
// decode phần body (dạng byte[]) của message AMQP thành chuỗi JSON dạng text.
    private String extractBody(Message message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }
}
