package shop.shop.integration.RabbitMQ.service.comsumer;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import shop.shop.order.service.OrderPaymentTimeoutService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayTimeoutMessageListener {

    private final OrderPaymentTimeoutService timeoutService;

    @RabbitListener(queues = "${app.rabbitMq.orderSepayCheckQueue}")
    public void handleSepayTimeout(String id) {
        parseOrderId(id).ifPresent(timeoutService::cancelIfTimeout);
    }

    private Optional<Long> parseOrderId(String id) {
        try {
            return Optional.of(Long.parseLong(id));
        } catch (NumberFormatException ex) {
            log.warn("Message timeout SEPAY không hợp lệ: {}", id);
            return Optional.empty();
        }
    }
}