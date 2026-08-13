/**
 * class  định nghĩa rule chuyển đổi trạng thái của Order ở trang thái peding.

 */
package shop.shop.order.policy;

import org.springframework.stereotype.Component;

import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.Order;
import shop.shop.order.policy.interfaces.IOrderStatusTransitionPolicy;

@Component
public class PendingTransitionPolicy  implements IOrderStatusTransitionPolicy{

    @Override
    public OrderStatus fromStatus() {
        return OrderStatus.PENDING;
    }

    @Override
    public boolean canTransitionTo(Order order, OrderStatus targetStatus) {
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            return targetStatus == OrderStatus.CANCELLED;
        }
        return targetStatus == OrderStatus.CONFIRMED || targetStatus == OrderStatus.CANCELLED;
    }
    
}
