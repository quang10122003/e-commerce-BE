package shop.shop.order.policy;

import org.springframework.stereotype.Component;

import shop.shop.common.PaymentMethod;

@Component
public class CodPaymentPolicy implements IOrderPaymentPolicy {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.COD;
    }

}