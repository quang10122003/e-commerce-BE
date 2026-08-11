// gom các handler theo PaymentMethod
package shop.shop.order.handler;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import shop.shop.common.PaymentMethod;

@Component
public class PaymentTimeoutHandlerRegistry {

    private final Map<PaymentMethod, IPaymentTimeoutHandler> handlers;

    public PaymentTimeoutHandlerRegistry(List<IPaymentTimeoutHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(IPaymentTimeoutHandler::supports, h -> h));
    }

    public IPaymentTimeoutHandler get(PaymentMethod method) {
        return handlers.get(method);
    }
}