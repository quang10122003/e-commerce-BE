package shop.shop.integration.RabbitMQ.DTO;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.RabbitMQ.DTO.interfaces.DomainEvent;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordProducer  implements DomainEvent{
    String email;
    String token;
    @Override
    public String routingKey() {
        return "email.reset-password";
    }
}
