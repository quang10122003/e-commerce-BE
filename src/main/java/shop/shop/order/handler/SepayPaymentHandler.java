// xử lý payment khai tạo order với payment sepay;
package shop.shop.order.handler;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;
import shop.shop.integration.RabbitMQ.QueueService;
import shop.shop.integration.RabbitMQ.DTO.OrderSepayDelayEventProducer;
import shop.shop.order.entity.Order;
import shop.shop.order.service.interfaces.IOrderPaymentHandler;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SepayPaymentHandler implements IOrderPaymentHandler {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    PaymentRepo paymentRepo;
    QueueService queueService;

    @NonFinal
    @Value("${app.rabbitMq.order-sepay-delay-ttl-ms}")
    int expired;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.SEPAY;
    }

    @Override
    public void afterOrderSaved(Order order, String orderCode) {
        LocalDateTime expiredAt = LocalDateTime.now().plus(Duration.ofMillis(expired));

        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .method(PaymentMethod.SEPAY)
                .transactionRef(orderCode)
                .status(PaymentStatus.PENDING)
                .expiredAt(expiredAt)
                .paidAt(null)
                .build();

        PaymentEntity paymentDone = paymentRepo.save(payment);

        try {
            queueService.publish(new OrderSepayDelayEventProducer(order.getId().toString()));
        } catch (AmqpException e) {
            logger.warn(
                    "rabbitMQ ko hoạt động order:{} ordercode:{} payment:{} payment của đơn hàng này sẽ ko tự động chuyển trạng thái khi hết time",
                    order.getId(), orderCode, paymentDone.getId());
        }
    }
}
