package shop.shop.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.auth.dto.response.AccessTokenValidationResponse;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.auth.dto.response.CurrentUserResponse;
import shop.shop.auth.dto.response.WsTicketResponse;
import shop.shop.auth.dto.request.ForgotPasswordRequest;
import shop.shop.auth.dto.request.LoginRequest;
import shop.shop.auth.dto.request.ChangePasswordRequest;
import shop.shop.auth.dto.request.ResetPasswordRequest;
import shop.shop.auth.dto.request.UpdateProfileRequest;
import shop.shop.auth.dto.response.RefreshTokenResponse;
import shop.shop.auth.dto.request.SingUpResquest;
import shop.shop.auth.service.AuthenticationService;
import shop.shop.auth.service.PasswordResetService;
import shop.shop.auth.service.UserProfileService;
import shop.shop.auth.service.WsTicketService;
import shop.shop.common.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;
    PasswordResetService passwordResetService;
    UserProfileService userProfileService;
    WsTicketService wsTicketService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(200).body(authenticationService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SingUpResquest request) {
        return ResponseEntity.status(201).body(authenticationService.signup(request));
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(authenticationService.refreshAccessToken(authorizationHeader));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<AccessTokenValidationResponse>> validateToken(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(authenticationService.validateAccessToken(authorizationHeader));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {
        return ResponseEntity.ok(userProfileService.me());
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateCurrentUser(request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userProfileService.changeCurrentUserPassword(request));
    }

    @PostMapping("/ws-ticket")
    public ResponseEntity<ApiResponse<WsTicketResponse>> createWsTicket() {
        return ResponseEntity.ok(wsTicketService.createWsTicket());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }
}