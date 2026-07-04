// Tep CartController.java.
package shop.shop.cart.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.shop.cart.dto.request.AddCartItemRequest;
import shop.shop.cart.dto.request.CheckoutCartTotalRequest;
import shop.shop.cart.dto.response.CheckoutCartResponse;
import shop.shop.cart.dto.response.CheckoutCartTotalResponse;
import shop.shop.cart.dto.response.CartResponse;
import shop.shop.cart.service.CartService;
import shop.shop.common.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CartResponse>> getCurrentUserCart() {
        return ResponseEntity.status(200)
                .body(ApiResponse.success("Cart fetched", cartService.getCurrentUserCart()));
    }
    @PostMapping("add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCurrentUserCart(@Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.status(200)
                .body(ApiResponse.success("Them san pham vao gio hang thanh cong",
                        cartService.addToCurrentUserCart(request)));
    }

    // Xóa một sản phẩm khỏi giỏ hàng của user hiện tại.
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCurrentUserCart(@PathVariable Long productId) {
        return ResponseEntity.status(200)
                .body(ApiResponse.success("Xoa san pham khoi gio hang thanh cong",
                        cartService.removeFromCurrentUserCart(productId)));
    }

    // Tính tổng tiền thanh toán theo danh sách sản phẩm user gửi lên.
    @PostMapping("/checkout-total")
    public ResponseEntity<ApiResponse<CheckoutCartTotalResponse>> calculateCheckoutTotal(
            @Valid @RequestBody CheckoutCartTotalRequest request) {
        return ResponseEntity.status(200)
                .body(ApiResponse.success("Tinh tong tien thanh toan thanh cong",
                        cartService.calculateCheckoutTotal(request)));
    }

    // Lấy thông tin sản phẩm và tổng tiền cho trang checkout theo danh sách user đã chọn.
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutCartResponse>> getCheckoutPreview(
            @Valid @RequestBody CheckoutCartTotalRequest request) {
        return ResponseEntity.status(200)
                .body(ApiResponse.success("Lay thong tin checkout thanh cong",
                        cartService.getCheckoutPreview(request)));
    }
}
