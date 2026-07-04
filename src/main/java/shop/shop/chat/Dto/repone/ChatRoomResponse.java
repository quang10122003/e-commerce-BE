package shop.shop.chat.Dto.repone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shop.shop.common.ChatRoomAssignmentStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {

    private Long id;

    private Long productId;

    private String productName;

    private Long userId;

    private String userName;

    private Long adminId;

    private String adminName;

    private ChatRoomAssignmentStatus assignmentStatus;
    // Tin nhắn cuối.
    private String lastMessageContent;

    // Kiểu của tin nhắn cuối.
    private String lastMessageType;

    // Người nhắn cuối.
    private Long lastMessageSenderId;

    // Tên người nhắn cuối.
    private String lastMessageSenderName;

    // Thời gian tin nhắn cuối.
    private LocalDateTime lastMessageAt;

    // Số lượng tin nhắn chưa reply.
    private long unreadCount;

    private LocalDateTime createdAt;
}
