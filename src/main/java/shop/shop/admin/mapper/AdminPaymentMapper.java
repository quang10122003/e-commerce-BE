package shop.shop.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import shop.shop.admin.dto.response.AdminPayementItemRepone;
import shop.shop.payment.entity.PaymentEntity;

@Mapper(componentModel = "spring")
public interface AdminPaymentMapper {

    @Mapping(target = "orderCode",source = "order.orderCode")
    public AdminPayementItemRepone toAdminPaymentsRepone(PaymentEntity payment);
}
