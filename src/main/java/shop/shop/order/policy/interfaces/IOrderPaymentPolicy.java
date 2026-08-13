/**
 * Interface định nghĩa các chính sách xử lý thanh toán theo từng phương thức
 * trong vòng đời của đơn hàng, bao gồm xử lý khi đơn hàng bị hủy và kiểm tra
 * điều kiện thanh toán trước khi thay đổi trạng thái đơn hàng.
 */
package shop.shop.order.policy.interfaces;

import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.Order;

public interface IOrderPaymentPolicy {

    PaymentMethod getPaymentMethod();

    
    void handlePaymentWhenCancelOrder(Order order, CancelledBy cancelledBy);

   
    void validatePaymentBeforeChangeStatus(Order order, OrderStatus targetStatus) ;
}