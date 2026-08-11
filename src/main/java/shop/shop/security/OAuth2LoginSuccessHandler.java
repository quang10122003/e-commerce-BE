package shop.shop.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
import shop.shop.auth.dto.request.OAuthLoginRequest;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.auth.service.OAuthLoginService;
import shop.shop.common.dto.response.ApiResponse;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    OAuthLoginService oAuthLoginService;

    @Value("${app.frontend-url}")
    @NonFinal
    String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId(); 

        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String sub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");

        OAuthLoginRequest loginRequest = OAuthLoginRequest.builder()
                .rawEmail(email)
                .providerId(sub)
                .fullName(fullName)
                .build();

        ApiResponse<AuthResponse> result = oAuthLoginService.login(registrationId, loginRequest);
        AuthResponse data = result.data();

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/api/auth/" + registrationId + "/callback")
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

        response.sendRedirect(redirectUrl);
    }
}