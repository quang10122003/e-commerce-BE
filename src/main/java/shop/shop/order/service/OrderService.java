package shop.shop.order.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.admin.Projection.DailyRevenueProjection;
import shop.shop.admin.dto.response.AdminComparisonSeries;
import shop.shop.admin.dto.response.AdminOrderItemRepone;
import shop.shop.admin.dto.response.AdminOrdersRepone;
import shop.shop.admin.dto.response.AdminRevenueIn7day;
import shop.shop.admin.dto.response.AdminRevenueKpi;
import shop.shop.admin.dto.response.AdminRevenueRepone;
import shop.shop.admin.dto.response.AdminTrendSeries;
import shop.shop.cart.repository.CartLineItemRepository;
import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;
import shop.shop.common.PeriodRange;
import shop.shop.common.PeriodType;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserClass;
import shop.shop.integration.RabbitMQ.RabbitProducer;
import shop.shop.order.dto.request.OrderRequest;
import shop.shop.order.dto.response.CheckoutResponse;
import shop.shop.order.dto.response.OrderResponse;
import shop.shop.order.entity.Order;
import shop.shop.order.entity.OrderItem;
import shop.shop.order.mapper.OrderMapper;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;
import shop.shop.product.entity.Product;
import shop.shop.product.repository.ProductRepository;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    CurrentUserClass currentUserClass;
    OrderRepository orderRepository;
    UserRepo userRepo;
    ProductRepository productRepository;
    CartLineItemRepository cartLineItemRepository;
    PaymentRepo paymentRepo;
    OrderMapper orderMapper;
    RabbitProducer rabbitProducer;
    static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static DateTimeFormatter DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("dd/MM");
    static SecureRandom RANDOM = new SecureRandom();

    @NonFinal
    @Value("${app.rabbitMq.order-sepay-delay-ttl-ms}")
    int expired;

    // Lấy order của user.
    @Transactional(readOnly = true)
    public List<OrderResponse> getCurrentUserOrders() {
        User currentUser = getCurrentUser();
        List<Order> orders = orderRepository.findAllByUserIdWithItems(currentUser.getId());

        // MapStruct tự map field + tự gắn expiredAt qua @AfterMapping.
        return orderMapper.toResponseList(orders);
    }

    // Lấy danh sách đơn hàng cho admin theo bộ lọc.
    @Transactional(readOnly = true)
    public ApiResponse<AdminOrdersRepone> getAdminOrders(String search, String status, LocalDate from,
            LocalDate to) {
        String normalizedSearch = normalize(search);
        OrderStatus normalizedStatus = normalizeOrderStatus(status);

        // Chuyển LocalDate sang LocalDateTime để lọc theo createdAt.
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : null;

        List<AdminOrderItemRepone> items = orderMapper.toAdminOrderItemList(
                orderRepository.findAdminOrders(normalizedSearch, normalizedStatus, fromDt, toDt));

        long completedLast7Days = orderRepository.countByStatusAndCreatedAtGreaterThanEqual(
                OrderStatus.COMPLETED,
                LocalDateTime.now().minusDays(7));
        long cancelledLast7Days = orderRepository.countByStatusAndCreatedAtGreaterThanEqual(
                OrderStatus.CANCELLED,
                LocalDateTime.now().minusDays(7));

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
        validatePaymentBeforeCompleteOrder(order, targetStatus);
        order.setStatus(targetStatus);

        if (targetStatus == OrderStatus.CANCELLED) {
            order.setCancelledBy(CancelledBy.ADMIN);
            handlePaymentWhenAdminCancelOrder(order);
        }
        logger.info("admin:{} chuyển trạng thái đơn hàng:{} với order code:{} về {}",currentUserClass.getCurrentUser().getId(),orderId,order.getOrderCode(),status);
        return ApiResponse.success("cập nhật trạng thái đơn hàng thành công",
                orderMapper.toAdminOrderItem(orderRepository.save(order)));
    }

    // Chuẩn hóa trạng thái bắt buộc từ path variable sang enum OrderStatus.
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

    // Kiểm tra luồng đổi trạng thái hợp lệ cho admin.
    private void validateAdminStatusTransition(Order order, OrderStatus targetStatus) {
        OrderStatus currentStatus = order.getStatus();

        boolean validTransition = switch (currentStatus) {
            case PENDING -> isValidPendingTransition(order, targetStatus);
            case CONFIRMED -> targetStatus == OrderStatus.SHIPPING || targetStatus == OrderStatus.CANCELLED;
            case SHIPPING -> targetStatus == OrderStatus.COMPLETED || targetStatus == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new ApiError(ErrorCode.ILLEGAL_STATE, "Không thể đổi trạng thái đơn hàng");
        }
    }

    // Đơn không phải COD (ví dụ SEPAY) sẽ tự động chuyển sang SHIPPING khi nhận
    // được thanh toán, nên admin ở trạng thái PENDING chỉ được phép hủy đơn.
    private boolean isValidPendingTransition(Order order, OrderStatus targetStatus) {
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            return targetStatus == OrderStatus.CANCELLED;
        }

        return targetStatus == OrderStatus.CONFIRMED || targetStatus == OrderStatus.CANCELLED;
    }

    // Kiểm tra đơn SEPAY đã thanh toán trước khi cho hoàn tất.
    private void validatePaymentBeforeCompleteOrder(Order order, OrderStatus targetStatus) {
        if (targetStatus != OrderStatus.COMPLETED || order.getPaymentMethod() != PaymentMethod.SEPAY) {
            return;
        }

        PaymentEntity payment = paymentRepo.findByOrderId(order.getId())
                .orElseThrow(() -> new ApiError(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiError(ErrorCode.ILLEGAL_STATE, "Đơn SEPAY chưa thanh toán thành công");
        }
    }

    // Xử lý payment SEPAY khi admin hủy đơn hàng.
    private void handlePaymentWhenAdminCancelOrder(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.SEPAY) {
            return;
        }

        PaymentEntity payment = paymentRepo.findByOrderId(order.getId()).orElse(null);

        if (payment == null) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            logger.info("admin {} hủy order {} sepay payement chưa thành toán nên chuyển payment:{} về FAILED",currentUserClass.getCurrentUser().getId(),order.getId(),payment.getId());
            return;
        }

        // Đơn đã thanh toán (PAID/PAID_LATE) nhưng bị admin hủy
        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.PAID_LATE) {
            logger.warn("admin {} hủy order {} sepay payement id:{} đã thanh toán check hoàn tiền thủ công");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chuẩn hóa status từ query param sang enum OrderStatus.
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

    // Tính tỷ lệ giao thành công trong 7 ngày gần nhất.
    private BigDecimal calculateDeliverySuccessRate(long completed, long cancelled) {
        long totalClosedOrders = completed + cancelled;

        if (totalClosedOrders == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalClosedOrders), 1, RoundingMode.HALF_UP);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        return userRepo.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
    }

    // Lấy doanh thu của tuần trước.
    public BigDecimal getLastWeekRevenue() {
        LocalDate today = LocalDate.now();

        // Ngày bắt đầu của tuần hiện tại.
        LocalDate startOfThisWeek = today.with(DayOfWeek.MONDAY);
        // Ngày bắt đầu của tuần trước.
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDate endOfLastWeek = startOfThisWeek;

        return orderRepository.getRevenueByDay(
                startOfLastWeek.atStartOfDay(),
                endOfLastWeek.atStartOfDay());
    }

    // Lấy doanh thu của tuần hiện tại.
    public BigDecimal getWeekRevenue() {
        LocalDate today = LocalDate.now();
        // Ngày bắt đầu tuần để tính doanh thu.
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(7);

        return orderRepository.getRevenueByDay(
                startOfWeek.atStartOfDay(),
                endOfWeek.atStartOfDay());
    }

    // So sánh doanh thu hai tuần.
    public BigDecimal calculateGrowth(BigDecimal weekRevenue, BigDecimal lastWeekRevenue) {
        if (weekRevenue == null)
            weekRevenue = BigDecimal.ZERO;
        if (lastWeekRevenue == null)
            lastWeekRevenue = BigDecimal.ZERO;

        // Case đặc biệt: tuần trước = 0.
        if (lastWeekRevenue.compareTo(BigDecimal.ZERO) == 0) {
            if (weekRevenue.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(100);
        }

        // Tính chênh lệch giữa tuần hiện tại và tuần trước.
        BigDecimal diff = weekRevenue.subtract(lastWeekRevenue);

        // Chia chênh lệch cho doanh thu tuần trước.
        BigDecimal ratio = diff.divide(lastWeekRevenue, 4, RoundingMode.HALF_UP);

        // Nhân với 100 để ra phần trăm.
        return ratio.multiply(BigDecimal.valueOf(100));
    }

    // Tính doanh thu từng ngày trong tuần.
    public List<AdminRevenueIn7day> getRevenueIn7Days() {
        LocalDate today = LocalDate.now();

        // Lấy thứ Hai đầu tuần.
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        Map<LocalDate, Long> dailyMap = getDailyRevenueMap(OrderStatus.COMPLETED,
                new PeriodRange(startOfWeek.atStartOfDay(), endOfWeek.atTime(23, 59, 59)));

        List<AdminRevenueIn7day> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDay = startOfWeek.plusDays(i);
            Long revenue = dailyMap.getOrDefault(currentDay, 0L);
            result.add(new AdminRevenueIn7day(BigDecimal.valueOf(revenue), currentDay.atStartOfDay()));
        }
        return result;
    }

    // Tạo đơn hàng với phương thức COD.
    @Transactional
    public CheckoutResponse createOrderByCod(OrderRequest request) {
        User currentUser = getCurrentUser();
        Order order = prepareOrder(currentUser, request);
        order.setPaymentMethod(PaymentMethod.COD);

        order = saveOrderWithStock(order);

        String orderCode = generateOrderCode(order.getId());
        order.setOrderCode(orderCode);
        Order orderdone = orderRepository.save(order);

        cartLineItemRepository.deleteByUserIdAndProductIds(
                currentUser.getId(),
                extractProductIds(request));

        logger.info("user:{} tạo order:{} method:{} ordercode:{}",currentUser.getId(), orderdone.getId(),request.getPaymentMethod(),orderCode);
        return CheckoutResponse.builder()
                .orderCode(orderCode)
                .paymentMethod(PaymentMethod.COD.name())
                .status(order.getStatus().name())
                .build();
    }

    // Tạo đơn hàng với phương thức Payment.
    @Transactional(noRollbackFor = AmqpConnectException.class)
    public CheckoutResponse createOrderByBank(OrderRequest request) {
        User currentUser = getCurrentUser();
        Order order = prepareOrder(currentUser, request);
        order.setPaymentMethod(PaymentMethod.SEPAY);

        order = saveOrderWithStock(order);

        String orderCode = generateOrderCode(order.getId());
        order.setOrderCode(orderCode);
        Order orderDone = orderRepository.save(order);

        LocalDateTime expiredAt = LocalDateTime.now().plus(Duration.ofMillis(expired));
        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .method(PaymentMethod.SEPAY)
                .transactionRef(orderCode)
                .status(PaymentStatus.PENDING)
                .expiredAt(expiredAt)
                .paidAt(null)
                .build();

        PaymentEntity paymeneDone = paymentRepo.save(payment);

        cartLineItemRepository.deleteByUserIdAndProductIds(
                currentUser.getId(),
                extractProductIds(request));
        // chuyển mess order id vào rabbitMQ để xử lý khi đơn hàng hết expiredAt
        try {
            rabbitProducer.sendOrderSepayCheckQueue(orderDone.getId().toString());
        } catch (AmqpException e) {
            logger.warn("rabbitMQ ko hoạt đồng order:{} ordercode:{} payment:{} payment của đơn hàng này sẽ ko tự động chuyển trạng thái khi hết time",orderDone.getId(),orderCode,paymeneDone.getId());
        }
        logger.info("order:{} dc tạo với paymene:{} ordercode:{} tạo bởi user:{}",orderDone.getId(),paymeneDone.getId(),orderCode,currentUser.getId());
        return CheckoutResponse.builder()
                .orderCode(orderCode)
                .paymentMethod(PaymentMethod.SEPAY.name())
                .status(order.getStatus().name())
                .build();
    }

    private List<Long> extractProductIds(OrderRequest request) {
        return request.getItems()
                .stream()
                .map(OrderRequest.Item::getProductId)
                .toList();
    }

    private Order prepareOrder(User currentUser, OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Đơn hàng phải có ít nhất một sản phẩm");
        }

        List<Long> productIds = extractProductIds(request);
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

    // sinh orderCode duy nhất dựa trên orderId.
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

            total = total.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        order.setTotalAmount(total);
        return order;
    }

    // lấy data cho trang thống kê doanh thu
    @Transactional(readOnly = true)
    public AdminRevenueRepone getRevenueData(PeriodType type, Integer year, Integer week, Integer month) {
        if (year == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Thiếu tham số year");
        }
        if (type == PeriodType.WEEK && week == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Thiếu tham số week");
        }
        if (type == PeriodType.MONTH && month == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Thiếu tham số month");
        }

        PeriodRange currentRange = getPeriodRange(type, year, week, month);
        PeriodRange previousRange = getPreviousPeriodRange(type, currentRange);

        AdminRevenueKpi totalRevenue = buildKpi(OrderStatus.COMPLETED, currentRange, previousRange);
        AdminRevenueKpi pendingRevenue = buildKpi(OrderStatus.PENDING, currentRange, previousRange);

        Map<String, AdminRevenueKpi> kpis = new HashMap<>();
        kpis.put("totalRevenue", totalRevenue);
        kpis.put("pending", pendingRevenue);

        Map<LocalDate, Long> currentDailyMap = getDailyRevenueMap(OrderStatus.COMPLETED, currentRange);
        Map<LocalDate, Long> previousDailyMap = getDailyRevenueMap(OrderStatus.COMPLETED, previousRange);

        List<LabeledPeriod> currentPeriods = groupByPeriod(currentDailyMap, currentRange, type);
        List<LabeledPeriod> previousPeriods = groupByPeriod(previousDailyMap, previousRange, type);

        List<AdminTrendSeries> trendSeries = currentPeriods.stream()
                .map(p -> new AdminTrendSeries(p.label(), p.total()))
                .toList();

        List<AdminComparisonSeries> comparisonSeries = new ArrayList<>();
        for (int i = 0; i < currentPeriods.size(); i++) {
            long previousTotal = i < previousPeriods.size() ? previousPeriods.get(i).total() : 0L;
            comparisonSeries.add(new AdminComparisonSeries(
                    currentPeriods.get(i).label(),
                    currentPeriods.get(i).total(),
                    previousTotal));
        }

        return AdminRevenueRepone.builder()
                .kpis(kpis)
                .trendSeries(trendSeries)
                .comparisonSeries(comparisonSeries)
                .build();
    }

    // Xác định khoảng thời gian (start, end) dựa trên loại kỳ và tham số.
    private PeriodRange getPeriodRange(PeriodType type, int year, Integer week, Integer month) {
        LocalDate startDate, endDate;

        switch (type) {
            case WEEK -> {
                LocalDate firstDayOfYear = LocalDate.ofYearDay(year, 1);
                LocalDate monday = firstDayOfYear.with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                        .with(WeekFields.ISO.dayOfWeek(), 1);
                startDate = monday;
                endDate = monday.plusDays(6);
            }
            case MONTH -> {
                startDate = LocalDate.of(year, month, 1);
                endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            }
            default -> {
                startDate = LocalDate.of(year, 1, 1);
                endDate = LocalDate.of(year, 12, 31);
            }
        }
        return new PeriodRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    // Tính khoảng thời gian kỳ trước bằng cách lùi đi đúng số ngày của kỳ hiện tại.
    private PeriodRange getPreviousPeriodRange(PeriodType type, PeriodRange current) {
        LocalDateTime start = current.getStart();
        LocalDateTime end = current.getEnd();
        long days = java.time.Duration.between(start, end).toDays() + 1;
        LocalDateTime prevStart = start.minusDays(days);
        LocalDateTime prevEnd = end.minusDays(days);
        return new PeriodRange(prevStart, prevEnd);
    }

    // lấy data overview cho biểu đồ: giá trị hiện tại, % thay đổi so với kỳ trước.
    private AdminRevenueKpi buildKpi(OrderStatus status, PeriodRange current, PeriodRange previous) {
        BigDecimal currentValue = orderRepository.sumTotalAmountByStatusAndDateRange(status, current.getStart(),
                current.getEnd());
        BigDecimal previousValue = orderRepository.sumTotalAmountByStatusAndDateRange(status, previous.getStart(),
                previous.getEnd());

        if (currentValue == null)
            currentValue = BigDecimal.ZERO;
        if (previousValue == null)
            previousValue = BigDecimal.ZERO;

        Double deltaPct;
        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            deltaPct = currentValue.compareTo(BigDecimal.ZERO) > 0 ? null : 0.0;
        } else {
            deltaPct = currentValue.subtract(previousValue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousValue, 4, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return AdminRevenueKpi.builder()
                .value(currentValue)
                .deltaPct(deltaPct)
                .build();
    }

    private record LabeledPeriod(String label, long total) {
    }

    // Gộp doanh thu theo ngày/tuần/tháng tùy loại kỳ, dùng chung cho cả trend
    // và comparison series để tránh lặp logic.
    private List<LabeledPeriod> groupByPeriod(Map<LocalDate, Long> dailyMap, PeriodRange range, PeriodType type) {
        return switch (type) {
            case WEEK -> {
                List<LabeledPeriod> result = new ArrayList<>();
                LocalDate currentDate = range.getStart().toLocalDate();
                LocalDate endDate = range.getEnd().toLocalDate();
                while (!currentDate.isAfter(endDate)) {
                    String label = currentDate.format(DATE_FORMATTER);
                    result.add(new LabeledPeriod(label, dailyMap.getOrDefault(currentDate, 0L)));
                    currentDate = currentDate.plusDays(1);
                }
                yield result;
            }
            case MONTH -> {
                List<LabeledPeriod> result = new ArrayList<>();
                LocalDate start = range.getStart().toLocalDate();
                LocalDate end = range.getEnd().toLocalDate();
                LocalDate current = start;
                int weekNum = 1;
                while (!current.isAfter(end)) {
                    LocalDate weekEnd = current.plusDays(6);
                    if (weekEnd.isAfter(end))
                        weekEnd = end;
                    long weekTotal = 0;
                    for (LocalDate d = current; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                        weekTotal += dailyMap.getOrDefault(d, 0L);
                    }
                    String label = String.format("Tuần %d (%s - %s)", weekNum,
                            current.format(DATE_FORMATTER_SHORT), weekEnd.format(DATE_FORMATTER_SHORT));
                    result.add(new LabeledPeriod(label, weekTotal));
                    current = weekEnd.plusDays(1);
                    weekNum++;
                }
                // Gộp tuần thứ 5 (nếu có) vào tuần thứ 4.
                if (result.size() > 4) {
                    LabeledPeriod last = result.remove(result.size() - 1);
                    LabeledPeriod fourth = result.get(3);
                    result.set(3, new LabeledPeriod(fourth.label(), fourth.total() + last.total()));
                }
                yield result;
            }
            case YEAR -> {
                List<LabeledPeriod> result = new ArrayList<>();
                LocalDate start = range.getStart().toLocalDate();
                LocalDate end = range.getEnd().toLocalDate();
                LocalDate current = start;
                while (!current.isAfter(end)) {
                    LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
                    if (monthEnd.isAfter(end))
                        monthEnd = end;
                    long monthTotal = 0;
                    for (LocalDate d = current; !d.isAfter(monthEnd); d = d.plusDays(1)) {
                        monthTotal += dailyMap.getOrDefault(d, 0L);
                    }
                    String label = String.format("Tháng %d", current.getMonthValue());
                    result.add(new LabeledPeriod(label, monthTotal));
                    current = monthEnd.plusDays(1);
                }
                yield result;
            }
            default -> throw new ApiError(ErrorCode.INVALID_PERIOD_TYPE);
        };
    }

    // tạo map từ data DailyRevenueProjection trả về từ db để truy xuất nhanh.
    private Map<LocalDate, Long> getDailyRevenueMap(OrderStatus status, PeriodRange range) {
        List<DailyRevenueProjection> dailyData = orderRepository.getDailyRevenueByStatusAndDateRange(status,
                range.getStart(), range.getEnd());
        return dailyData.stream()
                .collect(Collectors.toMap(
                        DailyRevenueProjection::getDate,
                        DailyRevenueProjection::getTotal));
    }
}