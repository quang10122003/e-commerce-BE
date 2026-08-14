// Trường hợp mặc định (fallback): payment/order vẫn khả dụng và chưa trễ
// hạn. Luôn "supports"
package shop.shop.payment.policy;

import java.math.BigDecimal;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentStatus;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.policy.DTO.PaymentWebhookContext;
import shop.shop.payment.policy.interfaces.IPaymentWebhookOutcomeHandler;
import shop.shop.payment.repo.PaymentRepo;
import shop.shop.payment.service.PaymentWebhookNotifier;


@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OnTimePaymentOutcomeHandler implements IPaymentWebhookOutcomeHandler {
    PaymentRepo paymentRepo;
    OrderRepository orderRepository;
    PaymentWebhookNotifier notifier;

    @Override
    public boolean supports(PaymentWebhookContext context) {
        return true;
    }

    @Override
    public void handle(PaymentWebhookContext context) {
        PaymentEntity payment = context.payment();
        BigDecimal amount = context.amount();

        if (amount.compareTo(payment.getOrder().getTotalAmount()) != 0) {
            log.warn("Khách chuyển khoản sai số tiền: expected={}, received={}",
                    payment.getOrder().getTotalAmount(), amount);
            notifier.notify(context.transactionRef(), "AMOUNT_MISMATCH", "Số tiền không khớp");
            return;
        }

        payment.setPaidAt(context.transactionDate());
        payment.setReferenceCode(context.referenceCode());
        payment.setStatus(PaymentStatus.PAID);
        paymentRepo.save(payment);

        payment.getOrder().setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(payment.getOrder());

        log.info("Thanh toán thành công: {}", context.transactionRef());
        notifier.notify(context.transactionRef(), "SUCCESS", "Thanh toán thành công!");
    }
}