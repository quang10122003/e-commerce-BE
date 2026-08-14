// Spring inject List<IPaymentWebhookOutcomeHandler> theo đúng thứ tự @Order
// của từng bean. Thử lần lượt, dùng handler đầu tiên "supports" context.
package shop.shop.payment.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.payment.policy.DTO.PaymentWebhookContext;
import shop.shop.payment.policy.interfaces.IPaymentWebhookOutcomeHandler;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
@Component
public class PaymentWebhookOutcomeRegistry {

     List<IPaymentWebhookOutcomeHandler> handlers;


    public IPaymentWebhookOutcomeHandler gethandler(PaymentWebhookContext context) {
        return handlers.stream()
                .filter(handler -> handler.supports(context))
                .findFirst()
                .orElseThrow(() -> new ApiError(ErrorCode.ILLEGAL_STATE,
                        "Không xác định được cách xử lý webhook thanh toán"));
    }
}