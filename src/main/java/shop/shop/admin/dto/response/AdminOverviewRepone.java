package shop.shop.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewRepone {
    private AdminUserOverview adminUserOverview;
    private AdminProductOverview adminProductOverview;
    private AdminOrderOverview adminOrderOverview;
    private AdminRevenueOverview adminRevenueOverview;
}



