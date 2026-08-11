// interface định nghĩa các hàm xử lý payment khi tạo order
package shop.shop.order.service.interfaces;

import shop.shop.common.PaymentMethod;
import shop.shop.order.entity.Order;

public interface IOrderPaymentHandler {

    PaymentMethod getPaymentMethod();

    // hook xử lý phần riêng của từng phương thức, chạy SAU khi order đã được lưu +
    // trừ kho
    void afterOrderSaved(Order order, String orderCode);
}