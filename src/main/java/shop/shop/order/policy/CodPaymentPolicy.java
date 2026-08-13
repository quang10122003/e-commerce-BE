/**
 * class định nghĩa các chính sách xử lý thanh toán theo  phương thức COD 
 * trong vòng đời của đơn hàng, bao gồm xử lý khi đơn hàng bị hủy và kiểm tra
 * điều kiện thanh toán trước khi thay đổi trạng thái đơn hàng.
 */
package shop.shop.order.policy;

import org.springframework.stereotype.Component;

import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.Order;
import shop.shop.order.policy.interfaces.IOrderPaymentPolicy;

@Component
public class CodPaymentPolicy implements IOrderPaymentPolicy {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.COD;
    }

    @Override
    public void handlePaymentWhenCancelOrder(Order order, CancelledBy cancelledBy) {
    }

    @Override
    public void validatePaymentBeforeChangeStatus(Order order, OrderStatus targetStatus) {
    }

}