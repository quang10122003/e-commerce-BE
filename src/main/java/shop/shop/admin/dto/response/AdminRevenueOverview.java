package shop.shop.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueOverview {
    BigDecimal weeklyRevenue; // Tổng doanh thu một tuần.
    BigDecimal weeklyRevenueGrowthRate ; // Tỉ lệ tăng trưởng so với tuần trước.
    List<AdminRevenueIn7day> adminRevenueIn7day; // Dữ liệu doanh thu 7 ngày trong tuần.
}
