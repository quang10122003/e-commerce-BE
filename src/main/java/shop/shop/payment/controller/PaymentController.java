package shop.shop.payment.controller;

import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.payment.DTO.repone.QrRepone;
import shop.shop.payment.DTO.request.QrRquest;
import shop.shop.payment.DTO.request.SePayWebhookRequest;
import shop.shop.payment.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PaymentController {

    PaymentService paymentServeice;
    @NonFinal
    @Value("${app.sepay.secret-key-webhook}")
    private String APIKEY_SEPAY;

    // api nhặn wedhook sepay bắn về khi chuyển tiền thành công
    @PostMapping("/sepay/webhook")
    public ResponseEntity<Map<String, Boolean>> webhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody SePayWebhookRequest payload) {

        // check authen của webhook gửi về
        if (!("Apikey " + APIKEY_SEPAY).equals(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(paymentServeice.webhook(payload));
    }

    @PostMapping("/qr")
    public ResponseEntity<ApiResponse<QrRepone>> getQRPayment(@Valid @RequestBody QrRquest request) {
        return ResponseEntity.status(200).body(
                ApiResponse.success("", paymentServeice.getQr(request)));
    }
}