
/**
 * Registry quản lý các policy chuyển trạng thái của Order.
 * Mỗi policy được đăng ký theo trạng thái nguồn (fromStatus),
 * giúp tìm và lấy đúng policy để kiểm tra việc chuyển sang trạng thái đích.
 */
package shop.shop.order.policy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import shop.shop.common.OrderStatus;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.order.policy.interfaces.IOrderStatusTransitionPolicy;

@RequiredArgsConstructor
@Component
public class OrderStatusTransitionPolicyRegistry {
    private final List<IOrderStatusTransitionPolicy> policyList;
    private Map<OrderStatus, IOrderStatusTransitionPolicy> policies;
   
    @PostConstruct
    void init() {
        policies = policyList.stream()
                .collect(Collectors.toMap(
                        IOrderStatusTransitionPolicy::fromStatus,
                        Function.identity()));
    }

    // lấy policie theo status order
    public IOrderStatusTransitionPolicy getPolicy(OrderStatus fromStatus) {
        IOrderStatusTransitionPolicy policy = policies.get(fromStatus);
        if (policy == null) {
            throw new ApiError(ErrorCode.ILLEGAL_STATE, "Không thể đổi trạng thái đơn hàng");
        }
        return policy;
    }

}
