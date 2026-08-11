package shop.shop.order.service;


import org.springframework.stereotype.Component;

// xử lý payment khai tạo order với payment COD;
import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.Order;
import shop.shop.order.service.interfaces.IOrderPaymentHandler;

@Component
public class CodPaymentHandler implements IOrderPaymentHandler {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.COD;
    }

    @Override
    public void afterOrderSaved(Order order, String orderCode) {
        // COD không cần xử lý gì thêm
    }
}
