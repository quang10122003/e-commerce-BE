package shop.shop.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.auth.service.AuthService;
import shop.shop.common.dto.response.ApiResponse;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    AuthService authService;
    @Value("${app.doaminFE}")
    @NonFinal
    String frontendUrl;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // lấy thông tin user đăng nhập gg
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String sub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");
        ApiResponse<AuthResponse> result = authService.googleLogin(email, sub, fullName);
        AuthResponse data = result.data();

         String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/api/auth/google/callback")
                .queryParam("userId", data.getUserId())
                .queryParam("email", data.getEmail())
                .queryParam("fullName", data.getFullName())
                .queryParam("role", data.getRole())
                .queryParam("jwt", data.getJwt())
                .queryParam("accessToken", data.getAccessToken())
                .queryParam("refreshToken", data.getRefreshToken())
                .queryParam("tokenType", data.getTokenType())
                 .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
                System.out.println(redirectUrl);
        // chuyển hướng trình duyệt client để trình duyệt xử lý token
        response.sendRedirect(redirectUrl);

    }



}
