package shop.shop.admin.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private boolean locked;
    private Instant createdAt;
    private Instant updatedAt;
}
