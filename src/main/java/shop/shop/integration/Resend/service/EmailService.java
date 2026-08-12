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
import shop.shop.integration.Resend.DTO.resquest.CreateOrderMailDTO;
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
    CreateOrderMailTemplate createOrderMailTemplate;

    // gửi email khi reset mk
    public void SendResetPasswordMail(String emailUser, String token) {
        ResetPasswordMailDTO resetPasswordMailDTO = new ResetPasswordMailDTO(emailUser, token);

        EmailContent content = resetPasswordMailTemplate.build(resetPasswordMailDTO);

        sendEmail(content);
    }

    // gửi email khi order đơn hàng
    public void sendOrderMail(CreateOrderMailDTO data){
        EmailContent content = createOrderMailTemplate.build(data);
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
            logger.info("gửi email {} cho email:{} thành công", content.getSubject(), content.getTo());
        } catch (Exception e) {
            logger.error("gửi email {} cho email:{} thất bại",content.getSubject(), content.getTo());
        }
    }
}
