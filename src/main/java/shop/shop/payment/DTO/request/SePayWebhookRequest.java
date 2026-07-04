package shop.shop.payment.DTO.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SePayWebhookRequest {

    Long id;

    String gateway;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime transactionDate;

    String accountNumber;

    String subAccount;

    String code;

    String content;

    String transferType;

    String description;

    BigDecimal transferAmount;

    BigDecimal accumulated;

    String referenceCode;
}