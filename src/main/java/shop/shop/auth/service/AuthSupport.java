// Class này đảm nhận "cách xây TokenResponse, kiểm tra khóa tài khoản hoặc parse Bearer Token có thay đổi". Đây là hạ tầng dùng chung.
package shop.shop.auth.service;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
// import shop.shop.security.AuthUtil;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthSupport {
    // AuthUtil authUtil;
    UserRepo userRepo;

    // kiểm tra email
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // check kiểm tra xem tài khoản có bị khóa hay k
    public void assertUserNotLocked(User user) {
        if (user.isLocked()) {
            throw new LockedException(ErrorCode.ACCOUNT_LOCKED.getMessage());
        }
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

    // Lưu ý: trùng ý tưởng với CurrentUserClass đang dùng ở module Order
    // (đã ghi trong review, mục "Medium: Trùng lặp logic lấy user hiện tại").
    // Khác biệt là ở đây có thêm assertUserNotLocked. Nếu muốn gộp hẳn về 1
    // CurrentUserProvider chung toàn hệ thống, làm ở bước refactor sau.
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepo.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        assertUserNotLocked(user);
        return user;
    }
}