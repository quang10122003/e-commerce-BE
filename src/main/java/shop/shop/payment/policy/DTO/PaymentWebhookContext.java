// Gói toàn bộ thông tin cần để 1 handler quyết định có xử lý webhook này hay không, và xử lý ra sao.
package shop.shop.payment.policy.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import shop.shop.payment.entity.PaymentEntity;

public record PaymentWebhookContext(PaymentEntity payment,
        String referenceCode,
        LocalDateTime transactionDate,
        String transactionRef,
        BigDecimal amount,
        boolean late) {
    
}
