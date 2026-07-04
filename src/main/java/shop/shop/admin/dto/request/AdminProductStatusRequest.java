package shop.shop.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.ProductStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductStatusRequest {
    @NotNull(message = "Trạng thái sản phẩm không được để trống")
    private ProductStatus status;
}
