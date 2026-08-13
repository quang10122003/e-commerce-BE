// calss này chỉ lo 4 use-case lõi: login, signup, refresh token, validate token.
package shop.shop.auth.service;

import java.util.Optional;
import io.jsonwebtoken.JwtException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.Role.entity.Role;
import shop.shop.Role.repo.RoleRepository;
import shop.shop.auth.dto.response.AccessTokenValidationResponse;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.auth.dto.request.LoginRequest;
import shop.shop.auth.dto.request.SingUpResquest;
import shop.shop.auth.dto.response.RefreshTokenResponse;
import shop.shop.common.AuthProvider;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.security.AuthUtil;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    AuthenticationManager authenticationManager;
    AuthUtil authUtil;
    UserRepo userRepo;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    AuthSupport authSupport;
    CurrentUserProvider currentUserProvider;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // hàm login
    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        String email = authSupport.normalizeEmail(request.getEmail());
        String password = request.getPassword();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepo.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw new ApiError(ErrorCode.USER_NOT_FOUND);
        }

        currentUserProvider.assertUserNotLocked(user);

        String accessToken = authUtil.generateAccessToken(user);
        String refreshToken = authUtil.generateRefreshToken(user);

        logger.info("Đăng nhập thành công cho user: {} (id: {})", user.getEmail(), user.getId());

        return ApiResponse.success(
                "Dang nhap thanh cong",
                authSupport.buildAuthResponse(user, accessToken, refreshToken));
    }


    @Transactional
    public ApiResponse<AuthResponse> signup(SingUpResquest request) {
        String email = authSupport.normalizeEmail(request.getEmail());
        String password = request.getPassword();
        String fullName = normalizeFullName(request.getFullName());

        validateSignupInput(email, password, fullName);

        Role userRole = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new ApiError(ErrorCode.ROLE_USER_NOT_FOUND));

        Optional<User> userOptional = userRepo.findByEmail(email);
        User savedUser;

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();

            if (existingUser.getProvider() != AuthProvider.GOOGLE) {
                throw new ApiError(ErrorCode.EMAIL_ALREADY_REGISTERED);
            }
            existingUser.setPassword(passwordEncoder.encode(password));
            existingUser.setProvider(AuthProvider.LOCAL_GOOGLE);

            savedUser = userRepo.save(existingUser);
        } else {
            User newUser = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode(password))
                    .role(userRole)
                    .isLocked(false)
                    .provider(AuthProvider.LOCAL)
                    .build();

            savedUser = userRepo.save(newUser);
        }

        String accessToken = authUtil.generateAccessToken(savedUser);
        String refreshToken = authUtil.generateRefreshToken(savedUser);

        logger.info("Đăng ký thành công cho user: {}", email);

        return ApiResponse.success(
                "Dang ky thanh cong",
                authSupport.buildAuthResponse(savedUser, accessToken, refreshToken));
    }

    @Transactional(readOnly = true)
    public ApiResponse<RefreshTokenResponse> refreshAccessToken(String authorizationHeader) {
        String refreshToken = authSupport.extractBearerToken(authorizationHeader);

        try {
            String email = authUtil.extractEmail(refreshToken);
            User user = userRepo.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
            currentUserProvider.assertUserNotLocked(user);

            if (!authUtil.isRefreshTokenValid(refreshToken, user)) {
                throw new ApiError(ErrorCode.REFRESH_TOKEN_INVALID);
            }

            String newAccessToken = authUtil.generateAccessToken(user);
            logger.info("user  với id {} vừa xin cấp lại accect token", user.getId());
            RefreshTokenResponse response = RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .tokenType("Bearer")
                    .build();

            return ApiResponse.success("Cap moi access token thanh cong", response);
        } catch (JwtException ex) {
            throw new ApiError(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    // check accectoken
    @Transactional(readOnly = true)
    public ApiResponse<AccessTokenValidationResponse> validateAccessToken(String authorizationHeader) {
        try {
            String accessToken = authSupport.extractBearerToken(authorizationHeader);
            String email = authUtil.extractEmail(accessToken);

            User user = userRepo.findByEmailIgnoreCase(email)
                    .orElse(null);

            if (user != null) {
                currentUserProvider.assertUserNotLocked(user);
            }

            boolean isValid = user != null && authUtil.isAccessTokenValid(accessToken, user);

            return ApiResponse.success(
                    isValid ? "Access token hop le" : "Access token khong hop le hoac da het han",
                    AccessTokenValidationResponse.builder()
                            .valid(isValid)
                            .build());
        } catch (JwtException | ApiError ex) {
            return ApiResponse.success(
                    "Access token khong hop le hoac da het han",
                    AccessTokenValidationResponse.builder()
                            .valid(false)
                            .build());
        }
    }


    private void validateSignupInput(String email, String password, String fullName) {
        if (email == null || email.isBlank()) {
            throw new ApiError(ErrorCode.EMAIL_REQUIRED);
        }
        if (password == null || password.isBlank()) {
            throw new ApiError(ErrorCode.PASSWORD_REQUIRED);
        }
        if (password.length() < 6) {
            throw new ApiError(ErrorCode.PASSWORD_TOO_SHORT);
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ApiError(ErrorCode.FULL_NAME_REQUIRED);
        }
    }

    // validation ffull name 
    private String normalizeFullName(String fullName) {
        return fullName == null ? null : fullName.trim();
    }
}
