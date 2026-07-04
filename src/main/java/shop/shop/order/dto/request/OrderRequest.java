package shop.shop.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(regexp = "COD|SEPAY", message = "Phương thức thanh toán phải là COD hoặc SEPAY")
    private String paymentMethod;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<Item> items;

    @NotNull(message = "Thông tin vận chuyển không được để trống")
    @Valid
    private ShippingAddress shippingAddress;

    @Data
    public static class Item {

        @NotNull(message = "ProductId không được để trống")
        @Positive(message = "ProductId phải lớn hơn 0")
        private Long productId;

        @NotNull(message = "Quantity không được để trống")
        @Positive(message = "Quantity phải lớn hơn 0")
        private Integer quantity;
    }

    @Data
    public static class ShippingAddress {

        @NotBlank(message = "Tên người nhận không được để trống")
        @Size(min = 2, max = 255, message = "Tên người nhận phải từ 2 đến 255 ký tự")
        private String fullName;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(min = 9, max = 50, message = "Số điện thoại phải từ 9 đến 50 ký tự")
        private String phone;

        @NotBlank(message = "Địa chỉ không được để trống")
        @Size(min = 5, max = 500, message = "Địa chỉ phải từ 5 đến 500 ký tự")
        private String address;

    }
}