// Thanh toán đến sau khi payment đã hết hạn (expiredAt) nhưng đơn vẫn đang PENDING - hủy đơn quá hạn và hoàn tồn kho ngay.
package shop.shop.payment.policy;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentStatus;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.policy.DTO.PaymentWebhookContext;
import shop.shop.payment.policy.interfaces.IPaymentWebhookOutcomeHandler;
import shop.shop.payment.repo.PaymentRepo;
import shop.shop.payment.service.PaymentWebhookNotifier;
import shop.shop.product.repository.ProductRepository;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LatePaymentOutcomeHandler implements IPaymentWebhookOutcomeHandler {
    PaymentRepo paymentRepo;
    OrderRepository orderRepository;
    ProductRepository productRepository;
    PaymentWebhookNotifier notifier;

    @Override
    public boolean supports(PaymentWebhookContext context) {
        return context.late();
    }

    @Override
    public void handle(PaymentWebhookContext context) {
        log.warn("Đơn hàng đã hết hạn, khách chuyển khoản muộn: {}", context.transactionRef());

        PaymentEntity payment = context.payment();
        payment.setPaidAt(context.transactionDate());
        payment.setReferenceCode(context.referenceCode());
        payment.setStatus(PaymentStatus.PAID_LATE);
        paymentRepo.save(payment);

        if (payment.getOrder().getStatus() == OrderStatus.PENDING) {
            payment.getOrder().setStatus(OrderStatus.CANCELLED);
            payment.getOrder().setCancelledBy(CancelledBy.SYSTEM);
            productRepository.restoreStockByOrderId(payment.getOrder().getId());
            orderRepository.save(payment.getOrder());
        }

        notifier.notify(context.transactionRef(), "PAID_LATE", "Thanh toán muộn, đơn hàng đã bị hủy");
    }

}
