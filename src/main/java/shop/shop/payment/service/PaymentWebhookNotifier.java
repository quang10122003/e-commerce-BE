// class chỉ lo việc bắn thông báo qua WebSocket cho FE
package shop.shop.payment.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.integration.wedsocket.service.StompWebSocketSender;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentWebhookNotifier {
    StompWebSocketSender stompWebSocketSender;

    public void notify(String orderCode, String status, String message) {
        try {
            Map<String, String> payload = Map.of(
                    "status", status,
                    "orderCode", orderCode,
                    "message", message);
            stompWebSocketSender.send("/topic/payment/" + orderCode, payload);
        } catch (Exception ex) {
            log.error("Gửi thông báo socket thất bại: orderCode={}, status={}, message={}, error={}",
                    orderCode, status, message, ex.getMessage(), ex);
        }
    }
}