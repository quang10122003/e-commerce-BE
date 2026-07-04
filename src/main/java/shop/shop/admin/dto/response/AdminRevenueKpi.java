package shop.shop.admin.dto.response;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRevenueKpi {
    BigDecimal value;
    // % thay đổi so với kì trc
    Double deltaPct;
}
