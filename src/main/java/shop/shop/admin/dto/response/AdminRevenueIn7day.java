package shop.shop.admin.dto.response;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueIn7day {
    BigDecimal revenueInDay; 
    LocalDateTime createdAt;
}
