package shop.shop.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.dto.response.PagedResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {
    private PagedResponse<AdminUserSummaryResponse> users;
    private AdminUserStatsResponse stats;
}
