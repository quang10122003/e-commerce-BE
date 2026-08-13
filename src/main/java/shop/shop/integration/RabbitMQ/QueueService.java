package shop.shop.integration.RabbitMQ;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import shop.shop.integration.RabbitMQ.DTO.interfaces.DomainEvent;
import shop.shop.integration.RabbitMQ.service.interfaces.MessagePublisher;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueService {
    @NonFinal
    @Value("${app.rabbitMq.exchange}")
    String exchange;

    MessagePublisher messagePublisher;

    //push event 
    public void publish(DomainEvent event) {
        messagePublisher.send(exchange, event.routingKey(), event);
    }
}