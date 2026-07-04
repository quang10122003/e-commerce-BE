package shop.shop.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AdminNewOrderOverviewProjection {
    Long getId();

    LocalDateTime getCreatedAt();

    String getShippingName();

    BigDecimal getTotalAmount();

    String getMethodPayment();

    String getStatusOrder();
}
