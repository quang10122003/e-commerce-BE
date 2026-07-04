package shop.shop.admin.dto.response;

import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AdminRevenueRepone {
     Map<String, AdminRevenueKpi > kpis; // key: "totalRevenue", "pending"
     List<AdminTrendSeries> trendSeries;
     List<AdminComparisonSeries> comparisonSeries;
}
