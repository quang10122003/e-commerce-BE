// Mỗi implementation phụ trách 1 outcome (tình huống) khi nhận webhook
package shop.shop.payment.policy.interfaces;

import shop.shop.payment.policy.DTO.PaymentWebhookContext;

public interface IPaymentWebhookOutcomeHandler {
    boolean supports(PaymentWebhookContext context);

    void handle(PaymentWebhookContext context);
}
