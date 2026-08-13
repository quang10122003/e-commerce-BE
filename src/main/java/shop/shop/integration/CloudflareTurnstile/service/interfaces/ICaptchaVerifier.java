package shop.shop.integration.CloudflareTurnstile.service.interfaces;

public interface ICaptchaVerifier {
    boolean verify(String token);
}
