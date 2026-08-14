// class chỉ lo việc bắn thông báo qua WebSocket cho FE
package shop.shop.payment.service;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentWebhookNotifier {
    SimpMessagingTemplate messagingTemplate;

    public void notify(String orderCode, String status, String message) {
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
}