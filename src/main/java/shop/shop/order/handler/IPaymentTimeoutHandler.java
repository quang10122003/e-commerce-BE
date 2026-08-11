
// interface mở rộng theo phương thức thanh toán
package shop.shop.order.handler;

import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.Order;
import shop.shop.payment.entity.PaymentEntity;

public interface IPaymentTimeoutHandler {
    PaymentMethod supports();

    void handleTimeout(Order order, PaymentEntity payment);
}