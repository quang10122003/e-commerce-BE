package shop.shop.common.until;

import java.util.Locale;

import org.springframework.stereotype.Component;

import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;

@Component
public class ValidationUtils {

    // Chuẩn hóa chuỗi: trim và đổi chuỗi rỗng thành null.
    public String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chuẩn hóa chuỗi bắt buộc, nếu rỗng thì ném lỗi tương ứng.
    public String requireNormalized(String value, ErrorCode errorCode) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            throw new ApiError(errorCode);
        }
        return normalizedValue;
    }

    // Chuẩn hóa email về dạng dùng chung trong hệ thống.
    public String normalizeEmail(String email) {
        String normalizedEmail = normalize(email);
        return normalizedEmail == null ? null : normalizedEmail.toLowerCase(Locale.ROOT);
    }

    // Kiểm tra chuỗi có dữ liệu hợp lệ hay không.
    public boolean hasText(String value) {
        return normalize(value) != null;
    }

    // Chuẩn hóa chuỗi rồi đổi sang enum, sai định dạng thì ném lỗi cấu hình sẵn.
    public <E extends Enum<E>> E parseEnumIgnoreCase(String value, Class<E> enumType, ErrorCode errorCode) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, normalizedValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiError(errorCode);
        }
    }

}
