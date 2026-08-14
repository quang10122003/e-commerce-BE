package shop.shop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Chỉ cho phép 2 domain frontend được gọi API/WebSocket.
        configuration.setAllowedOrigins(List.of(
                "https://shopmini.daoxuanquang.dev",
                "https://admin.daoxuanquang.dev",
            "http://localhost:3000"));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Cho phép mọi header, bao gồm Authorization, Content-Type, ...
        configuration.setAllowedHeaders(List.of("*"));

        // Cần thiết nếu frontend gửi cookie/credentials kèm request
        // (ví dụ refresh token qua cookie). Nếu không dùng cookie thì có thể bỏ.
        configuration.setAllowCredentials(true);

        // Một số header custom trả về mà frontend cần đọc được (nếu có),
        // ví dụ Authorization ở response.
        configuration.setExposedHeaders(List.of("Authorization"));

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}