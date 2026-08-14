// Class này đảm nhận "cách xây TokenResponse, parse Bearer Token". Đây là hạ tầng dùng chung.
package shop.shop.auth.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.ValidationUtils;
import shop.shop.user.entity.User;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthSupport {

    ValidationUtils validationUtils;

    // kiểm tra email
    public String normalizeEmail(String email) {
        return validationUtils.normalizeEmail(email);
    }

    // tạo repone auth 
    public AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRoleName())
                .jwt(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    // kiểm tra uthorization Header vfa trả token đc lấy từ herder
    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ApiError(ErrorCode.AUTHORIZATION_HEADER_REQUIRED);
        }

        String headerValue = authorizationHeader.trim();
        if (!headerValue.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            throw new ApiError(ErrorCode.AUTHORIZATION_HEADER_INVALID);
        }

        String token = headerValue.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new ApiError(ErrorCode.BEARER_TOKEN_INVALID);
        }

        return token;
    }
}
