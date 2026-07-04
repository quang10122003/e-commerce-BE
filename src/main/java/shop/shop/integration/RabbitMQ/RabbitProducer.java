package shop.shop.integration.RabbitMQ;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RabbitProducer {
    @Value("${app.rabbitMq.exchange}")
    String exchange;

    @Value("${app.rabbitMq.orderSepayDelayRoutingKey}")
    String orderSepayDelayRoutingKey;

    final RabbitTemplate rabbitTemplate;

    @Retryable(retryFor = AmqpException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendOrderSepayCheckQueue(String mess) {
        rabbitTemplate.convertAndSend(exchange, orderSepayDelayRoutingKey, mess, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }
}
