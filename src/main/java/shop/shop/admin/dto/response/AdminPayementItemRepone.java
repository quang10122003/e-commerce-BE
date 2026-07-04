package shop.shop.admin.dto.response;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPayementItemRepone {
     Long id;
    String orderCode;
    PaymentMethod method;
    PaymentStatus status;
    String transactionRef;
    LocalDateTime paidAt;
}
