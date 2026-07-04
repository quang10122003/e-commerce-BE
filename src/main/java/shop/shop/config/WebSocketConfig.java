    package shop.shop.config;

    import org.springframework.context.annotation.Configuration;
    import org.springframework.messaging.simp.config.ChannelRegistration;
    import org.springframework.messaging.simp.config.MessageBrokerRegistry;
    import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
    import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
    import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

    import lombok.AccessLevel;
    import lombok.RequiredArgsConstructor;
    import lombok.experimental.FieldDefaults;
    import shop.shop.security.WebSocketJwtInterceptor;

    @FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
    @RequiredArgsConstructor
    @Configuration
    @EnableWebSocketMessageBroker
    public class WebSocketConfig  implements WebSocketMessageBrokerConfigurer   {
        WebSocketJwtInterceptor webSocketJwtInterceptor;

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            // Giao dien se ket noi vao ws://localhost:8080/ws/chat.
            registry.addEndpoint("/ws/chat")
                    .setAllowedOriginPatterns("*");
            registry.addEndpoint("/ws")
                    .setAllowedOriginPatterns("*")
                    .withSockJS();
        }

        @Override
        public void configureMessageBroker(MessageBrokerRegistry registry) {
            // Client dang ky nhan tin tai /topic/...
            registry.enableSimpleBroker("/topic");

            // Client gui tin nhan vao /api/...
            registry.setApplicationDestinationPrefixes("/api");
        }
        @Override
        public void configureClientInboundChannel(ChannelRegistration registration) {
            // Bat cac frame de xac thuc JWT trong STOMP header.
            registration.interceptors(webSocketJwtInterceptor);
        }
    }
