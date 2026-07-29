package shop.shop.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import shop.shop.common.PaymentMethod;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.order.dto.request.OrderRequest;
import shop.shop.order.dto.response.CheckoutResponse;
import shop.shop.order.dto.response.OrderResponse;
import shop.shop.order.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getCurrentUserOrders() {
        return ResponseEntity.ok(
                ApiResponse.success("Lay danh sach don hang thanh cong",
                        orderService.getCurrentUserOrders()));
    }

    // Hủy đơn hàng của user đang đăng nhập khi đơn còn đủ điều kiện hủy.
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelCurrentUserOrder(
            @PathVariable(name = "orderId") Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success("Huy don hang thanh cong",
                        orderService.cancelCurrentUserOrder(orderId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        // Tạo đơn COD khi frontend gửi đúng phương thức thanh toán.
        if (PaymentMethod.COD.toString().equals(request.getPaymentMethod())) {
            return ResponseEntity.status(201).body(
                    ApiResponse.success("tạo order thành công", orderService.createOrderByCod(request)));
        }

        if (PaymentMethod.SEPAY.toString().equals(request.getPaymentMethod())) {
            return ResponseEntity.status(201).body(
                    ApiResponse.success("tạo order thành công", orderService.createOrderByBank(request)));
        }
        throw new ApiError(ErrorCode.PAYMENT_METHOD_INVALID);
    }
}
