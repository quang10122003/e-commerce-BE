package shop.shop.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import shop.shop.common.PaymentMethod;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.order.dto.request.OrderRequest;
import shop.shop.order.dto.response.CheckoutResponse;
import shop.shop.order.dto.response.OrderResponse;
import shop.shop.order.service.OrderCheckoutService;
import shop.shop.order.service.OrderLifecycleService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCheckoutService orderCheckoutService;
    private final OrderLifecycleService orderLifecycleService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getCurrentUserOrders() {
        return ResponseEntity.ok(
                ApiResponse.success("Lay danh sach don hang thanh cong",
                        orderLifecycleService.getCurrentUserOrders()));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelCurrentUserOrder(
            @PathVariable(name = "orderId") Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success("Huy don hang thanh cong",
                        orderLifecycleService.cancelCurrentUserOrder(orderId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(201).body(
                ApiResponse.success("tạo order thành công", orderCheckoutService.createOrder(request)));
    }
}