package shop.shop.payment.DTO.repone;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QrRepone {
    String url;
    // Thời điểm QR hết hạn theo UTC để FE tính countdown không lệch múi giờ.
    Instant expiredAt;
}
