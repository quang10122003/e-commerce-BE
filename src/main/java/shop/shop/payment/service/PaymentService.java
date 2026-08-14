package shop.shop.payment.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.admin.Projection.PaymentStatsProjection;
import shop.shop.admin.dto.response.AdminPayementItemRepone;
import shop.shop.admin.dto.response.AdminPaymentsRepone;
import shop.shop.admin.mapper.AdminPaymentMapper;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PaymentStatus;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.common.until.ValidationUtils;
import shop.shop.config.SepayProperties;
import shop.shop.order.entity.Order;
import shop.shop.order.repo.OrderRepository;
import shop.shop.payment.DTO.repone.QrRepone;
import shop.shop.payment.DTO.request.QrRquest;
import shop.shop.payment.DTO.request.SePayWebhookRequest;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.policy.PaymentWebhookOutcomeRegistry;
import shop.shop.payment.policy.DTO.PaymentWebhookContext;
import shop.shop.payment.policy.interfaces.IPaymentWebhookOutcomeHandler;
import shop.shop.payment.repo.PaymentRepo;
import shop.shop.user.entity.User;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {

    OrderRepository orderRepository;
    PaymentRepo paymentRepo;

    SepayProperties sepayProperties;
    PaymentWebhookOutcomeRegistry paymentWebhookOutcomeRegistry;
    CurrentUserProvider currentUserProvider;
    AdminPaymentMapper adminPaymentMapper;
    ValidationUtils validationUtils;

    // lấy qr code
    public QrRepone getQr(QrRquest request) {

        User user = currentUserProvider.getCurrentUser();

        Order order = orderRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new ApiError(ErrorCode.ORDER_NOT_FOUND));

        PaymentEntity payment = paymentRepo.findByTransactionRef(request.getOrderCode())
                .orElseThrow(() -> new ApiError(ErrorCode.PAYMENT_NOT_FOUND));

        validateOwnership(user, payment);

        validatePaymentMethod(order);

        validateQrAllowed(payment, order);

        String qrUrl = buildQrcode(order);

        Instant expiredAt = Instant.now().plus(Duration.ofMinutes(5));

        return QrRepone.builder()
                .url(qrUrl)
                .expiredAt(expiredAt)
                .build();
    }

    // check xem order có thuộc user ko để gen qr
    private void validateOwnership(User user, PaymentEntity payment) {
        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }
    }

    // check method qr sp
    private void validatePaymentMethod(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.SEPAY) {
            throw new ApiError(ErrorCode.PAYMENT_NOT_SUPPORTED);
        }
    }

    // Kiểm tra payment và order còn ở trạng thái cho phép tạo/hiển thị mã QR hay
    // không.
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

    // hàm tạo url
    private String buildQrcode(Order order) {
        return String.format(
                sepayProperties.qrUrlTemplate(),
                sepayProperties.bank(),
                sepayProperties.accountNumber(),
                order.getTotalAmount().toPlainString(),
                order.getOrderCode());
    }

    // hàm xử lý wedhook sepay
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

        if (isDuplicateWebhook(payment, referenceCode)) {
            log.warn("Giao dịch trùng lặp (webhook bắn nhiều lần): {}", referenceCode);
            return Map.of("success", true);
        }

        if (isAlreadyPaid(payment)) {
            log.warn("Đơn hàng đã được thanh toán trước đó, khách chuyển khoản nhiều lần: {}", transactionRef);
            return Map.of("success", true);
        }

        LocalDateTime transactionDate = payload.getTransactionDate() != null
                ? payload.getTransactionDate()
                : LocalDateTime.now();

        boolean isLate = payment.getExpiredAt() != null
                && !transactionDate.isBefore(payment.getExpiredAt());

        // tạo contexxt để handler quyết định có xử lý webhook
        PaymentWebhookContext context = new PaymentWebhookContext(
                payment, referenceCode, transactionDate, transactionRef, amount, isLate);

        try {
            // xử lý theo các trường hợp đã handler
            IPaymentWebhookOutcomeHandler handler = paymentWebhookOutcomeRegistry.gethandler(context);
            handler.handle(context);
        } catch (Exception ex) {
            log.error(
                    "Lỗi khi xử lý webhook thanh toán, rollback giao dịch: transactionRef={}, referenceCode={}, error={}",
                    transactionRef, referenceCode, ex.getMessage(), ex);
            throw ex;
        }

        return Map.of("success", true);
    }

    // check giao dịch trùng lặp
    private boolean isDuplicateWebhook(PaymentEntity payment, String referenceCode) {
        return referenceCode.equals(payment.getReferenceCode());
    }

    // đơn hàng đã thanh toán mà lại thanh toán lần nx
    private boolean isAlreadyPaid(PaymentEntity payment) {
        return payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.PAID_LATE;
    }

    public ApiResponse<AdminPaymentsRepone> getPayment(String search, String status, LocalDate from,
            LocalDate to) {
        String normalizedSearch = validationUtils.normalize(search);
        PaymentStatus normalizedStatus = validationUtils.parseEnumIgnoreCase(status, PaymentStatus.class,
                ErrorCode.BAD_REQUEST);

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

}
