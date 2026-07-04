package shop.shop.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatsResponse {
    private long totalUsers;
    private long adminUsers;
    private long lockedUsers;
}
