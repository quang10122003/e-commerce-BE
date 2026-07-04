package shop.shop.admin.dto.request;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import shop.shop.common.ProductStatus;

@Data
public class AdminUpdateProductRequest {
    @NotNull(message = "Version sản phẩm không được để trống")
    private Long version;

    @Size(min = 2, max = 50, message = "Tên sản phẩm phải từ 2 đến 50 ký tự")
    private String name;

    @Size(max = 100, message = "Mô tả sản phẩm không quá 100 ký tự")
    private String description;

    @PositiveOrZero(message = "Giá tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Positive(message = "Tồn kho phải lớn hơn 0")
    private Integer stock;

    private ProductStatus status;

    private Long categoryId;
    private List<String> deleteImageUrls;
    private List<MultipartFile> images;
    private MultipartFile thumbnail;
}
