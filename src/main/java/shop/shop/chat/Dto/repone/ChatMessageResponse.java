package shop.shop.chat.Dto.repone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;

    private Long roomId;

    private Long senderId;

    private String senderName;

    private String content;

    private String messageType;

    private boolean read;

    private LocalDateTime createdAt;
}