package shop.shop.chat.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;

import shop.shop.chat.Dto.request.SendMessageRequest;
import shop.shop.chat.service.ChatService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatWebSocketController {
    ChatService chatService;

    @MessageMapping("/chat/rooms/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Valid @Payload SendMessageRequest request,
            // Xac thuc duoc lay tu WebSocketJwtInterceptor
            Principal principal) {
        chatService.sendMessage(roomId, request, principal);
    }
}
