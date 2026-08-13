package shop.shop.order.service;

import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.order.entity.Order;
import shop.shop.order.policy.interfaces.IOrderPaymentPolicy;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderPaymentPolicyService {

    List<IOrderPaymentPolicy> policies;

    public void handlePaymentWhenCancelOrder(Order order, CancelledBy cancelledBy) {
        findPolicy(order).handlePaymentWhenCancelOrder(order, cancelledBy);
    }

    public void validatePaymentBeforeChangeStatusOrder(Order order, OrderStatus targetStatus) {
        findPolicy(order).validatePaymentBeforeChangeStatus(order, targetStatus);
    }

    // lấy policies theo method của order
    private IOrderPaymentPolicy findPolicy(Order order) {
        return policies.stream()
                .filter(p -> p.getPaymentMethod() == order.getPaymentMethod())
                .findFirst()
                .orElse(NOOP_POLICY); // phòng trường hợp có PaymentMethod mới mà chưa kịp viết policy tương ứng
    }

    private static final IOrderPaymentPolicy NOOP_POLICY = new IOrderPaymentPolicy() {
        @Override
        public shop.shop.common.PaymentMethod getPaymentMethod() {
            return null;
        }

        @Override
        public void handlePaymentWhenCancelOrder(Order order, CancelledBy cancelledBy) {
        }

        @Override
        public void validatePaymentBeforeChangeStatus(Order order, OrderStatus targetStatus) {
        }
    };
}