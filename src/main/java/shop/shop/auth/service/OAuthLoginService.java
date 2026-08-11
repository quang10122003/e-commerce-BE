package shop.shop.auth.service;

import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import shop.shop.auth.dto.request.OAuthLoginRequest;
import shop.shop.auth.dto.response.AuthResponse;
import shop.shop.auth.provider.IOAuthProvider;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuthLoginService {

    List<IOAuthProvider> providers; // Spring tự inject tất cả bean implement OAuthProvider

    public ApiResponse<AuthResponse> login(String registrationId, OAuthLoginRequest request) {
        IOAuthProvider provider = providers.stream()
                .filter(p -> p.getRegistrationId().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new ApiError(ErrorCode.PROVIDER_NOT_SUPPORTED));

        return provider.login(request);
    }
}