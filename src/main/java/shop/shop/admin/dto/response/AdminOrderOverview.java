package shop.shop.admin.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderOverview {
    long todayOrderCount;
    long pendingOrderCount;
    List<AdminNewOrderOverview> adminNewOrderOverview;
}
