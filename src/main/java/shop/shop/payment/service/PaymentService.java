package shop.shop.payment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.admin.Projection.PaymentStatsProjection;
import shop.shop.admin.dto.response.AdminPayementItemRepone;
import shop.shop.admin.dto.response.AdminPaymentsRepone;
import shop.shop.admin.dto.response.AdminProductSummaryResponse;
import shop.shop.admin.mapper.AdminPaymentMapper;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.order.entity.Order;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.DTO.repone.QrRepone;
import shop.shop.payment.DTO.request.QrRquest;
import shop.shop.payment.DTO.request.SePayWebhookRequest;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {

    OrderRepository orderRepository;
    PaymentRepo paymentRepo;
    UserRepo userRepo;
    SimpMessagingTemplate messagingTemplate;

    // cấu tk nhận tiền ( hiện tại đang dùng sepay test)
    static String SEPAY_QR_URL_TEMPLATE = "https://qr.sepay.vn/img?bank=%s&acc=%s&template=compact&amount=%s&des=%s";
    static String BANK = "MSB";
    static String ACC_BANK = "SBSEPAY4L58YTRESWZP";

    AdminPaymentMapper adminPaymentMapper;

    public QrRepone getQr(QrRquest request) {

        User user = getCurrentUser();

        Order order = orderRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new ApiError(ErrorCode.ORDER_NOT_FOUND));

        PaymentEntity payment = paymentRepo.findByTransactionRef(request.getOrderCode())
                .orElseThrow(() -> new ApiError(ErrorCode.PAYMENT_NOT_FOUND));

        validateOwnership(user, payment);

        validatePaymentMethod(order);

        validateQrAllowed(payment, order);

        String qrUrl = buildQrcode(order);

        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(5);

        return QrRepone.builder()
                .url(qrUrl)
                .expiredAt(
                        expiredAt)
                .build();
    }

    // kiểm trả xem payment này có phải của user k để tiến hành thanh toán
    private void validateOwnership(User user, PaymentEntity payment) {
        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }
    }

    // kiếm tra xem đơn hàng này có phải thanh toán sepay k
    private void validatePaymentMethod(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.SEPAY) {
            throw new ApiError(ErrorCode.PAYMENT_NOT_SUPPORTED);
        }
    }

    private void validateQrAllowed(PaymentEntity payment, Order order) {

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ApiError(ErrorCode.PAYMENT_NO_LONGER_AVAILABLE);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiError(ErrorCode.PAYMENT_NO_LONGER_AVAILABLE);
        }

        if (payment.getExpiredAt() != null &&
                payment.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ApiError(ErrorCode.PAYMENT_EXPIRED);
        }
    }

    // ===================== QR BUILD =====================

    private String buildQrcode(Order order) {

        return String.format(
                SEPAY_QR_URL_TEMPLATE,
                BANK,
                ACC_BANK,
                order.getTotalAmount().toPlainString(),
                order.getOrderCode());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        return userRepo.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Boolean> webhook(SePayWebhookRequest payload) {
        String transactionRef = payload.getCode();
        String referenceCode = payload.getReferenceCode();
        BigDecimal amount = payload.getTransferAmount();

        log.info("Nhận webhook thanh toán: {}", payload);

        Optional<PaymentEntity> paymentOpt = paymentRepo.findByTransactionRef(transactionRef);
        if (paymentOpt.isEmpty()) {
            log.warn("Khách hàng chuyển khoản nhưng không tìm thấy giao dịch: {}", transactionRef);
            return Map.of("success", true);
        }

        PaymentEntity payment = paymentOpt.get();

        // banking bắn webhook nhiều lần
        if (isDuplicateWebhook(payment, referenceCode)) {
            log.warn("Giao dịch trùng lặp (webhook bắn nhiều lần): {}", referenceCode);
            return Map.of("success", true);
        }

        // khách hàng quét QR thanh toán nhiều lần
        if (isAlreadyPaid(payment)) {
            log.warn("Đơn hàng đã được thanh toán trước đó, khách chuyển khoản nhiều lần: {}", transactionRef);
            notify(transactionRef, "DUPLICATE_PAYMENT", "Thanh toán nhiều lần");
            return Map.of("success", true);
        }

        LocalDateTime transactionDate = payload.getTransactionDate() != null
                ? payload.getTransactionDate()
                : LocalDateTime.now();

        boolean isLate = payment.getExpiredAt() != null
                && !transactionDate.isBefore(payment.getExpiredAt());

        try {
            if (isLate) {
                handleLatePayment(payment, referenceCode, transactionDate, transactionRef);
            } else {
                handleOnTimePayment(payment, referenceCode, transactionDate, transactionRef, amount);
            }
        } catch (Exception ex) {
            log.error(
                    "Lỗi khi xử lý webhook thanh toán, rollback giao dịch: transactionRef={}, referenceCode={}, error={}",
                    transactionRef, referenceCode, ex.getMessage(), ex);
            throw ex; // để @Transactional rollback
        }

        return Map.of("success", true);
    }

    private boolean isDuplicateWebhook(PaymentEntity payment, String referenceCode) {
        return referenceCode.equals(payment.getReferenceCode());
    }

    private boolean isAlreadyPaid(PaymentEntity payment) {
        return payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.PAID_LATE;
    }

    private void handleLatePayment(PaymentEntity payment, String referenceCode,
            LocalDateTime transactionDate, String transactionRef) {
        log.warn("Đơn hàng đã hết hạn, khách chuyển khoản muộn: {}", transactionRef);

        payment.setPaidAt(transactionDate);
        payment.setReferenceCode(referenceCode);
        payment.setStatus(PaymentStatus.PAID_LATE);
        paymentRepo.save(payment);

        notify(transactionRef, "PAID_LATE", "Thanh toán muộn, đơn hàng sẽ bị hủy");
    }

    private void handleOnTimePayment(PaymentEntity payment, String referenceCode, LocalDateTime transactionDate,
            String transactionRef, BigDecimal amount) {
        if (amount.compareTo(payment.getOrder().getTotalAmount()) != 0) {
            log.warn("Khách chuyển khoản sai số tiền: expected={}, received={}",
                    payment.getOrder().getTotalAmount(), amount);
            notify(transactionRef, "AMOUNT_MISMATCH", "Số tiền không khớp");
            return;
        }

        payment.setPaidAt(transactionDate);
        payment.setReferenceCode(referenceCode);
        payment.setStatus(PaymentStatus.PAID);
        paymentRepo.save(payment);

        payment.getOrder().setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(payment.getOrder());

        log.info("Thanh toán thành công: {}", transactionRef);
        notify(transactionRef, "SUCCESS", "Thanh toán thành công!");
    }

    // bắn sự kiện về websocket cho fonent
    private void notify(String orderCode, String status, String message) {
        try {
            Map<String, String> payload = Map.of(
                    "status", status,
                    "orderCode", orderCode,
                    "message", message);
            messagingTemplate.convertAndSend("/topic/payment/" + orderCode, payload);
        } catch (Exception ex) {
            log.error("Gửi thông báo socket thất bại: orderCode={}, status={}, message={}, error={}",
                    orderCode, status, message, ex.getMessage(), ex);
        }
    }

    public ApiResponse<AdminPaymentsRepone> getPayment(String search, String status, LocalDate from,
            LocalDate to) {
        String normalizedSearch = normalize(search);
        PaymentStatus normalizedStatus = normalizePaymentStatus(status);

        // Convert LocalDate → LocalDateTime để so sánh với createdAt (timestamp)
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : null;

        List<AdminPayementItemRepone> result = paymentRepo
                .findAdminPayments(normalizedSearch, normalizedStatus, fromDt, toDt)
                .stream()
                .map(adminPaymentMapper::toAdminPaymentsRepone)
                .toList();

        PaymentStatsProjection statuss = paymentRepo.getStats();

        return ApiResponse.success("lấy payments thành công", AdminPaymentsRepone.builder().item(result).total(
                statuss.getTotal()).paid(
                        statuss.getPaid())
                .pending(statuss.getPending()).paidLate(
                        statuss.getPaidLate())
                .failed(statuss.getFailed()).build());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PaymentStatus normalizePaymentStatus(String status) {
        String normalizedStatus = normalize(status);

        if (normalizedStatus == null) {
            return null;
        }

        try {
            return PaymentStatus.valueOf(normalizedStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
    }
}
