package shop.shop.integration.wedsocket.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.wedsocket.service.interfaces.IWebSocketSender;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class StompWebSocketSender implements IWebSocketSender {
    SimpMessagingTemplate messagingTemplate;
    @Override
    public void send(String toppic, Object data) {
        messagingTemplate.convertAndSend(toppic, data);
    }
    
}
