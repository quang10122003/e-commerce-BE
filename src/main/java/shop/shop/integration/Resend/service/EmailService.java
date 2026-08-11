package shop.shop.integration.Resend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import shop.shop.integration.Resend.DTO.respone.EmailContent;
import shop.shop.integration.Resend.DTO.resquest.ResetPasswordMailDTO;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {
    @NonFinal
    @Value("${app.resend.resend_key}")
    String apiKey;
    ResetPasswordMailTemplate resetPasswordMailTemplate;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    RestClient restClient;

    public void SendResetPasswordMail(String emailUser, String token) {
        ResetPasswordMailDTO resetPasswordMailDTO = new ResetPasswordMailDTO(emailUser, token);

        EmailContent content = resetPasswordMailTemplate.build(resetPasswordMailDTO);

        sendEmail(content);
    }

    private void sendEmail(EmailContent content) {
        try {
            restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.error("gửi email reset password cho email:{} thất bại", content.getTo());
        }
    }
}
