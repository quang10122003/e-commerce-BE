package shop.shop.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String jwt;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
