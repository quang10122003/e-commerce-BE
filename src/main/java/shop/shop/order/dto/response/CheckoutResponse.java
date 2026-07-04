package shop.shop.order.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutResponse {
    private String orderCode;
    private String paymentMethod;
    private String status;

}