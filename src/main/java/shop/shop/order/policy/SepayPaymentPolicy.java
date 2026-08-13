/**
 * class định nghĩa các chính sách xử lý thanh toán theo  phương thức Sepay 
 * trong vòng đời của đơn hàng, bao gồm xử lý khi đơn hàng bị hủy và kiểm tra
 * điều kiện thanh toán trước khi thay đổi trạng thái đơn hàng.
 */
package shop.shop.order.policy;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.order.entity.Order;
import shop.shop.order.policy.interfaces.IOrderPaymentPolicy;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SepayPaymentPolicy implements IOrderPaymentPolicy {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    PaymentRepo paymentRepo;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.SEPAY;
    }

    @Override
    public void handlePaymentWhenCancelOrder(Order order, CancelledBy cancelledBy) {
        PaymentEntity payment = paymentRepo.findByOrderId(order.getId()).orElse(null);

        if (payment == null) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            logger.info("{} hủy order {} sepay payment chưa thanh toán nên chuyển payment:{} về FAILED",
                    cancelledBy, order.getId(), payment.getId());
            return;
        }

        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.PAID_LATE) {
            logger.warn("{} hủy order {} sepay payment id:{} đã thanh toán, cần kiểm tra hoàn tiền thủ công",
                    cancelledBy, order.getId(), payment.getId());
        }
    }

    @Override
    public void validatePaymentBeforeChangeStatus(Order order, OrderStatus targetStatus) {
        if (targetStatus != OrderStatus.COMPLETED) {
            return;
        }

        PaymentEntity payment = paymentRepo.findByOrderId(order.getId())
                .orElseThrow(() -> new ApiError(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiError(ErrorCode.ILLEGAL_STATE, "Đơn SEPAY chưa thanh toán thành công");
        }
    }
}