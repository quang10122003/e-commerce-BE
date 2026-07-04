package shop.shop.exception;

import jakarta.persistence.OptimisticLockException;

import java.net.ConnectException;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import shop.shop.common.error.ApiError;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ErrorCode;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Sai email hoặc mật khẩu trong lúc đăng nhập.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return build(ErrorCode.INVALID_CREDENTIALS);
    }

    // Tài khoản bị khóa trong lúc đăng nhập hoặc xác thực request.
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleLockedException(LockedException ex) {
        // Giữ response đồng nhất giữa login bị khóa và các API khác khi token thuộc tài
        // khoản bị khóa.
        return build(ErrorCode.ACCOUNT_LOCKED);
    }

    // Không tìm thấy user trong quá trình xác thực của Spring Security.
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return build(ErrorCode.USER_NOT_FOUND, ex.getMessage());
    }

    // Lỗi xác thực tổng quát không thuộc riêng trường hợp sai mật khẩu.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        return build(ErrorCode.AUTHENTICATION_FAILED, ex.getMessage());
    }

    // User đã đăng nhập nhưng không đủ quyền truy cập tài nguyên.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.ACCESS_DENIED);
    }

    // Lỗi dữ liệu đầu vào hoặc tham số request không hợp lệ.
    @ExceptionHandler(ApiError.class)
    public ResponseEntity<ApiResponse<Object>> handleApiError(ApiError ex) {
        return build(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(ErrorCode.BAD_REQUEST, ex.getMessage());
    }

    // Lỗi trạng thái nghiệp vụ không đúng với điều kiện hệ thống hiện tại.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        return build(ErrorCode.ILLEGAL_STATE, ex.getMessage());
    }

    @ExceptionHandler({ OptimisticLockException.class, ObjectOptimisticLockingFailureException.class })
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLock(Exception ex) {
        return build(ErrorCode.PRODUCT_VERSION_CONFLICT);
    }

    // Lỗi database (SQL sai, column không tồn tại, constraint vi phạm, v.v.).
    // Phải bắt tường minh ở đây để tránh bị nuốt thành UNAUTHORIZED bởi Spring
    // Security filter.
    // Log đầy đủ để debug nhưng không expose chi tiết SQL ra client.
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccess(DataAccessException ex) {
        log.error("Database error: {}", ex.getMessage(), ex);
        return build(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    // lỗi validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleExceptionValidation(MethodArgumentNotValidException e){
        return build(ErrorCode.VALIDATION, e.getFieldError().getDefaultMessage());
    }
    @ExceptionHandler(AmqpConnectException.class)
    public void handleConnectException(ConnectException ex){
        log.warn("rabbitMQ đang ko hoạt động:{}",ex.getMessage());
    }
        
      
    

    // Fallback cuối cùng cho các exception chưa được bắt ở trên.
    // Bật handler này để lỗi hệ thống không bị nuốt thành UNAUTHORIZED.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // Dùng message mặc định trong ErrorCode.
    private ResponseEntity<ApiResponse<Object>> build(ErrorCode errorCode) {
        return build(errorCode, errorCode.getMessage());
    }

    // Tạo response lỗi theo format ApiResponse.error(...) và gán HTTP status từ
    // ErrorCode.
    private ResponseEntity<ApiResponse<Object>> build(ErrorCode errorCode, String message) {
        ApiError apiError = new ApiError(errorCode, message);
        return ResponseEntity.status(errorCode.getStatusCode()).body(ApiResponse.error(message, apiError));
    }
    
}
