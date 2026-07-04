package shop.shop.common.dto.response;

import java.time.Instant;

import lombok.Builder;
import shop.shop.common.error.ApiError;
@Builder

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error, Instant.now());
    }
}
