package shop.shop.integration.RabbitMQ;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentStatus;
import shop.shop.order.entity.Order;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true  )
@RequiredArgsConstructor
public class RabbitProducerConsumer {
    OrderRepository orderRepository;
    PaymentRepo paymentRepo;

    
    @RabbitListener(queues = "${app.rabbitMq.orderSepayCheckQueue}")
    @Transactional
    public void handleSepayTimeout(String id) {
        Long orderId = Long.parseLong(id);

        Order order = orderRepository.findById(orderId)
                .orElse(null);
        PaymentEntity payment = paymentRepo.findByOrderId(orderId)
                .orElse(null);

        if (order == null) {
            // log
            return;
        }
        if (payment == null) {
            // log
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        payment.setStatus(PaymentStatus.FAILED);
    }
}
