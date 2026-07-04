package shop.shop.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import shop.shop.auth.dto.request.ResetPasswordRequest;
import shop.shop.auth.dto.response.RefreshTokenResponse;
import shop.shop.auth.dto.request.SingUpResquest;
import shop.shop.auth.service.AuthService;
import shop.shop.common.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(200).body(authService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SingUpResquest request) {
        return ResponseEntity.status(201).body(authService.signup(request));
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(authService.refreshAccessToken(authorizationHeader));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<AccessTokenValidationResponse>> validateToken(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(authService.validateAccessToken(authorizationHeader));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/ws-ticket")
    public ResponseEntity<ApiResponse<WsTicketResponse>> createWsTicket() {
        return ResponseEntity.ok(authService.createWsTicket());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
    