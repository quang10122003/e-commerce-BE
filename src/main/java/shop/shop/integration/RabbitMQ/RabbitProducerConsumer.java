package shop.shop.integration.RabbitMQ;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;
import shop.shop.order.entity.Order;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;
import shop.shop.product.repository.ProductRepository;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true  )
@RequiredArgsConstructor
@Slf4j
public class RabbitProducerConsumer {
    OrderRepository orderRepository;
    PaymentRepo paymentRepo;
    ProductRepository productRepository;

    
    @RabbitListener(queues = "${app.rabbitMq.orderSepayCheckQueue}")
    @Transactional
    public void handleSepayTimeout(String id) {
        Long orderId = parseOrderId(id);
        if (orderId == null) {
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElse(null);
        PaymentEntity payment = paymentRepo.findByOrderId(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("Không tìm thấy order khi xử lý timeout SEPAY: {}", orderId);
            return;
        }
        if (payment == null) {
            log.warn("Không tìm thấy payment khi xử lý timeout SEPAY: {}", orderId);
            return;
        }

        if (order.getPaymentMethod() != PaymentMethod.SEPAY) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        if (payment.getExpiredAt() == null || payment.getExpiredAt().isAfter(LocalDateTime.now())) {
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(CancelledBy.SYSTEM);
        payment.setStatus(PaymentStatus.FAILED);
        productRepository.restoreStockByOrderId(order.getId());
        log.info("Tự động hủy order:{} do payment SEPAY quá hạn", order.getId());
    }

    // Chuyển message từ queue sang orderId, bỏ qua message không hợp lệ.
    private Long parseOrderId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            log.warn("Message timeout SEPAY không hợp lệ: {}", id);
            return null;
        }
    }
}
