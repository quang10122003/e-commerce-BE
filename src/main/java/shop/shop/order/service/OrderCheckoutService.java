package shop.shop.order.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.cart.repository.CartLineItemRepository;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserClass;
import shop.shop.integration.Resend.DTO.resquest.CreateOrderMailDTO;
import shop.shop.integration.Resend.service.EmailService;
import shop.shop.integration.redis.service.CatalogCacheService;
import shop.shop.order.dto.request.OrderRequest;
import shop.shop.order.dto.response.CheckoutResponse;
import shop.shop.order.entity.Order;
import shop.shop.order.entity.OrderItem;
import shop.shop.order.repo.OrderRepository;
import shop.shop.order.service.interfaces.IOrderPaymentHandler;
import shop.shop.product.entity.Product;
import shop.shop.product.repository.ProductRepository;
import shop.shop.user.entity.User;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// class này chỉ chịu trách nhiệm duy nhất: tạo order + giữ chỗ tồn kho.
// Phần logic riêng của từng phương thức thanh toán được đẩy ra IOrderPaymentHandler (áp dụng OCP).
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderCheckoutService {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    CurrentUserClass currentUserClass;
    OrderRepository orderRepository;
    ProductRepository productRepository;
    CartLineItemRepository cartLineItemRepository;
    CatalogCacheService catalogCacheService;
    List<IOrderPaymentHandler> paymentHandlers; 
    EmailService emailService;
    static SecureRandom RANDOM = new SecureRandom();

    // hàm tạo order duy nhất cho mọi phương thức thanh toán 
    @Transactional
    public CheckoutResponse createOrder(OrderRequest request) {
        PaymentMethod method = resolvePaymentMethod(request.getPaymentMethod());
        IOrderPaymentHandler IOrderPaymentHandler = findHandler(method);

        User currentUser = currentUserClass.getCurrentUser();
        Order order = prepareOrder(currentUser, request);
        order.setPaymentMethod(method);

        order = saveOrderWithStock(order);

        List<Long> productIds = extractProductIds(request);
        catalogCacheService.registerProductCacheDeleteAfterCommit(productIds);

        String orderCode = generateOrderCode(order.getId());
        order.setOrderCode(orderCode);
        Order orderDone = orderRepository.save(order);

        cartLineItemRepository.deleteByUserIdAndProductIds(currentUser.getId(), productIds);

        // phần riêng của từng phương thức thanh toán (VD: SEPAY tạo PaymentEntity + gửi
        // RabbitMQ)
        IOrderPaymentHandler.afterOrderSaved(orderDone, orderCode);

        logger.info("user:{} tạo order:{} method:{} ordercode:{}", currentUser.getId(), orderDone.getId(),
                method, orderCode);
        CreateOrderMailDTO orderMailDto = new CreateOrderMailDTO(orderDone.getUser().getEmail(),orderDone.getShippingName(),orderCode,orderDone.getShippingPhone(),orderDone.getPaymentMethod(),orderDone.getShippingAddress(),orderDone.getTotalAmount(),orderDone.getItems());
        emailService.sendOrderMail(orderMailDto);

        return CheckoutResponse.builder()
                .orderCode(orderCode)
                .paymentMethod(method.name())
                .status(order.getStatus().name())
                .build();
    }

    // chuyển chuỗi paymentMethod từ request thành enum, ném lỗi nếu client gửi giá
    // trị không hợp lệ
    private PaymentMethod resolvePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiError(ErrorCode.PAYMENT_METHOD_INVALID);
        }
    }

    // tìm đúng handler xử lý riêng cho phương thức thanh toán tương ứng
    private IOrderPaymentHandler findHandler(PaymentMethod method) {
        return paymentHandlers.stream()
                .filter(h -> h.getPaymentMethod() == method)
                .findFirst()
                .orElseThrow(() -> new ApiError(ErrorCode.PAYMENT_METHOD_INVALID));
    }

    // lấy id sản phẩm mua hàng đẩy vào 1 list
    private List<Long> extractProductIds(OrderRequest request) {
        return request.getItems().stream()
                .map(OrderRequest.Item::getProductId)
                .toList();
    }

    // validate request mua hàng và tiến hành build order
    private Order prepareOrder(User currentUser, OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Đơn hàng phải có ít nhất một sản phẩm");
        }

        List<Long> productIds = extractProductIds(request);
        // map id sản phẩm với sản phẩm
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return buildOrder(currentUser, request, productMap);
    }

    // Trừ tồn kho dựa trên optimistic lock của Product (@Version) để tránh
    // overselling khi nhiều request mua cùng sản phẩm đồng thời.
    private Order saveOrderWithStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getStock() < item.getQuantity()) {
                throw new ApiError(ErrorCode.INSUFFICIENT_STOCK);
            }

            product.setStock(product.getStock() - item.getQuantity());

            try {
                productRepository.save(product);
            } catch (OptimisticLockingFailureException ex) {
                throw new ApiError(ErrorCode.PRODUCT_VERSION_CONFLICT);
            }
        }

        return orderRepository.save(order);
    }

    // generate orderCode cho order (prefix "DH" + orderId + chuỗi random)
    private String generateOrderCode(Long orderId) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int length = 10;

        StringBuilder sb = new StringBuilder("DH");
        sb.append(orderId);

        int randomLength = Math.max(3, Math.min(30 - sb.length(), length));
        for (int i = 0; i < randomLength; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }

        return sb.toString();
    }

    // hàm base tạo order: build order entity + order items + tính tổng tiền
    private Order buildOrder(User currentUser, OrderRequest request, Map<Long, Product> productMap) {
        Order order = Order.builder()
                .user(currentUser)
                .status(OrderStatus.PENDING)
                .shippingName(request.getShippingAddress().getFullName())
                .shippingPhone(request.getShippingAddress().getPhone())
                .shippingAddress(request.getShippingAddress().getAddress())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.Item itemReq : request.getItems()) {
            Product product = productMap.get(itemReq.getProductId());

            if (product == null)
                throw new ApiError(ErrorCode.PRODUCT_NOT_FOUND);

            if (product.getStock() < itemReq.getQuantity())
                throw new ApiError(ErrorCode.INSUFFICIENT_STOCK);

            product.setPurchases(product.getPurchases() + itemReq.getQuantity());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())
                    .categoryName(product.getCategory().getName())
                    .price(product.getPrice())
                    .quantity(itemReq.getQuantity())
                    .thumbnail(product.getThumbnail())
                    .build();

            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        order.setTotalAmount(total);
        return order;
    }
}