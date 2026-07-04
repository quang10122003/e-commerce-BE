package shop.shop.admin.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminOrdersRepone {
    Long total;
    Long today;
    Long pending;
    Long shipping;
    Long completed;
    Long cancelled;
    BigDecimal deliverySuccessRate;
    List<AdminOrderItemRepone> item;
}
