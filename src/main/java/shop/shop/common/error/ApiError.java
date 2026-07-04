package shop.shop.common.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import lombok.AccessLevel;
@Data
@JsonIgnoreProperties({ "cause", "stackTrace", "suppressed", "localizedMessage" })
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiError extends RuntimeException {
        ErrorCode errorCode;
        // Exception tùy chỉnh.
        public ApiError(ErrorCode errorCode) {
                super(errorCode.getMessage()); // Đẩy thông báo lỗi của ErrorCode vào custom exception qua hàm khởi tạo của
                                               // lớp RuntimeException.
                this.errorCode = errorCode;
        }

        // Exception Spring Security theo format ApiError.
        public ApiError(ErrorCode errorCode, String message) {
                super(message);
                this.errorCode = errorCode;
        }

}
