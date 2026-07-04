package shop.shop.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.ProductStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductSummaryResponse {
    private Long id;
    private String name;
    // Trả version hiện tại để admin dashboard gửi lại khi cập nhật, giúp phát hiện dữ liệu sửa đã cũ.
    private Long version;
    private Integer purchases;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
    private Long categoryId;
    private String categoryName;
    private String thumbnail;
    private List<AdminProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
