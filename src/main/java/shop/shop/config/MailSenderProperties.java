// obj lưu các biến config cho mail
package shop.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "app.mail")
public record MailSenderProperties(
    String orderSender,
    String resetPasswordSender) {
     
}
