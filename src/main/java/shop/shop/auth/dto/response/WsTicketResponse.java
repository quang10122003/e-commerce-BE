package shop.shop.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsTicketResponse {
    private String ticket;
    private String tokenType;
    private long expiresInSeconds;
}
