package shop.shop.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("Khong tim thay nguoi dung", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND("Ko tim thay san pham", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_REGISTERED("Email da duoc dang ky", HttpStatus.CONFLICT),
    ROLE_USER_NOT_FOUND("Khong tim thay role USER trong DB", HttpStatus.INTERNAL_SERVER_ERROR),
    REFRESH_TOKEN_INVALID("Refresh token khong hop le hoac da het han", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("Email hoac mat khau khong dung", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("Tai khoan da bi khoa", HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_FAILED("Xac thuc that bai", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("Ban khong co quyen truy cap", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("Unauthorized", HttpStatus.UNAUTHORIZED),
    EMAIL_REQUIRED("Email khong duoc de trong", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED("Mat khau khong duoc de trong", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT("Mat khau phai co it nhat 6 ky tu", HttpStatus.BAD_REQUEST),
    FULL_NAME_REQUIRED("Ho ten khong duoc de trong", HttpStatus.BAD_REQUEST),
    AUTHORIZATION_HEADER_REQUIRED("Authorization header khong duoc de trong", HttpStatus.BAD_REQUEST),
    AUTHORIZATION_HEADER_INVALID("Authorization header phai co dang Bearer <token>", HttpStatus.BAD_REQUEST),
    BEARER_TOKEN_INVALID("Bearer token khong hop le", HttpStatus.BAD_REQUEST),
    ADMIN_USER_STATUS_INVALID("Trạng thái lọc người dùng không hợp lệ", HttpStatus.BAD_REQUEST),
    ADMIN_ACCOUNT_LOCK_NOT_ALLOWED("Khong the khoa tai khoan admin", HttpStatus.BAD_REQUEST),
    ADMIN_ACCOUNT_DELETE_NOT_ALLOWED("Khong the khoa tai khoan admin", HttpStatus.BAD_REQUEST),
    ADMIN_USER_ROLE_INVALID("Role nguoi dung khong hop le", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("Du lieu khong hop le", HttpStatus.BAD_REQUEST),
    ILLEGAL_STATE("Trang thai khong hop le", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),

    USERNAME_DUPLICATE("User name đã tồn tại", HttpStatus.CONFLICT),
    PRODUCT_VERSION_CONFLICT("San pham vua duoc thay doi, vui long tai lai du lieu", HttpStatus.CONFLICT),
    CLOUDINARY_DELETE_FAILED("xóa ảnh trên cloud thất bại", HttpStatus.BAD_REQUEST),
    CATEGORY_ALREADY_EXISTS("Ten danh muc da ton tai", HttpStatus.CONFLICT),
    CATEGORY_NOT_FOUND("ko tim thay danh muc san pham", HttpStatus.NOT_FOUND),
    CHAT_ROOM_NOT_FOUND("Khong tim thay room chat", HttpStatus.NOT_FOUND),
    CAPTCHA_INVALID("Captcha khong hop le", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_NOT_FOUND("Token reset khong ton tai", HttpStatus.NOT_FOUND),
    RESET_TOKEN_USED("Token reset da duoc su dung", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_EXPIRED("Token reset da het han", HttpStatus.BAD_REQUEST),
    RESET_PASSWORD_FAILED("Reset password that bai", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_INVALID("Phuong thuc thanh toan khong hop le", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK("So luong ton kho khong du", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND("Khong tim thay don hang", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND("Khong tim thay thanh toan", HttpStatus.NOT_FOUND),
    PAYMENT_NO_LONGER_AVAILABLE("Thanh toan don hang khong con kha dung", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_SUPPORTED("Phương thức thanh toán không được hỗ trợ", HttpStatus.BAD_REQUEST),
    PAYMENT_EXPIRED("Thanh toán đã hết hạn", HttpStatus.BAD_REQUEST),
    INVALID_PERIOD_TYPE("Loại thời gian không hợp lệ", HttpStatus.BAD_REQUEST),
    VALIDATION("sai định dạng body gửi đến api",HttpStatus.BAD_REQUEST),
    LIMIT_REQUEST("limit request", HttpStatus.TOO_MANY_REQUESTS);

    String message;
    HttpStatusCode statusCode;
}
