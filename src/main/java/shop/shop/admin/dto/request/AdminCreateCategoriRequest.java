package shop.shop.admin.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateCategoriRequest {
    @NotBlank(message = "trường name ko đc trống or khoảng trắng")
    @Size(min = 2,message = "ít nhất có 2 khi tự trong tên danh mục")
    String name;
}
