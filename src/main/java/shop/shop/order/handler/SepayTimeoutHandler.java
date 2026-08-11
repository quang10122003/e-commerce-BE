// logic riêng cho SEPAY order timeout
package shop.shop.order.handler;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;
import shop.shop.order.entity.Order;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.product.repository.ProductRepository;

@Component
@RequiredArgsConstructor
public class SepayTimeoutHandler implements IPaymentTimeoutHandler {

    private final ProductRepository productRepository;

    @Override
    public PaymentMethod supports() {
        return PaymentMethod.SEPAY;
    }

    @Override
    public void handleTimeout(Order order, PaymentEntity payment) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(CancelledBy.SYSTEM);
        payment.setStatus(PaymentStatus.FAILED);
        productRepository.restoreStockByOrderId(order.getId());
    }
}