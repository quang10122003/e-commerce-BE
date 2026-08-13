package shop.shop.integration.RabbitMQ.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import shop.shop.integration.RabbitMQ.handle.interfaces.IMailHandler;

@Component
@RequiredArgsConstructor
public class MailHandlerRegistry {

    private final List<IMailHandler> handlerList;

    private Map<String, IMailHandler> handlers;

    @PostConstruct
    void init() {
        handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        IMailHandler::routingKey,
                        Function.identity()));
    }


    public IMailHandler getHandler(String routingKey) {

        IMailHandler handler = handlers.get(routingKey);

        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unknown mail routing key: " + routingKey);
        }

        return handler;
    }
}