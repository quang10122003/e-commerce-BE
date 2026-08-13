package shop.shop.auth.provider;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import shop.shop.Role.entity.Role;
import shop.shop.Role.repo.RoleRepository;
import shop.shop.auth.dto.request.OAuthLoginRequest;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.auth.service.AuthSupport;
import shop.shop.common.AuthProvider;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.security.AuthUtil;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoogleOAuthProvider implements IOAuthProvider {

    UserRepo userRepo;
    RoleRepository roleRepository;
    AuthUtil authUtil;
    AuthSupport authSupport;
    CurrentUserProvider currentUserProvider;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public String getRegistrationId() {
        return "google";
    }

    @Override
    public ApiResponse<AuthResponse> login(OAuthLoginRequest request) {
        String email = authSupport.normalizeEmail(request.getRawEmail());
        String sub = request.getProviderId();
        String fullName = request.getFullName();
        User user;
        
        Optional<User> localUserOpt = userRepo.findByEmailIgnoreCaseAndProvider(email, AuthProvider.LOCAL);
        if (localUserOpt.isPresent()) {
            User localUser = localUserOpt.get();
            localUser.setProviderId(sub);
            localUser.setProvider(AuthProvider.LOCAL_GOOGLE);
            user = userRepo.save(localUser);
        } else {
            Optional<User> userSubOpt = userRepo.findByProviderId(sub);
            if (userSubOpt.isPresent()) {
                user = userSubOpt.get();
            } else {
                Role userRole = roleRepository.findByNameIgnoreCase("USER")
                        .orElseThrow(() -> new ApiError(ErrorCode.ROLE_USER_NOT_FOUND));

                User newUser = User.builder()
                        .email(email)
                        .fullName(fullName)
                        .providerId(sub)
                        .provider(AuthProvider.GOOGLE)
                        .role(userRole)
                        .isLocked(false)
                        .password(null)
                        .build();

                user = userRepo.save(newUser);
            }
        }

        currentUserProvider.assertUserNotLocked(user);
        String accessToken = authUtil.generateAccessToken(user);
        String refreshToken = authUtil.generateRefreshToken(user);

        logger.info("Đăng nhập Google thành công cho user: {} (id: {})", user.getEmail(), user.getId());

        return ApiResponse.success(
                "Dang nhap google thanh cong",
                authSupport.buildAuthResponse(user, accessToken, refreshToken));
    }
}