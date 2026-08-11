package shop.shop.integration.Resend.DTO.resquest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordMailDTO {
    String email;
    String token;
}
