package shop.shop.integration.RabbitMQ.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.RabbitMQ.service.interfaces.WebSocketSender;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@Component
public class StompWebSocketproducer implements  WebSocketSender {
    RabbitTemplate rabbitTemplate;


    @Override
    @Retryable(retryFor = AmqpException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public <T> void send(String exchange, String routingKey, T mess) {
         rabbitTemplate.convertAndSend(exchange, routingKey, mess, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }
}
