package shop.shop.product.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import shop.shop.common.ProductStatus;

@Getter
@Setter
public class Productdetail {
    private Long id;
    private String name;
    private Integer purchases;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
    private String nameCategory;
    private String thumbnail;
    List<String> url; // Lay danh sach URL tu cac productImg cua san pham.
}
