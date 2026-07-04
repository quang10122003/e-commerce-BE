package shop.shop.order.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import shop.shop.admin.dto.response.AdminOrderItemRepone;
import shop.shop.admin.dto.response.AdminOrderProductItemRepone;
import shop.shop.order.dto.response.OrderItemResponse;
import shop.shop.order.dto.response.OrderResponse;
import shop.shop.order.entity.Order;
import shop.shop.order.entity.OrderItem;
import shop.shop.payment.entity.PaymentEntity;
import shop.shop.payment.repo.PaymentRepo;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class OrderMapper {

    @Autowired
    protected PaymentRepo paymentRepo;

    // MapStruct tự map field cùng tên — không cần khai báo lại
    @Mapping(target = "paymentMethod", source = "paymentMethod")
    @Mapping(target = "expiredAt", ignore = true) // set thủ công ở fillExpiredAt
    public abstract OrderResponse toResponse(Order order);

    public abstract OrderItemResponse toItemResponse(OrderItem orderItem);

    public abstract List<OrderResponse> toResponseList(List<Order> orders);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "paymentMethod", source = "paymentMethod")
    @Mapping(target = "items", source = "items")
    public abstract AdminOrderItemRepone toAdminOrderItem(Order order);

    @Mapping(target = "name", source = "productName")
    @Mapping(target = "category", source = "categoryName")
    public abstract AdminOrderProductItemRepone toAdminOrderProductItem(OrderItem item);

    public abstract List<AdminOrderItemRepone> toAdminOrderItemList(List<Order> orders);

    // Sau khi MapStruct map xong các field thường của toResponse(), set thêm
    // expiredAt từ payment.
    @AfterMapping
    protected void fillExpiredAt(Order order, @MappingTarget OrderResponse target) {
        PaymentEntity payment = paymentRepo.findByOrderId(order.getId()).orElse(null);
        target.setExpiredAt(payment == null ? null : payment.getExpiredAt());
    }
}