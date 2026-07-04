package shop.shop.chat.Dto.repone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadReceiptResponse {
    private String type;

    private Long roomId;

    private Long readerId;

    private String readerName;

    private List<Long> messageIds;

    private LocalDateTime readAt;
}
