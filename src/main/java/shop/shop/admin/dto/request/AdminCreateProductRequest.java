package shop.shop.admin.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import shop.shop.common.ProductStatus;

@Data
public class AdminCreateProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 2, max = 50, message = "Tên sản phẩm phải từ 2 đến 50 ký tự")
    private String name;

    @Size(max = 100, message = "Mô tả sản phẩm không quá 100 ký tự")
    private String description;

    @NotNull(message = "Giá tiền không được để trống")
    @PositiveOrZero(message = "Giá tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "Tồn kho không được để trống")
    @Positive(message = "Tồn kho phải lớn hơn 0")
    private Integer stock;

    @NotNull(message = "Trạng thái sản phẩm không được để trống")
    private ProductStatus status;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;
}
