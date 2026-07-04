package shop.shop.admin.dto.response;

import java.math.BigDecimal;

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
public class AdminOrderProductItemRepone {
    Long id;
    Long productId;
    String name;
    String category;
    Integer quantity;
    BigDecimal price;
    String thumbnail;
}
