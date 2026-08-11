package shop.shop.integration.Resend.DTO.respone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailContent {
    String from;
    String to;
    String subject;
    String html;
}
