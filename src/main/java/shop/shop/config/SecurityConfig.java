package shop.shop.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

import shop.shop.common.error.ErrorCode;
import shop.shop.security.JwtAuthFilter;
import shop.shop.security.OAuth2LoginFailureHandler;
import shop.shop.security.OAuth2LoginSuccessHandler;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {
        JwtAuthFilter jwtAuthFilter;
        AuthenticationProvider authenticationProvider;
        RestAuthenticationEntryPoint restAuthenticationEntryPoint;
        OAuth2LoginFailureHandler failureHandler;
        OAuth2LoginSuccessHandler successHandler;

        static String[] PUBLIC_ENDPOINTS = {
                        "/api/auth/login",
                        "/api/auth/signup",
                        "/api/auth/refresh-token",
                        "/api/auth/validate-token",
                        "/api/cloudinary/upload-images",
                        "/api/products/**",
                        "/api/categories",
                        "/ws/chat",
                        "/ws/chat/**",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/payments/sepay/webhook",
                        "/api/auth/oauth2/authorize/**",
                        "/api/auth/login/oauth2"
        };

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(restAuthenticationEntryPoint)
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
                                                        response.setStatus(errorCode.getStatusCode().value());
                                                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        response.getWriter().write(buildErrorBody(errorCode));
                                                }))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                // Co the mo them GET public cho products/categories neu can.
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(
                                                                auth -> auth.baseUri("/api/auth/oauth2/authorize")) // cấu
                                                                                                                    // hình
                                                                                                                    // endpoint
                                                                                                                    // fe
                                                                                                                    // call
                                                                                                                    // khi
                                                                                                                    // ấn
                                                                                                                    // đăng
                                                                                                                    // nhập
                                                                                                                    // gg
                                                                                                                    // từ
                                                                                                                    // url
                                                                                                                    // mặc
                                                                                                                    // định
                                                                                                                    // /oauth2/authorization/google
                                                                                                                    // của
                                                                                                                    // OAuth2AuthorizationRequestRedirectFilter
                                                .redirectionEndpoint(
                                                                redirect -> redirect.baseUri("/api/auth/login/oauth2"))
                                                .successHandler(successHandler)
                                                .failureHandler(failureHandler));

                return http.build();
        }

        String buildErrorBody(ErrorCode errorCode) {
                String message = escapeJson(errorCode.getMessage());
                return """
                                {"success":false,"message":"%s","data":null,"error":{"errorCode":"%s","message":"%s"},"timestamp":null}
                                """
                                .formatted(message, errorCode.name(), message);
        }

        String escapeJson(String value) {
                return value
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"");
        }
}
