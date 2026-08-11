package shop.shop.integration.Resend.service;

import org.springframework.stereotype.Component;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.Resend.DTO.respone.EmailContent;
import shop.shop.integration.Resend.DTO.resquest.ResetPasswordMailDTO;
import shop.shop.integration.Resend.service.interfaces.IEmailTemplate;

@Component
@FieldDefaults(level =  AccessLevel.PRIVATE)
@NoArgsConstructor
public class ResetPasswordMailTemplate extends IEmailTemplate<ResetPasswordMailDTO> {
    
    @Override
    protected EmailContent build(ResetPasswordMailDTO data) {
        String resetLink = domain + "/reset-password?token="
                + data.getToken();

        EmailContent contenet = new EmailContent(
            from_email,
            data.getEmail(),
            "Reset password",
            wrapHtml("""
                        <h1>Reset password</h1>
                        <a href="%s">Reset password</a>
                        """.formatted(resetLink))
                        
        );
        return contenet;
        
    }
    
}
