package shop.shop.integration.Resend.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResendService {

    final RestClient restClient;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${app.resend.resend_key}")
    String apiKey;
    @Value("${app.frontend-url}")
    String domain;

    // gửi mail
    public void sendResetPasswordMail(String email, String token) {
        String resetLink = domain + "/reset-password?token="
                + token;

        Map<String, Object> body = Map.of(
                // dùng mail dev của resend
                "from", "onboarding@resend.dev",
                "to", email,
                "subject", "Reset password",
                "html",
                """
                        <h1>Reset password</h1>

                        <a href="%s">
                            Reset password
                        </a>
                        """.formatted(resetLink));
        try {
            restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.error("gửi email reset password cho email:{} thất bại",email);
        }

    }
}