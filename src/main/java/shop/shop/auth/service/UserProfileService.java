// class đảm nhận profile user đang đăng nhập (xem/sửa profile, đổi mật khẩu khi đã biết mật khẩu cũ)
package shop.shop.auth.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.auth.dto.request.ChangePasswordRequest;
import shop.shop.auth.dto.request.UpdateProfileRequest;
import shop.shop.auth.dto.response.CurrentUserResponse;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileService {

    UserRepo userRepo;
    PasswordEncoder passwordEncoder;
    CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    // hàm lấy thông tin user 
    public ApiResponse<CurrentUserResponse> me() {
        User user = currentUserProvider.getCurrentUser();
        return ApiResponse.success("Current user fetched", buildCurrentUserResponse(user));
    }


    // update thông tin tài khoản user
    @Transactional
    public ApiResponse<CurrentUserResponse> updateCurrentUser(UpdateProfileRequest request) {
        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }

        User user = currentUserProvider.getCurrentUser();
        user.setFullName(normalizeFullName(request.getFullName()));
        userRepo.save(user);

        return ApiResponse.success("Cap nhat thong tin tai khoan thanh cong", buildCurrentUserResponse(user));
    }

    // thay đổi mk
    @Transactional
    public ApiResponse<Void> changeCurrentUserPassword(ChangePasswordRequest request) {
        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }

        User user = currentUserProvider.getCurrentUser();
        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ApiError(ErrorCode.BAD_REQUEST,
                    "Tai khoan nay chua co mat khau, vui long dat mat khau moi tu giao dien dang nhap neu can.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Mat khau hien tai khong dung");
        }

        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Mat khau moi phai khac mat khau hien tai");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        return ApiResponse.success("Doi mat khau thanh cong", null);
    }

    // validation fullname
    private String normalizeFullName(String fullName) {
        return fullName == null ? null : fullName.trim();
    }

    // build repone trả thông tin user 
    private CurrentUserResponse buildCurrentUserResponse(User user) {
        return CurrentUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRoleName())
                .build();
    }
}