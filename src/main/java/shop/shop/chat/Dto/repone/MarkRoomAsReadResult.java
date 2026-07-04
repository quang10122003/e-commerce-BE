package shop.shop.chat.Dto.repone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkRoomAsReadResult {
    private ChatRoomResponse room;

    private ChatRoomResponse adminRoomSummary;

    private ChatReadReceiptResponse readReceipt;
}
