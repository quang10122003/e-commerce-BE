package shop.shop.admin.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateCategoriRequest {
    @Size(min = 2, max = 120, message = "Tên danh mục phải từ 2 đến 120 ký tự")
    String name;
}
