package shop.shop.integration.CloudflareTurnstile.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.CloudflareTurnstile.DTO.repone.TurnstileResponse;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TurnstileService {

    final RestClient restClient;

    @Value("${app.turnstile.secret-key}")
    String secretKey;

    public boolean verify(String token) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("secret", secretKey);
        body.add("response", token);

        TurnstileResponse response = restClient.post()
                .uri("https://challenges.cloudflare.com/turnstile/v0/siteverify")
                .body(body)
                .retrieve()
                .body(TurnstileResponse.class);

        return response != null && response.isSuccess();
    }
}