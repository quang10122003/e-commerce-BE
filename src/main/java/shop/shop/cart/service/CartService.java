package shop.shop.cart.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.shop.cart.dto.request.AddCartItemRequest;
import shop.shop.cart.dto.request.CheckoutCartItemRequest;
import shop.shop.cart.dto.request.CheckoutCartTotalRequest;
import shop.shop.cart.dto.response.CartItemResponse;
import shop.shop.cart.dto.response.CartResponse;
import shop.shop.cart.dto.response.CheckoutCartResponse;
import shop.shop.cart.dto.response.CheckoutCartTotalResponse;
import shop.shop.cart.entity.Cart;
import shop.shop.cart.entity.CartLineItem;
import shop.shop.cart.mapper.CartMapper;
import shop.shop.cart.repository.CartLineItemRepository;
import shop.shop.cart.repository.CartRepository;
import shop.shop.common.cache.CacheKeys;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserClass;
import shop.shop.integration.redis.service.CartCacheService;
import shop.shop.product.entity.Product;
import shop.shop.common.ProductStatus;
import shop.shop.product.repository.ProductRepository;
import shop.shop.user.entity.User;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {

    CartLineItemRepository cartLineItemRepository;
    CartRepository cartRepository;
    ProductRepository productRepository;
    CurrentUserClass currentUserClass;
    CartMapper cartMapper;
    CartCacheService cartCacheService;

    @Transactional(readOnly = true)
    public CartResponse getCurrentUserCart() {
        User currentUser = currentUserClass.getCurrentUser();
        String cacheKey = CacheKeys.cartByUser(currentUser.getId());

        // Lấy giỏ hàng từ cache Redis nếu dữ liệu đã tồn tại.
        CartResponse cachedCart = cartCacheService.getPayload(cacheKey, CartResponse.class);
        if (cachedCart != null) {
            return cachedCart;
        }

        CartResponse cartResponse = buildCartResponseFromDb(currentUser.getId());
        cartCacheService.set(cacheKey, cartResponse, Duration.ofDays(7));

        return cartResponse;
    }

    // Thêm sản phẩm vào giỏ hàng.
    @Transactional
    public CartResponse addToCurrentUserCart(AddCartItemRequest request) {
        validateAddToCartRequest(request);

        User currentUser = currentUserClass.getCurrentUser();
        Cart cart = cartRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> createCart(currentUser));
        Product product = productRepository.findById(request.productId())
                .filter(item -> item.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        CartLineItem cartLineItem = cartLineItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> createCartLineItem(cart, product));

        int newQuantity = resolveNewQuantity(cartLineItem, request);

        cartLineItem.setQuantity(newQuantity);
        cartLineItemRepository.save(cartLineItem);

        CartResponse cartResponse = buildCartResponseFromDb(currentUser.getId());
        cartCacheService.registerCartCacheUpdateAfterCommit(currentUser.getId(), cartResponse);

        return cartResponse;
    }

    // Xóa sản phẩm khỏi giỏ hàng của user hiện tại.
    @Transactional
    public CartResponse removeFromCurrentUserCart(Long productId) {
        validateProductId(productId);

        User currentUser = currentUserClass.getCurrentUser();
        CartLineItem cartLineItem = cartLineItemRepository.findByCart_User_IdAndProduct_Id(
                currentUser.getId(),
                productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        cartLineItemRepository.delete(cartLineItem);

        CartResponse cartResponse = buildCartResponseFromDb(currentUser.getId());
        cartCacheService.registerCartCacheUpdateAfterCommit(currentUser.getId(), cartResponse);

        return cartResponse;
    }

    // Tính tổng tiền cần thanh toán theo danh sách sản phẩm và số lượng.
    @Transactional(readOnly = true)
    public CheckoutCartTotalResponse calculateCheckoutTotal(CheckoutCartTotalRequest request) {
        validateCheckoutTotalRequest(request);

        // Gom số lượng theo productId để tránh tính trùng dòng sản phẩm.
        Map<Long, Integer> quantityByProductId = request.items().stream()
                .collect(Collectors.toMap(
                        CheckoutCartItemRequest::productId,
                        CheckoutCartItemRequest::quantity,
                        Integer::sum));

        List<Product> products = productRepository.findActiveByIds(quantityByProductId.keySet().stream().toList());

        validateCheckoutProducts(quantityByProductId.keySet(), products);
        // Nhân giá hiện tại trong DB với số lượng user gửi lên.
        BigDecimal totalAmount = products.stream()
                .map(product -> product.getPrice()
                        .multiply(BigDecimal.valueOf(quantityByProductId.get(product.getId()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutCartTotalResponse(totalAmount);
    }

    // Lấy thông tin sản phẩm checkout theo productId và số lượng user đã chọn.
    @Transactional(readOnly = true)
    public CheckoutCartResponse getCheckoutPreview(CheckoutCartTotalRequest request) {
        validateCheckoutTotalRequest(request);

        // Gom số lượng theo productId để tránh hiển thị trùng sản phẩm.
        Map<Long, Integer> quantityByProductId = request.items().stream()
                .collect(Collectors.toMap(
                        CheckoutCartItemRequest::productId,
                        CheckoutCartItemRequest::quantity,
                        Integer::sum));

        List<Product> products = productRepository.findActiveByIds(quantityByProductId.keySet().stream().toList());

        validateCheckoutProducts(quantityByProductId.keySet(), products);

        List<CartItemResponse> items = products.stream()
                .map(product -> buildCheckoutItemResponse(product, quantityByProductId.get(product.getId())))
                .toList();

        int totalQuantity = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutCartResponse(items, totalQuantity, totalAmount);
    }

    // Tạo dữ liệu giỏ hàng mới nhất từ DB theo userId.
    private CartResponse buildCartResponseFromDb(Long userId) {
        List<CartLineItem> cartItems = cartLineItemRepository.findByUserId(userId);

        List<CartItemResponse> items = cartMapper.toResponseList(cartItems);

        int totalQuantity = cartItems.stream()
                .mapToInt(CartLineItem::getQuantity)
                .sum();

        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(items, totalQuantity, totalAmount);
    }


    // Tạo một dòng sản phẩm checkout từ dữ liệu sản phẩm hiện tại trong DB.
    private CartItemResponse buildCheckoutItemResponse(Product product, Integer quantity) {
        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(quantity));

        return new CartItemResponse(
                product.getId(),
                product.getName(),
                product.getThumbnail(),
                product.getPrice(),
                product.getStock(),
                quantity,
                totalPrice);
    }

    private void validateAddToCartRequest(AddCartItemRequest request) {
        if (request == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Request khong hop le");
        }
        if (request.productId() == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Request khong hop le");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Request khong hop le");
        }
    }

    // Xử lý số lượng sản phẩm.
    private int resolveNewQuantity(CartLineItem cartLineItem, AddCartItemRequest request) {
        return cartLineItem.getQuantity() + request.quantity();
    }

    // Kiểm tra productId dùng cho thao tác xóa khỏi giỏ hàng.
    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Request khong hop le");
        }
    }

    // Kiểm tra danh sách sản phẩm cần tính tổng tiền thanh toán.
    private void validateCheckoutTotalRequest(CheckoutCartTotalRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Request khong hop le");
        }

        for (CheckoutCartItemRequest item : request.items()) {
            if (item == null || item.productId() == null || item.quantity() == null || item.quantity() <= 0) {
                throw new ApiError(ErrorCode.BAD_REQUEST, "Request khong hop le");
            }
        }
    }

    // Đảm bảo mọi productId gửi lên đều tồn tại và đang bán.
    private void validateCheckoutProducts(Set<Long> requestedProductIds, List<Product> products) {
        Map<Long, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (!productById.keySet().containsAll(requestedProductIds)) {
            throw new ApiError(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    // Tạo giỏ hàng.
    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    // Tạo sản phẩm trong giỏ hàng.
    private CartLineItem createCartLineItem(Cart cart, Product product) {
        CartLineItem cartLineItem = new CartLineItem();
        cartLineItem.setCart(cart);
        cartLineItem.setProduct(product);
        cartLineItem.setQuantity(0);
        return cartLineItem;
    }
    
   
}
