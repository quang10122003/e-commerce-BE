package shop.shop.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import shop.shop.chat.Dto.repone.ChatMessageResponse;
import shop.shop.chat.Dto.repone.ChatRoomResponse;
import shop.shop.chat.Dto.repone.MarkRoomAsReadResult;
import shop.shop.chat.service.ChatService;
import shop.shop.common.dto.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/chat/rooms/{productId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(@PathVariable Long productId) {
        return ResponseEntity.ok(chatService.createRoom(productId));
    }

    @GetMapping("/chat/rooms/{productId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getCurrentUserRoomByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(chatService.getCurrentUserRoomByProduct(productId));
    }

    @GetMapping("/chat/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getCurrentUserRooms(
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(chatService.getCurrentUserRooms(search));
    }

    @GetMapping("/chat/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getMessages(roomId));
    }

    @GetMapping("/admin/chat/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getAdminRooms(
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(chatService.getAdminRooms(search));
    }

    // Danh dau da doc tin nhan.
    @PostMapping("/chat/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> markRoomAsRead(@PathVariable Long roomId) {
        MarkRoomAsReadResult result = chatService.markRoomAsRead(roomId);

        // Phat read-receipt cho tat ca client dang mo room de cap nhat trang thai da doc theo thoi gian thuc.
        if (result.getReadReceipt() != null) {
            messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, result.getReadReceipt());
        }

        // Khi admin doc tin cua customer, inbox admin cung can bo badge unread ma khong phai refresh hoac polling.
        if (result.getAdminRoomSummary() != null) {
            messagingTemplate.convertAndSend("/topic/admin/chat/rooms", result.getAdminRoomSummary());
        }

        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thành công", result.getRoom()));
    }
}
