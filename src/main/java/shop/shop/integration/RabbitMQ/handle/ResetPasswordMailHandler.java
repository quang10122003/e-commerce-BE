package shop.shop.integration.RabbitMQ.handle;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import shop.shop.integration.RabbitMQ.DTO.ResetPasswordProducer;
import shop.shop.integration.RabbitMQ.handle.interfaces.IMailHandler;
import shop.shop.integration.Resend.DTO.resquest.ResetPasswordMailDTO;
import shop.shop.integration.Resend.service.EmailService;
import shop.shop.integration.Resend.service.ResetPasswordMailTemplate;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResetPasswordMailHandler implements IMailHandler {

    EmailService emailService;
    ResetPasswordMailTemplate resetPasswordMailTemplate;
    ObjectMapper objectMapper;

    @Override
    public String routingKey() {
        return "email.reset-password";
    }

    @Override
    public void handle(String data) {
        ResetPasswordProducer message = objectMapper.readValue(data, ResetPasswordProducer.class);
        ResetPasswordMailDTO dto = new ResetPasswordMailDTO(message.getEmail(), message.getToken());
        emailService.send(resetPasswordMailTemplate, dto);
    }
}