package shop.shop.auth.provider;

import shop.shop.auth.dto.request.OAuthLoginRequest;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.common.dto.response.ApiResponse;

public interface IOAuthProvider {

    String getRegistrationId();

    ApiResponse<AuthResponse> login(OAuthLoginRequest request);
}