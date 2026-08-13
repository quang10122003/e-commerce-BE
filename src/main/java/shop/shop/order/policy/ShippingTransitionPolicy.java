/**
 * class  định nghĩa rule chuyển đổi trạng thái của Order ở trang thái SHIPPING.

 */
package shop.shop.order.policy;

import org.springframework.stereotype.Component;

import shop.shop.common.OrderStatus;
import shop.shop.order.entity.Order;
import shop.shop.order.policy.interfaces.IOrderStatusTransitionPolicy;

@Component
public class ShippingTransitionPolicy implements IOrderStatusTransitionPolicy  {

    @Override
    public OrderStatus fromStatus() {
        return OrderStatus.SHIPPING;
    }

    @Override
    public boolean canTransitionTo(Order order, OrderStatus targetStatus) {
        return targetStatus == OrderStatus.COMPLETED ;
    }
    
}
