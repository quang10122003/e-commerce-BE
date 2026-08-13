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
import shop.shop.integration.Resend.service.interfaces.IEmailTemplate;

// OCP: không còn biết trước có bao nhiêu loại mail (trước đây phải có field +
// method riêng cho từng loại). Chỉ cần nhận đúng template + data để build và
// gửi. Thêm loại mail mới KHÔNG cần sửa class này.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {
    @NonFinal
    @Value("${app.resend.resend_key}")
    String apiKey;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    RestClient restClient;

    public <T> void send(IEmailTemplate<T> template, T data) {
        EmailContent content = template.build(data);
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
            logger.error("gửi email {} cho email:{} thất bại", content.getSubject(), content.getTo(), e);
        }
    }
}