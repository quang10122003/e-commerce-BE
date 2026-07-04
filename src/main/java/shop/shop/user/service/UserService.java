package shop.shop.user.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.Role.entity.Role;
import shop.shop.Role.repo.RoleRepository;
import shop.shop.admin.dto.request.AdminUserUpdateRequest;
import shop.shop.admin.dto.response.AdminUserDetailResponse;
import shop.shop.admin.dto.response.AdminUserListResponse;
import shop.shop.admin.dto.response.AdminUserLockResponse;
import shop.shop.admin.dto.response.AdminUserStatsResponse;
import shop.shop.admin.dto.response.AdminUserSummaryResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.dto.response.PagedResponse;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserClass;
import shop.shop.user.entity.User;
import shop.shop.user.mapper.UserMapper;
import shop.shop.user.repos.UserRepo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class UserService {
    Logger logger = LoggerFactory.getLogger(this.getClass());
   UserRepo userRepo;
   UserMapper userMapper;
   RoleRepository roleRepository;
   CurrentUserClass currentUserClass;

    // Lay thong tin user danh cho admin.
    public ApiResponse<AdminUserDetailResponse> getAdminUserById(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        AdminUserDetailResponse response = userMapper.toDetail(user);

        return ApiResponse.success("Lay thong tin nguoi dung thanh cong", response);
    }

    // Chuan hoa gia tri tim kiem dau vao.
    private String normalize(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Lay danh sach user cho admin.
    public ApiResponse<AdminUserListResponse> getAdminUsers(
            String search, String role, String status, Pageable pageable) {

        String normalizedSearch = normalize(search);
        String normalizedRole = normalize(role);
        String normalizedStatus = normalize(status);

        if (normalizedStatus != null
                && !"ACTIVE".equalsIgnoreCase(normalizedStatus)
                && !"LOCKED".equalsIgnoreCase(normalizedStatus)) {
            throw new ApiError(ErrorCode.ADMIN_USER_STATUS_INVALID);
        }

        Page<AdminUserSummaryResponse> page = userRepo
                .findAdminUsers(normalizedSearch, normalizedRole, normalizedStatus, pageable)
                .map(userMapper::toSummary);

        AdminUserStatsResponse stats = new AdminUserStatsResponse(
                userRepo.countAllUsersForAdmin(),
                userRepo.countAdminUsersForAdmin(),
                userRepo.countLockedUsersForAdmin());

        return ApiResponse.success("Admin user list fetched",
                AdminUserListResponse.builder()
                        .users(PagedResponse.from(page))
                        .stats(stats)
                        .build());
    }

    // Cap nhat thong tin user danh cho admin.
    @Transactional
    public ApiResponse<AdminUserDetailResponse> updateAdminUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }

        if (request.getEmail() != null) {
            String email = normalizeEmailForUpdate(request.getEmail());
            userRepo.findByEmailIgnoreCase(email)
                    .filter(existingUser -> !existingUser.getId().equals(userId))
                    .ifPresent(existingUser -> {
                        throw new ApiError(ErrorCode.EMAIL_ALREADY_REGISTERED);
                    });
            user.setEmail(email);
        }

        if (request.getFullName() != null) {
            user.setFullName(normalizeRequiredValue(request.getFullName(), ErrorCode.FULL_NAME_REQUIRED));
        }

        if (request.getRole() != null) {
            String roleName = normalizeRoleForUpdate(request.getRole());
            Role role = roleRepository.findByNameIgnoreCase(roleName)
                    .orElseThrow(() -> new ApiError(ErrorCode.ADMIN_USER_ROLE_INVALID));
            user.setRole(role);
        }

        User savedUser = userRepo.save(user);
        logger.info("admin id:{} cập nhật thông tin cho user với thông tin cập nhật như sau:{}",currentUserClass.getCurrentUser().getId(),userId,request);
        return ApiResponse.success("Cap nhat thong tin nguoi dung thanh cong",
                userMapper.toDetail(savedUser));
    }

    private String normalizeEmailForUpdate(String email) {
        return normalizeRequiredValue(email, ErrorCode.EMAIL_REQUIRED).toLowerCase();
    }

    private String normalizeRoleForUpdate(String role) {
        String normalizedRole = normalizeRequiredValue(role, ErrorCode.ADMIN_USER_ROLE_INVALID);
        return normalizedRole.regionMatches(true, 0, "ROLE_", 0, "ROLE_".length())
                ? normalizedRole.substring("ROLE_".length())
                : normalizedRole;
    }

    private String normalizeRequiredValue(String value, ErrorCode errorCode) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            throw new ApiError(errorCode);
        }
        return normalizedValue;
    }

    // Khoa hoac mo khoa user.
    @Transactional
    public ApiResponse<AdminUserLockResponse> updateUserLockStatus(Long userId, boolean locked) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        if (locked && isAdminUser(user)) {
            throw new ApiError(ErrorCode.ADMIN_ACCOUNT_LOCK_NOT_ALLOWED);
        }

        user.setLocked(locked);
        userRepo.save(user);

        AdminUserLockResponse response = AdminUserLockResponse.builder()
                .userId(user.getId())
                .locked(user.isLocked())
                .status(user.isLocked() ? "LOCKED" : "ACTIVE")
                .build();

        String message = user.isLocked() ? "Khoa tai khoan thanh cong" : "Mo khoa tai khoan thanh cong";
        logger.info("admin id:{} {} cho user id: {}",currentUserClass.getCurrentUser().getId(),message,userId);
        return ApiResponse.success(message, response);
    }

    // Kiem tra user co phai admin khong.
    private boolean isAdminUser(User user) {
        return "ADMIN".equalsIgnoreCase(user.getRoleName());
    }

    // Xoa user.
    @Transactional
    public ApiResponse<Void> deleteUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
        if (isAdminUser(user)) {
            throw new ApiError(ErrorCode.ADMIN_ACCOUNT_DELETE_NOT_ALLOWED);
        }
        userRepo.delete(user);
        logger.info("admin id:{} xóa user id:{}",currentUserClass.getCurrentUser().getId(),userId);

        return ApiResponse.success("Xóa người dùng thành công với id: " + userId, null);
    }

}
