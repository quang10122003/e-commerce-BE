package shop.shop.order.policy;

import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.Order;

public interface IOrderPaymentPolicy {

    PaymentMethod getPaymentMethod();

    // Xử lý payment khi order bị hủy. Mặc định không làm gì (ví dụ COD).
    default void handlePaymentWhenCancelOrder(Order order, CancelledBy cancelledBy) {
    }

    // Kiểm tra điều kiện payment trước khi cho đổi sang targetStatus. Mặc định
    // không chặn gì.
    default void validatePaymentBeforeChangeStatus(Order order, OrderStatus targetStatus) {
    }
}