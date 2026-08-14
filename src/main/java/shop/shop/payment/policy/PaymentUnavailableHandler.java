// Ghi nhận tiền đến khi đơn/payment không còn ở trạng thái PENDING để xử lý
// hoàn tiền thủ công.
package shop.shop.payment.policy;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentStatus;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.policy.DTO.PaymentWebhookContext;
import shop.shop.payment.policy.interfaces.IPaymentWebhookOutcomeHandler;
import shop.shop.payment.repo.PaymentRepo;
import shop.shop.payment.service.PaymentWebhookNotifier;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class PaymentUnavailableHandler implements IPaymentWebhookOutcomeHandler  {
  PaymentRepo paymentRepo;
    PaymentWebhookNotifier notifier;
    @Override
    public boolean supports(PaymentWebhookContext context) {
        PaymentEntity payment = context.payment();
        return payment.getOrder().getStatus() != OrderStatus.PENDING
                || payment.getStatus() != PaymentStatus.PENDING;
    }

    @Override
    public void handle(PaymentWebhookContext context) {
        log.warn("Thanh toán đến khi đơn không còn khả dụng: {}", context.transactionRef());

        PaymentEntity payment = context.payment();
        payment.setPaidAt(context.transactionDate());
        payment.setReferenceCode(context.referenceCode());
        payment.setStatus(PaymentStatus.PAID_LATE);
        paymentRepo.save(payment);

        notifier.notify(context.transactionRef(), "PAID_LATE",
                "Thanh toán đến khi đơn không còn khả dụng, cần kiểm tra hoàn tiền");
    }
    
}
