/**
 * Interface định nghĩa rule chuyển đổi trạng thái của Order.
 * Mỗi implementation chịu trách nhiệm kiểm tra các trạng thái đích
 * mà Order có thể chuyển đến từ một trạng thái nguồn cụ thể.
 */
package shop.shop.order.policy.interfaces;

import shop.shop.common.OrderStatus;
import shop.shop.order.entity.Order;


public interface IOrderStatusTransitionPolicy {
    // trạng thái hiện tại 
    OrderStatus fromStatus();

    // check xem từ trạng thái hiện tại chuyển đc đến trạng thái targetStatus ko 
    boolean canTransitionTo(Order order, OrderStatus targetStatus);
}