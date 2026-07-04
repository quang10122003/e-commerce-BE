package shop.shop.common.until;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class CurrentUserClass {
    UserRepo userRepo;

    // Lấy user từ context authentication.
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        return userRepo.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
    }
}
