package shop.shop.auth.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthLoginRequest {
    String rawEmail;
    String providerId; // sub bên Google, id bên Facebook/GitHub...
    String fullName;
}