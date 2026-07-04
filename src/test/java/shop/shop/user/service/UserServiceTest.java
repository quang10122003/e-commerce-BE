package shop.shop.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import shop.shop.Role.entity.Role;
import shop.shop.common.error.ApiError;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ErrorCode;
import shop.shop.admin.dto.response.AdminUserLockResponse;
import shop.shop.user.entity.User;
import shop.shop.user.mapper.UserMapper;
import shop.shop.user.repos.UserRepo;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private UserMapper adminUserMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUserLockStatus_throwsWhenLockingAdminAccount() {
        User adminUser = buildUser(1L, "ADMIN", false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(adminUser));

        ApiError error = assertThrows(ApiError.class, () -> userService.updateUserLockStatus(1L, true));

        assertEquals(ErrorCode.ADMIN_ACCOUNT_LOCK_NOT_ALLOWED, error.getErrorCode());
        verify(userRepo, never()).save(adminUser);
    }

    @Test
    void updateUserLockStatus_locksNonAdminAccount() {
        User normalUser = buildUser(2L, "USER", false);
        when(userRepo.findById(2L)).thenReturn(Optional.of(normalUser));

        ApiResponse<AdminUserLockResponse> response = userService.updateUserLockStatus(2L, true);

        assertTrue(response.success());
        assertEquals("Khoa tai khoan thanh cong", response.message());
        assertEquals(2L, response.data().getUserId());
        assertTrue(response.data().isLocked());
        assertEquals("LOCKED", response.data().getStatus());
        assertTrue(normalUser.isLocked());
        verify(userRepo).save(normalUser);
    }

    @Test
    void updateUserLockStatus_allowsUnlockingAdminAccount() {
        User adminUser = buildUser(3L, "ADMIN", true);
        when(userRepo.findById(3L)).thenReturn(Optional.of(adminUser));

        ApiResponse<AdminUserLockResponse> response = userService.updateUserLockStatus(3L, false);

        assertTrue(response.success());
        assertEquals("Mo khoa tai khoan thanh cong", response.message());
        assertFalse(response.data().isLocked());
        assertEquals("ACTIVE", response.data().getStatus());
        verify(userRepo).save(adminUser);
    }

    private User buildUser(Long id, String roleName, boolean locked) {
        Role role = new Role();
        role.setName(roleName);

        return User.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .password("secret")
                .fullName("User " + id)
                .role(role)
                .isLocked(locked)
                .build();
    }
}
