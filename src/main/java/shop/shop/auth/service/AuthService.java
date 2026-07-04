package shop.shop.auth.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import io.jsonwebtoken.JwtException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.Role.entity.Role;
import shop.shop.Role.repo.RoleRepository;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.auth.dto.response.AccessTokenValidationResponse;
import shop.shop.auth.dto.response.CurrentUserResponse;
import shop.shop.auth.dto.response.WsTicketResponse;
import shop.shop.auth.entity.PasswordResetToken;
import shop.shop.auth.repo.PasswordResetTokenRepo;
import shop.shop.auth.dto.request.ForgotPasswordRequest;
import shop.shop.auth.dto.request.LoginRequest;
import shop.shop.auth.dto.request.ResetPasswordRequest;
import shop.shop.auth.dto.response.RefreshTokenResponse;
import shop.shop.auth.dto.request.SingUpResquest;
import shop.shop.common.error.ApiError;
import shop.shop.common.AuthProvider;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ErrorCode;
import shop.shop.integration.CloudflareTurnstile.service.TurnstileService;
import shop.shop.integration.Resend.service.ResendService;
import shop.shop.security.AuthUtil;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    AuthenticationManager authenticationManager;
    AuthUtil authUtil;
    UserRepo userRepo;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    TurnstileService turnstileService;
    ResendService resendService;
    PasswordResetTokenRepo passwordResetTokenRepo;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        String password = normalizePassword(request.getPassword());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        // Lay UserDetails, khong phai entity.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Lay lai entity tu DB.
        User user = userRepo.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        // Chi cho phep dang nhap local voi provider LOCAL hoac LOCAL_GOOGLE.
        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw new ApiError(ErrorCode.USER_NOT_FOUND);
        }

        // Chan truong hop tai khoan vua bi khoa trong DB nhung client van con dang
        // nhap.
        assertUserNotLocked(user);

        String accessToken = authUtil.generateAccessToken(user);
        String refreshToken = authUtil.generateRefreshToken(user);

        logger.info("Đăng nhập thành công cho user: {} (id: {})", user.getEmail(), user.getId());

        return ApiResponse.success(
                "Dang nhap thanh cong",
                buildAuthResponse(user, accessToken, refreshToken));
    }

    @Transactional
    public ApiResponse<AuthResponse> signup(SingUpResquest request) {
        String email = normalizeEmail(request.getEmail());
        String password = normalizePassword(request.getPassword());
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
                buildAuthResponse(savedUser, accessToken, refreshToken));
    }

    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
        // lấy thông tin người dùng gửi lên
        String email = normalizeEmail(request.getEmail());
        String captchaToken = request.getCaptchaToken();

        if (email == null || email.isBlank()) {
            throw new ApiError(ErrorCode.EMAIL_REQUIRED);
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            throw new ApiError(ErrorCode.CAPTCHA_INVALID);
        }

        // check captcha có đúng hay ko
        boolean validCaptcha = turnstileService.verify(captchaToken);

        if (!validCaptcha) {
            throw new ApiError(ErrorCode.CAPTCHA_INVALID);
        }

        Optional<User> userOptional = userRepo.findByEmailIgnoreCase(email);

        // ko tìm thấy user vẫn gửi mail
        if (userOptional.isEmpty()) {
            logger.info("ko tìm thấy email:{} để gửi mail reset password", request.getEmail());
            return ApiResponse.success(
                    "vui lòng vào mail của bạn check mail để reset mật khẩu",
                    null);
        }
        // tìm thấy user
        User user = userOptional.get();

        // tạo PasswordResetToken
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUsed(false);
        // thời gian token hiệu lực là 15p
        resetToken.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        resetToken.setUser(user);

        passwordResetTokenRepo.save(resetToken);

        // gửi mail
        resendService.sendResetPasswordMail(user.getEmail(), token);

        logger.info("yêu cầu lấy lại mk đc chấp nhận cho email:{}", request.getEmail());
        return ApiResponse.success(
                "Neu email ton tai, chung toi da gui mail.",
                null);

    }

    // thay đổi lại mk mới
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        if (token == null || token.isBlank()) {
            throw new ApiError(ErrorCode.RESET_TOKEN_NOT_FOUND);
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new ApiError(ErrorCode.PASSWORD_REQUIRED);
        }
        // ko cho người dùng nhập password nhỏ hơn 6 kí tự
        if (newPassword.length() < 6) {
            throw new ApiError(ErrorCode.PASSWORD_TOO_SHORT);
        }

        // tìm token trong hệ thống
        PasswordResetToken resetToken = passwordResetTokenRepo
                .findByToken(token)
                .orElseThrow(() -> new ApiError(ErrorCode.RESET_TOKEN_NOT_FOUND));

        // check token bị sử dụng chưa
        if (resetToken.isUsed()) {
            throw new ApiError(ErrorCode.RESET_TOKEN_USED);
        }

        // check token còn hạn ko
        if (resetToken.getExpiredAt() == null || resetToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ApiError(ErrorCode.RESET_TOKEN_EXPIRED);
        }
        // lấy user và reset password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        // đánh dấu token đã sử dụng
        resetToken.setUsed(true);
        logger.info("user voi id {} vừa lấy lại mk và đổi mk thành công ", user.getId());

        return ApiResponse.success("Doi mat khau thanh cong", null);

    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public ApiResponse<RefreshTokenResponse> refreshAccessToken(String authorizationHeader) {
        String refreshToken = extractBearerToken(authorizationHeader);

        try {
            String email = authUtil.extractEmail(refreshToken);
            User user = userRepo.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
            // Tai khoan da bi khoa thi khong duoc cap access token moi nua.
            assertUserNotLocked(user);

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

    @Transactional(readOnly = true)
    public ApiResponse<AccessTokenValidationResponse> validateAccessToken(String authorizationHeader) {
        try {
            String accessToken = extractBearerToken(authorizationHeader);
            String email = authUtil.extractEmail(accessToken);

            User user = userRepo.findByEmailIgnoreCase(email)
                    .orElse(null);

            if (user != null) {
                // Neu token thuoc ve tai khoan dang bi khoa thi tra loi giong luong login bi
                // khoa.
                assertUserNotLocked(user);
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

    @Transactional(readOnly = true)
    public ApiResponse<CurrentUserResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepo.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
        // Kiem tra bo sung o service de user bi khoa khong doc duoc thong tin chinh
        // minh.
        assertUserNotLocked(user);

        CurrentUserResponse response = CurrentUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRoleName())
                .build();

        return ApiResponse.success("Current user fetched", response);
    }

    @Transactional(readOnly = true)
    public ApiResponse<WsTicketResponse> createWsTicket() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepo.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
        // Ticket WebSocket duoc cap tu access token da xac thuc, nhung chi song rat
        // ngan
        // va chi hop le cho STOMP CONNECT, khong dung thay access token cho REST API.
        assertUserNotLocked(user);

        WsTicketResponse response = WsTicketResponse.builder()
                .ticket(authUtil.generateWsTicket(user))
                .tokenType("Bearer")
                .expiresInSeconds(authUtil.getWsTicketExpirationSeconds())
                .build();

        return ApiResponse.success("Cap WebSocket ticket thanh cong", response);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
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

    private void assertUserNotLocked(User user) {
        // Dung chung mot diem kiem tra de toan bo auth flow tra cung mot kieu loi khi
        // tai khoan bi khoa.
        if (user.isLocked()) {
            throw new LockedException(ErrorCode.ACCOUNT_LOCKED.getMessage());
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

    private String normalizePassword(String password) {
        return password;
    }

    private String normalizeFullName(String fullName) {
        return fullName == null ? null : fullName.trim();
    }

    private String extractBearerToken(String authorizationHeader) {
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

    // hàm xử lý đăng nhập bằng gg
    public ApiResponse<AuthResponse> googleLogin(String rawEmail, String sub, String fullName) {
        String email = normalizeEmail(rawEmail);
        User user;
        // casse 1: co tai khoan LOCAL trung email -> nang cap thanh LOCAL_GOOGLE
        Optional<User> localUserOpt = userRepo.findByEmailIgnoreCaseAndProvider(email, AuthProvider.LOCAL);
        if (localUserOpt.isPresent()) {
            User localUser = localUserOpt.get();
            localUser.setProviderId(sub);
            localUser.setProvider(AuthProvider.LOCAL_GOOGLE);
            user = userRepo.save(localUser);
        } else {
            // case : khong co LOCAL trung email -> tim theo sub (GOOGLE hoac
            // LOCAL_GOOGLE)
            Optional<User> UserSubOpt = userRepo.findByProviderId(sub);
            if (UserSubOpt.isPresent()) {
                user = UserSubOpt.get();
            } else {
                // case 3: khong tim thay ai ca -> tao moi voi provider = GOOGLE
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
        assertUserNotLocked(user);
        String accessToken = authUtil.generateAccessToken(user);
        String refreshToken = authUtil.generateRefreshToken(user);

        logger.info("Đăng nhập Google thành công cho user: {} (id: {})", user.getEmail(), user.getId());

        return ApiResponse.success(
                "Dang nhap google thanh cong",
                buildAuthResponse(user, accessToken, refreshToken));

    }
}
