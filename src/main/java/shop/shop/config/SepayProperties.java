// obj lưu các biến config cho sepay
package shop.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sepay")
public record SepayProperties(
        String secretKeyWebhook,
        String bank,
        String accountNumber,
        String qrUrlTemplate) {
    
}
