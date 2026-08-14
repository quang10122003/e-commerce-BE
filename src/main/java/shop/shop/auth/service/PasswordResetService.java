// class này đảm nhiệm quên mk (captcha, provider mail, thời hạn token) .
package shop.shop.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.auth.dto.request.ForgotPasswordRequest;
import shop.shop.auth.dto.request.ResetPasswordRequest;
import shop.shop.auth.entity.PasswordResetToken;
import shop.shop.auth.repo.PasswordResetTokenRepo;
import shop.shop.common.AuthProvider;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.ValidationUtils;
import shop.shop.integration.CloudflareTurnstile.service.interfaces.ICaptchaVerifier;
import shop.shop.integration.RabbitMQ.QueueService;
import shop.shop.integration.RabbitMQ.DTO.ResetPasswordProducer;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetService {

    UserRepo userRepo;
    PasswordEncoder passwordEncoder;
    ICaptchaVerifier captchaVerifier;
    QueueService webSocketService;
    PasswordResetTokenRepo passwordResetTokenRepo;
    AuthSupport authSupport;
    ValidationUtils validationUtils;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // hàm nhận yêu cầu quên mk để gửi mail
    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
        String email = authSupport.normalizeEmail(request.getEmail());
        String captchaToken = request.getCaptchaToken();

        if (!validationUtils.hasText(email)) {
            throw new ApiError(ErrorCode.EMAIL_REQUIRED);
        }

        if (!validationUtils.hasText(captchaToken)) {
            throw new ApiError(ErrorCode.CAPTCHA_INVALID);
        }

        boolean validCaptcha = captchaVerifier.verify(captchaToken);

        if (!validCaptcha) {
            throw new ApiError(ErrorCode.CAPTCHA_INVALID);
        }

        Optional<User> userOptional = userRepo.findByEmailIgnoreCase(email);

        if (userOptional.isEmpty()) {
            logger.info("ko tìm thấy email:{} để gửi mail reset password", request.getEmail());
            return ApiResponse.success(
                    "vui lòng vào mail của bạn check mail để reset mật khẩu",
                    null);
        }
        User user = userOptional.get();
        if(user.getProvider() == AuthProvider.GOOGLE){
            logger.info("tài khoản mail: {} chưa đc đăng ký mới chỉ đăng nhập bằng gg nên ko thể lấy lại mk", request.getEmail());
            return ApiResponse.success(
                    "vui lòng vào mail của bạn check mail để reset mật khẩu",
                    null);
        }

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUsed(false);
        resetToken.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        resetToken.setUser(user);

        passwordResetTokenRepo.save(resetToken);

        ResetPasswordProducer resetPasswordProducer = new ResetPasswordProducer(email, token);
        webSocketService.publish(resetPasswordProducer);

        logger.info("yêu cầu lấy lại mk đc chấp nhận cho email:{}", request.getEmail());
        return ApiResponse.success(
                "Neu email ton tai, chung toi da gui mail.",
                null);
    }

    // hàm reset password khi quên mk 
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        if (!validationUtils.hasText(token)) {
            throw new ApiError(ErrorCode.RESET_TOKEN_NOT_FOUND);
        }

        if (!validationUtils.hasText(newPassword)) {
            throw new ApiError(ErrorCode.PASSWORD_REQUIRED);
        }
        if (newPassword.length() < 6) {
            throw new ApiError(ErrorCode.PASSWORD_TOO_SHORT);
        }

        PasswordResetToken resetToken = passwordResetTokenRepo
                .findByToken(token)
                .orElseThrow(() -> new ApiError(ErrorCode.RESET_TOKEN_NOT_FOUND));

        if (resetToken.isUsed()) {
            throw new ApiError(ErrorCode.RESET_TOKEN_USED);
        }

        if (resetToken.getExpiredAt() == null || resetToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ApiError(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        resetToken.setUsed(true);
        logger.info("user voi id {} vừa lấy lại mk và đổi mk thành công ", user.getId());

        return ApiResponse.success("Doi mat khau thanh cong", null);
    }
}
