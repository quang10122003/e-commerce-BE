package shop.shop.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import shop.shop.common.error.ApiError;
import shop.shop.common.dto.response.ApiResponse;

@RestControllerAdvice
public class ApiErrorException {

    @ExceptionHandler(ApiError.class)
    public ResponseEntity<ApiResponse<Object>> handleApiError(ApiError ex) {
        return ResponseEntity.status(ex.getErrorCode().getStatusCode())
                .body(ApiResponse.error(ex.getMessage(), ex));
    }
}
