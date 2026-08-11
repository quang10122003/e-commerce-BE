// điều phối nghiệp vụ, không biết chi tiết từng loại thanh toán
package shop.shop.order.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentStatus;
import shop.shop.order.entity.Order;
import shop.shop.order.handler.IPaymentTimeoutHandler;
import shop.shop.order.handler.PaymentTimeoutHandlerRegistry;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentTimeoutService {

    private final OrderRepository orderRepository;
    private final PaymentRepo paymentRepo;
    private final PaymentTimeoutHandlerRegistry handlerRegistry;

    @Transactional
    public void cancelIfTimeout(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        PaymentEntity payment = paymentRepo.findByOrderId(orderId).orElse(null);

        if (order == null || payment == null) {
            log.warn("Không tìm thấy order/payment khi xử lý timeout: {}", orderId);
            return;
        }

        IPaymentTimeoutHandler handler = handlerRegistry.get(order.getPaymentMethod());
        if (handler == null) {
            return; // Phương thức thanh toán chưa hỗ trợ xử lý timeout
        }

        if (!isEligibleForCancel(order, payment)) {
            return;
        }

        handler.handleTimeout(order, payment);
        log.info("Tự động hủy order:{} do payment {} quá hạn", order.getId(), order.getPaymentMethod());
    }
// kiểm tra xem một đơn hàng có đủ điều kiện để bị tự động hủy do quá hạn thanh toán hay không
    private boolean isEligibleForCancel(Order order, PaymentEntity payment) {
        return order.getStatus() == OrderStatus.PENDING
                && payment.getStatus() == PaymentStatus.PENDING
                && payment.getExpiredAt() != null
                && !payment.getExpiredAt().isAfter(LocalDateTime.now());
    }
}