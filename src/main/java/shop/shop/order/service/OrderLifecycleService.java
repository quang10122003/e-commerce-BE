// class chỉ lo vòng đời đơn hàng — xem đơn của user, hủy đơn, admin đổi trạng thái. Việc xử lý payment  khi hủy/hoàn tất được ủy quyền OrderPaymentPolicyService 
package shop.shop.order.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.admin.dto.response.AdminOrderItemRepone;
import shop.shop.admin.dto.response.AdminOrdersRepone;
import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.integration.redis.service.CacheInvalidationService;
import shop.shop.order.dto.response.OrderResponse;
import shop.shop.order.entity.Order;
import shop.shop.order.entity.OrderItem;
import shop.shop.order.mapper.OrderMapper;
import shop.shop.order.policy.OrderStatusTransitionPolicyRegistry;
import shop.shop.order.policy.interfaces.IOrderStatusTransitionPolicy;
import shop.shop.order.repo.OrderRepository;
import shop.shop.product.repository.ProductRepository;
import shop.shop.user.entity.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderLifecycleService {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    CurrentUserProvider currentUserProvider;
    OrderRepository orderRepository;
    ProductRepository productRepository;
    OrderMapper orderMapper;
    CacheInvalidationService cacheInvalidationService;
    OrderPaymentPolicyService orderPaymentPolicyService;
    OrderStatusTransitionPolicyRegistry orderStatusTransitionPolicyRegistry;

    @Transactional(readOnly = true)
    // xem đơn hàng của user 
    public List<OrderResponse> getCurrentUserOrders() {
        User currentUser = currentUserProvider.getCurrentUser();
        List<Order> orders = orderRepository.findAllByUserIdWithItems(currentUser.getId());
        return orderMapper.toResponseList(orders);
    }

    // Hủy đơn hàng của user hiện tại nếu đơn còn ở trạng thái cho phép.
    @Transactional
    public OrderResponse cancelCurrentUserOrder(Long orderId) {
        User currentUser = currentUserProvider.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiError(ErrorCode.ORDER_NOT_FOUND));

        validateCurrentUserOwnsOrder(order, currentUser);
        validateUserCancelStatus(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(CancelledBy.USER);
        // goi orderPaymentPolicyService để xử lý role  payment khi hủy order
        orderPaymentPolicyService.handlePaymentWhenCancelOrder(order, CancelledBy.USER);
        restoreStockWhenCancelOrder(order);

        List<Long> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .toList();
        // xóa cache reddis
        cacheInvalidationService.productsChanged(productIds);

        logger.info("user:{} hủy đơn hàng:{} với order code:{}", currentUser.getId(), orderId, order.getOrderCode());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // admin lấy order
    @Transactional(readOnly = true)
    public ApiResponse<AdminOrdersRepone> getAdminOrders(String search, String status, LocalDate from,
            LocalDate to) {
        String normalizedSearch = normalize(search);
        OrderStatus normalizedStatus = normalizeOrderStatus(status);

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : null;

        List<AdminOrderItemRepone> items = orderMapper.toAdminOrderItemList(
                orderRepository.findAdminOrders(normalizedSearch, normalizedStatus, fromDt, toDt));

        long completedLast7Days = orderRepository.countByStatusAndCreatedAtGreaterThanEqual(
                OrderStatus.COMPLETED, LocalDateTime.now().minusDays(7));
        long cancelledLast7Days = orderRepository.countByStatusAndCreatedAtGreaterThanEqual(
                OrderStatus.CANCELLED, LocalDateTime.now().minusDays(7));

        return ApiResponse.success("lấy orders thành công", AdminOrdersRepone.builder()
                .total(orderRepository.count())
                .today(orderRepository.countTodayOrderCount())
                .pending(orderRepository.countByStatus(OrderStatus.PENDING))
                .shipping(orderRepository.countByStatus(OrderStatus.SHIPPING))
                .completed(orderRepository.countByStatus(OrderStatus.COMPLETED))
                .cancelled(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .deliverySuccessRate(calculateDeliverySuccessRate(completedLast7Days, cancelledLast7Days))
                .item(items)
                .build());
    }

    // Cập nhật trạng thái đơn hàng theo đúng luồng xử lý của admin.
    @Transactional
    public ApiResponse<AdminOrderItemRepone> updateAdminOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiError(ErrorCode.ORDER_NOT_FOUND));
        OrderStatus targetStatus = normalizeRequiredOrderStatus(status);

        validateAdminStatusTransition(order, targetStatus);
        // goi orderPaymentPolicyService để xử lý role  payment khi chuyển đổi trang thái order
        orderPaymentPolicyService.validatePaymentBeforeChangeStatusOrder(order, targetStatus);

        order.setStatus(targetStatus);

        if (targetStatus == OrderStatus.CANCELLED) {
            List<Long> productIds = order.getItems().stream()
                    .map(OrderItem::getProductId)
                    .toList();
            order.setCancelledBy(CancelledBy.ADMIN);
            // goi orderPaymentPolicyService để xử lý role payment khi hủy order
            orderPaymentPolicyService.handlePaymentWhenCancelOrder(order, CancelledBy.ADMIN);
            // hoàn trả stock
            restoreStockWhenCancelOrder(order);

            cacheInvalidationService.productsChanged(productIds);
        }
        logger.info("admin:{} chuyển trạng thái đơn hàng:{} với order code:{} về {}",
                currentUserProvider.getCurrentUser().getId(), orderId, order.getOrderCode(), status);
        return ApiResponse.success("cập nhật trạng thái đơn hàng thành công",
                orderMapper.toAdminOrderItem(orderRepository.save(order)));
    }

    private OrderStatus normalizeRequiredOrderStatus(String status) {
        String normalizedStatus = normalize(status);

        if (normalizedStatus == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }

        try {
            return OrderStatus.valueOf(normalizedStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Trạng thái đơn hàng không hợp lệ");
        }
    }

    // Kiểm tra trạng  thái order đích có hợp lệ theo trạng thái hiện tại và phương thức thanh toán.
    private void validateAdminStatusTransition(Order order, OrderStatus targetStatus) {
        IOrderStatusTransitionPolicy policy = orderStatusTransitionPolicyRegistry.getPolicy(order.getStatus());

        if (!policy.canTransitionTo(order, targetStatus)) {
            throw new ApiError(ErrorCode.ILLEGAL_STATE, "Không thể đổi trạng thái đơn hàng");
        }
    }


    // kiểm tra xem order có phải của user hay k
    private void validateCurrentUserOwnsOrder(Order order, User currentUser) {
        if (order.getUser() == null || !order.getUser().getId().equals(currentUser.getId())) {
            throw new ApiError(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    // kiếm tra trang thái đơn hàng có hợp lệ cho việc hủy đơn hàng hay k
    private void validateUserCancelStatus(Order order) {
        IOrderStatusTransitionPolicy policy = orderStatusTransitionPolicyRegistry.getPolicy(order.getStatus());

        if (!policy.canTransitionTo(order, OrderStatus.CANCELLED)) {
            throw new ApiError(
                    ErrorCode.ILLEGAL_STATE,
                    "Không thể hủy đơn hàng ở trạng thái hiện tại");
        }
    }

    //  hoàn trả stock của sản phẩm khi hủy đơn
    private void restoreStockWhenCancelOrder(Order order) {
        productRepository.restoreStockByOrderId(order.getId());
    }

    // chuẩn hóa string
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chuyển đổi chuỗi status sang OrderStatus và kiểm tra giá trị hợp lệ
    private OrderStatus normalizeOrderStatus(String status) {
        String normalizedStatus = normalize(status);

        if (normalizedStatus == null) {
            return null;
        }

        try {
            return OrderStatus.valueOf(normalizedStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
    }

    // tính % tỉ lệ đơn hàng thành công
    private BigDecimal calculateDeliverySuccessRate(long completed, long cancelled) {
        long totalClosedOrders = completed + cancelled;

        if (totalClosedOrders == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalClosedOrders), 1, RoundingMode.HALF_UP);
    }
}
