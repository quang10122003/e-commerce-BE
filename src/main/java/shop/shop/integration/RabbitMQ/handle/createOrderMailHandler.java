package shop.shop.integration.RabbitMQ.handle;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.RabbitMQ.DTO.CreateOrderMailProducer;
import shop.shop.integration.RabbitMQ.service.interfaces.IMailHandler;
import shop.shop.integration.Resend.DTO.resquest.CreateOrderMailDTO;
import shop.shop.integration.Resend.service.EmailService;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class createOrderMailHandler implements IMailHandler {
    EmailService emailService;

    ObjectMapper objectMapper;

    @Override
    public String routingKey() {
        return "email.order-create";
    }

    @Override
    public void handle(String data) {
        
        CreateOrderMailProducer message = objectMapper.readValue(
                data,
                CreateOrderMailProducer.class);

        CreateOrderMailDTO createOrderMailDTO = objectMapper.convertValue(message,CreateOrderMailDTO.class);
        emailService.sendOrderMail(createOrderMailDTO);
    }

}
